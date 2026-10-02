package dev.cake.rawcost;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Shared client diagnostics; the bootstrap TCostMod remains the sole mod entrypoint. */
public final class TCost {
    public static final Logger LOG=LogManager.getLogger("TCost");
    private TCost() {}
}
