package de.craftorio.logistics;

/** Height rules for belts on slopes. Pure logic for unit tests. */
public final class BeltGeometry {
    private BeltGeometry() {
    }

    /**
     * Do the front edge of a sender belt and the back edge of a receiver belt meet? The front edge is
     * {@code senderOutput} above the sender's level, the back edge {@code receiverInput} above the receiver's level.
     */
    public static boolean connects(int senderLevel, int senderOutput, int receiverLevel, int receiverInput) {
        return senderLevel + senderOutput == receiverLevel + receiverInput;
    }
}
