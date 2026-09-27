package de.craftorio.energy;

import net.neoforged.neoforge.energy.EnergyStorage;

/** FE storage that reports changes and lets its owner add or use energy past the transfer limits. */
public final class EnergyBuffer extends EnergyStorage {
    private final Runnable onChange;

    public EnergyBuffer(int capacity, int maxReceive, int maxExtract, Runnable onChange) {
        super(capacity, maxReceive, maxExtract);
        this.onChange = onChange;
    }

    @Override
    public int receiveEnergy(int toReceive, boolean simulate) {
        int received = super.receiveEnergy(toReceive, simulate);
        if (received > 0 && !simulate) {
            onChange.run();
        }
        return received;
    }

    @Override
    public int extractEnergy(int toExtract, boolean simulate) {
        int extracted = super.extractEnergy(toExtract, simulate);
        if (extracted > 0 && !simulate) {
            onChange.run();
        }
        return extracted;
    }

    /** Produced by the owner itself (e.g. burning fuel). */
    public void generate(int amount) {
        energy = Math.min(capacity, energy + amount);
        onChange.run();
    }

    /** Used by the owner itself; returns false and uses nothing if not enough is stored. */
    public boolean consume(int amount) {
        if (energy < amount) {
            return false;
        }
        energy -= amount;
        onChange.run();
        return true;
    }

    public int freeSpace() {
        return capacity - energy;
    }

    public void setEnergy(int energy) {
        this.energy = Math.max(0, Math.min(capacity, energy));
    }
}
