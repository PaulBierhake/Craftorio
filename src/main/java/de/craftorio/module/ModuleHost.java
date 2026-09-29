package de.craftorio.module;

/** A block entity with module slots (machines, drills, laboratories, pumpjacks). */
public interface ModuleHost {
    ModuleState modules();
}
