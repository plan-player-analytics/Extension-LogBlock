/*
    Copyright(c) 2021 AuroraLS3

    The MIT License(MIT)

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files(the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions :
    The above copyright notice and this permission notice shall be included in
    all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
    THE SOFTWARE.
*/
package net.playeranalytics.extension.logblock;

import com.djrapitops.plan.extension.CallEvents;
import com.djrapitops.plan.extension.DataExtension;
import com.djrapitops.plan.extension.ElementOrder;
import com.djrapitops.plan.extension.NotReadyException;
import com.djrapitops.plan.extension.annotation.DataBuilderProvider;
import com.djrapitops.plan.extension.annotation.PluginInfo;
import com.djrapitops.plan.extension.annotation.TabInfo;
import com.djrapitops.plan.extension.builder.ExtensionDataBuilder;
import com.djrapitops.plan.extension.icon.Color;
import com.djrapitops.plan.extension.icon.Icon;
import com.djrapitops.plan.extension.table.Table;
import de.diddiz.LogBlock.LogBlock;
import de.diddiz.LogBlock.QueryParams;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DataExtension.
 *
 * @author AuroraLS3
 */
@PluginInfo(name = "LogBlock", color = Color.BROWN) // cube icon
@TabInfo(tab = "Overworld", iconName = "globe", elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE})
@TabInfo(tab = "Nether", iconName = "fire", elementOrder = {ElementOrder.VALUES, ElementOrder.TABLE})
public class LogBlockExtension implements DataExtension {

    private final LogBlock logblock;

    private final Set<String> skipWorlds = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public LogBlockExtension() {
        logblock = (LogBlock) Bukkit.getPluginManager().getPlugin("LogBlock");
    }

    LogBlockExtension(boolean forTesting) {
        logblock = null;
    }

    @Override
    public CallEvents[] callExtensionMethodsOn() {
        return new CallEvents[]{CallEvents.PLAYER_LEAVE};
    }

