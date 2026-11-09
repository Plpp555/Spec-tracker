package io.github.plpp555.spectracker.util

/** /proc/cpuinfo の "CPU implementer" / "CPU part" からコア名を判定する。 */
object CpuNames {
    private const val ARM = 0x41
    private const val QUALCOMM = 0x51

    private val arm = mapOf(
        0xd03 to "Cortex-A53", 0xd04 to "Cortex-A35", 0xd05 to "Cortex-A55",
        0xd07 to "Cortex-A57", 0xd08 to "Cortex-A72", 0xd09 to "Cortex-A73",
        0xd0a to "Cortex-A75", 0xd0b to "Cortex-A76", 0xd0c to "Neoverse-N1",
        0xd0d to "Cortex-A77", 0xd0e to "Cortex-A76AE", 0xd41 to "Cortex-A78",
        0xd42 to "Cortex-A78AE", 0xd44 to "Cortex-X1", 0xd46 to "Cortex-A510",
        0xd47 to "Cortex-A710", 0xd48 to "Cortex-X2", 0xd4b to "Cortex-A78C",
        0xd4d to "Cortex-A715", 0xd4e to "Cortex-X3", 0xd80 to "Cortex-A520",
        0xd81 to "Cortex-A720", 0xd82 to "Cortex-X4", 0xd85 to "Cortex-X925",
        0xd87 to "Cortex-A725",
    )

    private val qualcomm = mapOf(
        0x001 to "Oryon", 0x800 to "Kryo 2xx Gold", 0x801 to "Kryo 2xx Silver",
        0x802 to "Kryo 3xx Gold", 0x803 to "Kryo 3xx Silver",
        0x804 to "Kryo 4xx Gold", 0x805 to "Kryo 4xx Silver",
    )

    /** 判定できなければ null。 */
    fun resolve(implementer: Int, part: Int): String? = when (implementer) {
        ARM -> arm[part]
        QUALCOMM -> qualcomm[part]
        else -> null
    }

    /** 判定できなくても、16 進数の識別子を返す。 */
    fun describe(implementer: Int, part: Int): String =
        resolve(implementer, part) ?: "implementer 0x%02x / part 0x%03x".format(implementer, part)
}
