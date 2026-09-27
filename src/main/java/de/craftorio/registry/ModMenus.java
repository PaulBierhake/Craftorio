package de.craftorio.registry;

import de.craftorio.Craftorio;
import de.craftorio.menu.GeneratorMenu;
import de.craftorio.menu.ProcessingMachineMenu;
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

    private ModMenus() {
    }
}