    @DataBuilderProvider
    public ExtensionDataBuilder playerData(String playerName) {
        try {
            int stoneBroken = getBrokenStoneCount(playerName);
            int diamonds = getBrokenCount(playerName, Material.DIAMOND_ORE);
            int emeralds = getBrokenCount(playerName, Material.EMERALD_ORE);
            int redstone = getBrokenCount(playerName, Material.REDSTONE_ORE);
            int lapis = getBrokenCount(playerName, Material.LAPIS_ORE);
            int iron = getBrokenCount(playerName, Material.IRON_ORE);
            int gold = getBrokenCount(playerName, Material.GOLD_ORE);
            int coal = getBrokenCount(playerName, Material.COAL_ORE);
            
            /*
             * 1.17 DEEPSLATE ores and stones. Not using constants for backwards compatibility
             */
            try {
                Material.valueOf("DEEPSLATE_DIAMOND_ORE"); // to force the IllegalArgument Exception, on pre MC 1.17
                stoneBroken+=getBrokenCount(playerName, "DEEPSLATE");
                stoneBroken+=getBrokenCount(playerName, "TUFF");
                diamonds+=getBrokenCount(playerName, "DEEPSLATE_DIAMOND_ORE");
                emeralds+=getBrokenCount(playerName, "DEEPSLATE_EMERALD_ORE");
                redstone+=getBrokenCount(playerName, "DEEPSLATE_REDSTONE_ORE");
                lapis+=getBrokenCount(playerName, "DEEPSLATE_LAPIS_ORE");
                iron+=getBrokenCount(playerName, "DEEPSLATE_IRON_ORE");
                gold+=getBrokenCount(playerName, "DEEPSLATE_GOLD_ORE");
                coal+=getBrokenCount(playerName, "DEEPSLATE_COAL_ORE");
            } catch (IllegalArgumentException e) {
                // ignore (pre Minecraft 1.17, has no DEEPSLATE-Stuff)
            }

            Table.Factory table = Table.builder()
                    .columnOne("Ore", Icon.called("cube").build())
                    .columnTwo("Blocks broken", Icon.called("hammer").build())
                    .columnThree("per Stone ratio", Icon.called("percentage").build());

            DecimalFormat decimalFormat = new DecimalFormat("#.##");

            int divider = stoneBroken > 0 ? stoneBroken : 1;

            double diamondToStoneRatio = diamonds * 100.0 / divider;
            table.addRow("Diamonds", diamonds, decimalFormat.format(diamondToStoneRatio));
            table.addRow("Emeralds", emeralds, decimalFormat.format(emeralds * 100.0 / divider));
            table.addRow("Redstone", redstone, decimalFormat.format(redstone * 100.0 / divider));
            table.addRow("Lapis", lapis, decimalFormat.format(lapis * 100.0 / divider));
            table.addRow("Iron", iron, decimalFormat.format(iron * 100.0 / divider));
            table.addRow("Gold", gold, decimalFormat.format(gold * 100.0 / divider));
            table.addRow("Coal", coal, decimalFormat.format(coal * 100.0 / divider));

            Table.Factory netherTable = Table.builder()
                    .columnOne("Ore", Icon.called("cube").build())
                    .columnTwo("Blocks broken", Icon.called("hammer").build())
                    .columnThree("per Netherrack ratio", Icon.called("percentage").build());

            int netherStone = getBrokenCount(playerName, Material.NETHERRACK);
            int netherGold = getBrokenCount(playerName, "NETHER_GOLD_ORE");
            int ancientDebris = getBrokenCount(playerName, "ANCIENT_DEBRIS");

            int netherDivider = netherStone > 0 ? netherStone : 1;

            netherTable.addRow("Nether Gold", netherGold, netherGold != -1 ? decimalFormat.format(netherGold * 100.0 / netherDivider) : "Old version");
            netherTable.addRow("Ancient Debris", ancientDebris, ancientDebris != -1 ? decimalFormat.format(ancientDebris * 100.0 / netherDivider) : "Old version");

            return newExtensionDataBuilder()
                    .addTable("ore_table", table.build(), Color.LIGHT_BLUE, "Overworld")
                    .addTable("nether_ore_table", netherTable.build(), Color.BROWN, "Nether")
                    .addValue(Double.class, valueBuilder("Diamonds/Stone ratio")
                            .description("How many stone blocks the player has broken for each diamond they have found")
                            .showOnTab("Overworld")
                            .icon(Icon.called("gem").of(Color.CYAN).build())
                            .showInPlayerTable()
                            .buildDouble(diamondToStoneRatio!=0?100.0/diamondToStoneRatio:0))
                    .addValue(Long.class, valueBuilder("Stone broken")
                            .description("How many stone blocks the player has broken")
                            .priority(100)
                            .showOnTab("Overworld")
                            .icon(Icon.called("hammer").of(Color.LIGHT_BLUE).build())
                            .buildNumber(stoneBroken))
                    .addValue(Long.class, valueBuilder("Netherrack broken")
                            .description("How many netherrack blocks the player has broken")
                            .priority(100)
                            .showOnTab("Nether")
                            .icon(Icon.called("hammer").of(Color.BROWN).build())
                            .buildNumber(netherStone));
        } catch (SQLException e) {
            throw new NotReadyException();
        }
    }

    private int getBrokenStoneCount(String playerName) throws SQLException {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (skipWorlds.contains(world.getName())) continue;
            try {
                QueryParams params = new QueryParams(logblock);
                params.setPlayer(playerName);
                params.bct = QueryParams.BlockChangeType.DESTROYED;
                params.limit = -1;
                params.types = Arrays.asList(Material.STONE, Material.GRANITE, Material.ANDESITE, Material.DIORITE);
                params.needCount = true;
                params.world = world;
                count += logblock.getCount(params);
            } catch (IllegalArgumentException worldIsNotLogged) {
                skipWorlds.add(world.getName());
            }
        }
        return count;
    }

    private int getBrokenCount(String playerName, String materialName) throws SQLException {
        try {
            return getBrokenCount(playerName, Material.valueOf(materialName));
        } catch (IllegalArgumentException e) {
            // No such material
            return -1;
        }
    }

    private int getBrokenCount(String playerName, Material material) throws SQLException {
        int count = 0;
        for (World world : Bukkit.getWorlds()) {
            if (skipWorlds.contains(world.getName())) continue;
            try {
                QueryParams params = new QueryParams(logblock);
                params.setPlayer(playerName);
                params.bct = QueryParams.BlockChangeType.DESTROYED;
                params.limit = -1;
                params.types = Collections.singletonList(material);
                params.needCount = true;
                params.world = world;
                count += logblock.getCount(params);
            } catch (IllegalArgumentException worldIsNotLogged) {
                skipWorlds.add(world.getName());
            }
        }
        return count;
    }
}
