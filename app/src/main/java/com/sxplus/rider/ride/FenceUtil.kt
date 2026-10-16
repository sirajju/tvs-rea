package com.sxplus.rider.ride

import com.sxplus.rider.domain.Fence
import com.sxplus.rider.domain.FenceType
import java.util.Calendar

/**
 * If an enabled TIME fence is active right now (weekday + time window),
 * returns its overspeed limit; null if none active. Lowest limit wins.
 */
fun activeTimeFenceLimit(fences: List<Fence>): Int? {
    val cal = Calendar.getInstance()
    val nowMin = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
    val isoDay = (cal.get(Calendar.DAY_OF_WEEK) + 5) % 7 + 1 // 1=Mon .. 7=Sun

    return fences.asSequence()
        .filter { it.enabled && it.type == FenceType.TIME }
        .filter { it.weekdays.isEmpty() || isoDay in it.weekdays }
        .filter { withinWindow(nowMin, it.fromTime, it.toTime) }
        .map { it.overspeedLimit }
        .minOrNull()
}

private fun toMinutes(hhmm: String): Int? {
    val p = hhmm.split(":")
    if (p.size != 2) return null
    val h = p[0].toIntOrNull() ?: return null
    val m = p[1].toIntOrNull() ?: return null
    return h * 60 + m
}

private fun withinWindow(nowMin: Int, from: String, to: String): Boolean {
    val f = toMinutes(from) ?: return false
    val t = toMinutes(to) ?: return false
    return if (f <= t) nowMin in f..t else (nowMin >= f || nowMin <= t) // overnight window
}
