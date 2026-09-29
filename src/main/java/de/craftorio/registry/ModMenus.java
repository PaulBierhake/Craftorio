package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.menu.GeneratorMenu;
import de.craftorio.menu.ProcessingMachineMenu;
import de.craftorio.menu.TerminalMenu;
import de.craftorio.menu.TowerMenu;
import de.craftorio.menu.WorkbenchMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenus {
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, Craftorio.MOD_ID);

    public static final DeferredHolder<MenuType<?>, MenuType<GeneratorMenu>> GENERATOR =
            MENUS.register("generator", () -> IMenuTypeExtension.create(GeneratorMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ProcessingMachineMenu>> PROCESSING_MACHINE =
            MENUS.register("processing_machine", () -> IMenuTypeExtension.create(ProcessingMachineMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<de.craftorio.menu.DrillMenu>> DRILL =
            MENUS.register("drill", () -> IMenuTypeExtension.create(de.craftorio.menu.DrillMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<de.craftorio.menu.TradingPostMenu>> TRADING_POST =
            MENUS.register("trading_post", () -> IMenuTypeExtension.create(de.craftorio.menu.TradingPostMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<TerminalMenu>> TERMINAL =
            MENUS.register("terminal", () -> IMenuTypeExtension.create(TerminalMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<WorkbenchMenu>> WORKBENCH =
            MENUS.register("workbench", () -> IMenuTypeExtension.create(WorkbenchMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<TowerMenu>> TOWER =
            MENUS.register("tower", () -> IMenuTypeExtension.create(TowerMenu::new));

    public static final DeferredHolder<MenuType<?>, MenuType<de.craftorio.menu.LaboratoryMenu>> LABORATORY =
            MENUS.register("laboratory", () -> IMenuTypeExtension.create(de.craftorio.menu.LaboratoryMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<de.craftorio.menu.BoilerMenu>> BOILER =
            MENUS.register("boiler", () -> IMenuTypeExtension.create(de.craftorio.menu.BoilerMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<de.craftorio.menu.DepotMenu>> DEPOT =
            MENUS.register("depot", () -> new MenuType<>(de.craftorio.menu.DepotMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));

    private ModMenus() {
    }
}
