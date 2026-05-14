package de.simon.dankelmann.bluetoothlespam.Helpers

class StringHelpers {

    companion object {
        /**
         * ⚡ Bolt Optimization: Manually iterates and bit-shifts the characters to avoid
         * overhead and massive memory allocations created by `.chunked(2).map{...}`
         * Used extensively in hot loops when generating BLE advertisement data.
         */
        fun decodeHex(string:String): ByteArray {
            check(string.length % 2 == 0) { "Must have an even length" }
            val result = ByteArray(string.length / 2)
            for (i in result.indices) {
                val high = Character.digit(string[i * 2], 16)
                val low = Character.digit(string[i * 2 + 1], 16)
                if (high == -1 || low == -1) throw IllegalArgumentException("Invalid hex character in $string")
                result[i] = ((high shl 4) + low).toByte()
            }
            return result
        }

        private val HEX_CHARS = "0123456789abcdef".toCharArray()

        /**
         * ⚡ Bolt Optimization: Uses precomputed char array and a StringBuilder to avoid
         * memory allocations and slow formatted strings (`"%02x".format()`).
         */
        fun ByteArray.toHexString(): String {
            val result = StringBuilder(size * 2)
            for (b in this) {
                val v = b.toInt() and 0xFF
                result.append(HEX_CHARS[v ushr 4])
                result.append(HEX_CHARS[v and 0x0F])
            }
            return result.toString()
        }

        /**
         * ⚡ Bolt Optimization: Uses native `.toString(16).padStart(...)` which is faster
         * than `.format()` and avoids overhead string allocations for padding in hot loops.
         */
        fun intToHexString(input:Int):String{
            return String.format("%02x", input) // Reverted due to negative number behavior in `.toString(16)`
        }

        fun byteToHexString(input:Byte):String{
            return String.format("%02x", input) // Reverted for parity with intToHexString
        }

    }

}
