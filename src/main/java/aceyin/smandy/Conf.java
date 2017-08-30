package aceyin.smandy;

/**
 * Created by ace on 2017/8/30.
 */
public class Conf {

    public interface Keys {
        String BASE_DIR = "app.base.dir";
        String APP_ACCESS_KEY = "app.access.key";
        String APP_ACCESS_SECRET = "app.access.secret";
    }

    /**
     * Get a system property with the specified key.
     * If not found, return the value specified by 'default' parameter.
     */
    public static String str(String key, String def) {
        return System.getProperty(key, def);
    }

    /**
     * Get a system property with the specified key and return an int value.
     * If not found, return the value specified by 'default' parameter.
     */
    public static int ints(String key, int def) {
        String v = System.getProperty(key);
        int ret = def;
        if (v != null && v.matches("\\d+")) {
            ret = Integer.valueOf(v);
        }
        return ret;
    }
}
