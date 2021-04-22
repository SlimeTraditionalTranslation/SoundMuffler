package dev.j3fftw.soundmuffler;

import io.github.thebusybiscuit.slimefun4.core.attributes.EnergyNetComponent;
import io.github.thebusybiscuit.slimefun4.core.handlers.BlockPlaceHandler;
import io.github.thebusybiscuit.slimefun4.core.networks.energy.EnergyNetComponentType;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunPlugin;
import me.mrCookieSlime.CSCoreLibPlugin.Configuration.Config;
import me.mrCookieSlime.Slimefun.Lists.RecipeType;
import me.mrCookieSlime.Slimefun.Objects.SlimefunItem.SlimefunItem;
import me.mrCookieSlime.Slimefun.Objects.handlers.BlockTicker;
import me.mrCookieSlime.Slimefun.Objects.handlers.ItemHandler;
import me.mrCookieSlime.Slimefun.api.BlockStorage;
import me.mrCookieSlime.Slimefun.api.SlimefunItemStack;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenu;
import me.mrCookieSlime.Slimefun.api.inventory.BlockMenuPreset;
import me.mrCookieSlime.Slimefun.api.item_transport.ItemTransportFlow;
import me.mrCookieSlime.Slimefun.cscorelib2.item.CustomItem;
import me.mrCookieSlime.Slimefun.cscorelib2.protection.ProtectableAction;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.inventory.ItemStack;

public class SoundMufflerMachine extends SlimefunItem implements EnergyNetComponent {

    private static final String ITEM_NAME = "&3靜音羊毛";
    private static final String ITEM_ID = "SOUND_MUFFLER";
    
    public static final int DISTANCE = 8;
    private static final int[] border = {1, 2, 3, 4, 5, 6, 7};

    public SoundMufflerMachine() {
        super(SoundMuffler.SOUND_MUFFLER,
            new SlimefunItemStack(ITEM_ID, Material.WHITE_CONCRETE, ITEM_NAME,
                "", "&7靜音所有的聲音", "&7在8格範圍內", "", "&e\u26A1 需要使用電力!"
            ),
            ITEM_ID,
            RecipeType.ENHANCED_CRAFTING_TABLE,
            new ItemStack[] {
                new ItemStack(Material.WHITE_WOOL), SlimefunItems.STEEL_PLATE, new ItemStack(Material.WHITE_WOOL),
                SlimefunItems.STEEL_PLATE, SlimefunItems.ELECTRIC_MOTOR, SlimefunItems.STEEL_PLATE,
                new ItemStack(Material.WHITE_WOOL), SlimefunItems.STEEL_PLATE, new ItemStack(Material.WHITE_WOOL)
            }

        );
        addItemHandler(onPlace());

        new BlockMenuPreset(ITEM_ID, ITEM_NAME) {

            @Override
            public void init() {
                constructMenu(this);
            }

            @Override
            public void newInstance(final BlockMenu menu, final Block b) {
                int volume = 10;
                boolean enabled = false;
                if (!BlockStorage.hasBlockInfo(b) || BlockStorage.getLocationInfo(b.getLocation(), "enabled") == null) {
                    BlockStorage.addBlockInfo(b, "volume", String.valueOf(volume));
                    BlockStorage.addBlockInfo(b, "enabled", String.valueOf(false));

                } else {
                    volume = Integer.parseInt(BlockStorage.getLocationInfo(b.getLocation(), "volume"));
                    enabled = Boolean.parseBoolean(BlockStorage.getLocationInfo(b.getLocation(), "enabled"));
                }

                menu.replaceExistingItem(8, new CustomItem((enabled ? Material.REDSTONE : Material.GUNPOWDER),
                    "&7啟動: " + (enabled ? "&a\u2714" : "&4\u2718"), "", "&e> 點擊啟用此機器"));
                menu.replaceExistingItem(0, new CustomItem(Material.PAPER,
                    "&e音量: &b" + volume,
                    "&7有效範圍: 0-100",
                    "&7左鍵: -10",
                    "&7右鍵: +10",
                    "&7當按著Shift: +/-1"));

                final int finalVolume = volume;
                menu.addMenuClickHandler(0, (p, arg1, arg2, arg3) -> {
                    int newVolume;

                    if (arg3.isRightClicked()) {
                        if (arg3.isShiftClicked())
                            newVolume = Math.min(finalVolume + 1, 100);
                        else
                            newVolume = Math.min(finalVolume + 10, 100);
                    } else {
                        if (arg3.isShiftClicked())
                            newVolume = Math.max(finalVolume - 1, 0);
                        else
                            newVolume = Math.max(finalVolume - 10, 0);
                    }

                    BlockStorage.addBlockInfo(b, "volume", String.valueOf(newVolume));
                    newInstance(menu, b);
                    return false;
                });
                menu.addMenuClickHandler(8, (p, arg1, arg2, arg3) -> {
                    final String isEnabled = BlockStorage.getLocationInfo(b.getLocation(), "enabled");
                    if (isEnabled != null && isEnabled.equals("true"))
                        BlockStorage.addBlockInfo(b, "enabled", "false");
                    else
                        BlockStorage.addBlockInfo(b, "enabled", "true");
                    newInstance(menu, b);
                    return false;
                });
            }

            @Override
            public boolean canOpen(Block b, Player p) {
                return p.hasPermission("slimefun.inventory.bypass")
                    || SlimefunPlugin.getProtectionManager()
                    .hasPermission(p, b, ProtectableAction.INTERACT_BLOCK);
            }

            @Override
            public int[] getSlotsAccessedByItemTransport(ItemTransportFlow flow) {
                return new int[0];
            }
        };
    }

    protected void constructMenu(BlockMenuPreset preset) {
        for (int i : border) {
            preset.addItem(i, new CustomItem(Material.GRAY_STAINED_GLASS_PANE, " "),
                (player, i1, itemStack, clickAction) -> false);
        }
    }


    private ItemHandler onPlace() {
        return new BlockPlaceHandler(false) {
            @Override
            public void onPlayerPlace(BlockPlaceEvent e) {
                BlockStorage.addBlockInfo(e.getBlock(), "enabled", "false");
                BlockStorage.addBlockInfo(e.getBlock(), "volume", "10");
            }
        };
    }

    @Override
    public EnergyNetComponentType getEnergyComponentType() {
        return EnergyNetComponentType.CONSUMER;
    }

    public int getEnergyConsumption() {
        return 8;
    }

    @Override
    public int getCapacity() {
        return 352;
    }

    @Override
    public void preRegister() {
        addItemHandler(new BlockTicker() {

            @Override
            public void tick(Block b, SlimefunItem sf, Config data) {
                try {
                    SoundMufflerMachine.this.tick(b);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void uniqueTick() {
            }

            @Override
            public boolean isSynchronized() {
                return true;
            }
        });

    }

    private void tick(Block b) {
        if ((BlockStorage.getLocationInfo(b.getLocation(), "enabled").equals("true"))
            && (getCharge(b.getLocation()) > 8)) {
            removeCharge(b.getLocation(), getEnergyConsumption());
        }
    }
}

