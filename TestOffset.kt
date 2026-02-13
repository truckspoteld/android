
import java.time.ZoneId
import java.time.ZonedDateTime

fun main() {
    val zoneIds = listOf("America/Los_Angeles", "Pacific/Honolulu", "HST", "PST")
    for (zoneIdStr in zoneIds) {
        try {
            val zoneId = ZoneId.of(zoneIdStr)
            val zdt = ZonedDateTime.now(zoneId)
            val offset = zdt.offset
            println("Zone: $zoneIdStr, Offset ID: ${offset.id}, ToString: $offset")
        } catch (e: Exception) {
            println("Error for $zoneIdStr: ${e.message}")
        }
    }
}
