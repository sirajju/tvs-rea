package com.sxplus.rider.ble

/** The four keyless actions supported by the SmartXonnect protocol. */
enum class KeylessCommand(val label: String) {
    FIND_VEHICLE("Find Vehicle"),
    LOCK("Lock"),
    UNLOCK("Unlock"),
    OPEN_TRUNK("Open Seat/Trunk");
}
