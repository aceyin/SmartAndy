package aceyin.smandy

object Conf {
    /**
     * System config keys
     */
    enum class Keys(val key: String) {
        BASE_DIR("app.base.dir"),
        APP_ACCESS_KEY("app.access.key"),
        APP_ACCESS_SECRET("app.access.secret")
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