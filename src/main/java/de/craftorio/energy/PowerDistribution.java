package de.craftorio.energy;

/**
 * Splits one tick of energy within a network: everything the consumers ask for, up to what the producers
 * can give. Both sides are shared in proportion to their offer/demand, so a short grid slows every machine
 * equally, like Factorio's satisfaction.
 */
public final class PowerDistribution {
    private PowerDistribution() {
    }

    public record Result(long[] taken, long[] given, long transferred) {
    }

    public static Result distribute(long[] supply, long[] demand) {
        long totalSupply = sum(supply);
        long totalDemand = sum(demand);
        long transfer = Math.min(totalSupply, totalDemand);
        return new Result(share(transfer, supply, totalSupply), share(transfer, demand, totalDemand), transfer);
    }

    /** Splits {@code amount} by weight without exceeding any weight; rounding leftovers go to the first entries with room. */
    static long[] share(long amount, long[] weights, long totalWeight) {
        long[] out = new long[weights.length];
        if (amount <= 0 || totalWeight <= 0) {
            return out;
        }
        long assigned = 0;
        for (int i = 0; i < weights.length; i++) {
            out[i] = (long) ((double) amount * weights[i] / totalWeight);
            out[i] = Math.min(out[i], weights[i]);
            assigned += out[i];
        }
        for (int i = 0; i < weights.length && assigned < amount; i++) {
            long extra = Math.min(weights[i] - out[i], amount - assigned);
            out[i] += extra;
            assigned += extra;
        }
        return out;
    }

    private static long sum(long[] values) {
        long total = 0;
        for (long value : values) {
            total += Math.max(0, value);
        }
        return total;
    }
}
