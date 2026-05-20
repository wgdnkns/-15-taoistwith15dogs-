package com.wgdnkns.taoist.network;

import java.util.UUID;

public class TalismanSyncHandler {
    private static volatile UUID selectedFollowerUuid = null;
    private static volatile UUID commandedTargetUuid = null;

    public static void setSelectedFollowerUuid(UUID uuid) {
        selectedFollowerUuid = uuid;
    }

    public static UUID getSelectedFollowerUuid() {
        return selectedFollowerUuid;
    }

    public static void setCommandedTargetUuid(UUID uuid) {
        commandedTargetUuid = uuid;
    }

    public static UUID getCommandedTargetUuid() {
        return commandedTargetUuid;
    }
}
