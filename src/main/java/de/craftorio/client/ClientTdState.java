package de.craftorio.client;

import de.craftorio.network.TdStatusPayload;

/** Last tower defense status received from the server. Plain Java so common code can reference it. */
public final class ClientTdState {
    private static volatile TdStatusPayload status = TdStatusPayload.NONE;

    private ClientTdState() {
    }

    public static void update(TdStatusPayload payload) {
        status = payload;
    }

    public static void clear() {
        status = TdStatusPayload.NONE;
    }

    public static TdStatusPayload status() {
        return status;
    }
}
