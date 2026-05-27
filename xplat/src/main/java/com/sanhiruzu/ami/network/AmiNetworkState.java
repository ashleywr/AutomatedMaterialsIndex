package com.sanhiruzu.ami.network;

import com.sanhiruzu.ami.AmiCore;
public final class AmiNetworkState {
    private AmiNetworkState() {
    }

    /**
     * True when the server also has AMI installed (set by receiving AmiServerPingPacket).
     */
    public static boolean onServer = false;
}
