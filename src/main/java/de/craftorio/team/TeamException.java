package de.craftorio.team;

/** A rejected team operation. {@link #translationKey()} is shown to the player. */
public final class TeamException extends RuntimeException {
    private final String translationKey;
    private final transient Object[] args;

    public TeamException(String translationKey, Object... args) {
        super(translationKey);
        this.translationKey = translationKey;
        this.args = args;
    }

    public String translationKey() {
        return translationKey;
    }

    public Object[] args() {
        return args;
    }
}
