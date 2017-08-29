package aceyin.smandy

object Conf {
    /**
     * System config keys
     */
    object Keys {
        const val BASE_DIR = "app.base.dir"
    }

    /**
     * Get a system property with the specified key.
     * If not found, return the value specified by 'default' parameter.
     */
    fun str(key: String, default: String = ""): String = System.getProperty(key, default)

    /**
     * Get a system property with the specified key and return an int value.
     * If not found, return the value specified by 'default' parameter.
     */
    fun int(key: String, default: Int = 0): Int {
        val v = System.getProperty(key)
        return if (v == null || v.isNullOrEmpty()) default else {
            v.toIntOrNull() ?: default
        }
    }
}