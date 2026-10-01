package de.craftorio.defense.sim;

/**
 * What a supply depot (Banana Farm) earns: production per round, the bank with its interest and the loans of the
 * upper tiers. The numbers come from the effects of the bought upgrades (see {@link TowerProfile#numbers}).
 */
public final class DepotEconomy {
    /** Coins a depot without upgrades brings per round (4 bananas of 20). */
    public static final double BASE_INCOME = 80;
    /** Extra share of the round's income when the depot gets its basket of goods (§4.4). */
    public static final double BASKET_BONUS = 0.5;

    private double bank;
    private double debt;

    public double bank() {
        return bank;
    }

    public double debt() {
        return debt;
    }

    public void restore(double bank, double debt) {
        this.bank = bank;
        this.debt = debt;
    }

    /** Production per round: the income of path 1 with the value factor of path 2, plus the market of path 3. */
    public static double production(TowerProfile profile) {
        double income = profile.numbers.containsKey("income") ? profile.number("income") : BASE_INCOME;
        double factor = profile.numbers.containsKey("income_factor") ? profile.number("income_factor") : 1;
        return income * factor + profile.number("market") + profile.number("bonus");
    }

    /**
     * The end of a round: pays production (more with the basket), deposits into the bank and lets it earn interest.
     * Money goes to repay a loan first.
     *
     * @return coins to pay out now
     */
    public double roundEnd(TowerProfile profile, boolean basket) {
        double income = production(profile) * (basket ? 1 + BASKET_BONUS : 1);
        if (profile.has("bank")) {
            bank = Math.min(profile.number("bank_cap"), bank * (1 + profile.number("bank_interest")) + profile.number("bank_income"));
        }
        double repaid = Math.min(debt, income);
        debt -= repaid;
        return income - repaid;
    }

    /** Pays out the whole bank. */
    public double withdraw() {
        double paid = bank;
        bank = 0;
        return paid;
    }

    /** Takes out the loan of the upgrade; only one at a time. */
    public double loan(TowerProfile profile) {
        if (debt > 0) {
            return 0;
        }
        double amount = profile.number("loan");
        debt = amount;
        return amount;
    }
}
