package aceyin.smandy.client.socket;

import aceyin.smandy.Conf;
import com.alibaba.idst.nls.NlsClient;
import com.alibaba.idst.nls.event.NlsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ace on 2017/8/30.
 */
abstract class BaseAliVoiceClient implements NlsListener {
    private Logger log = LoggerFactory.getLogger("SmartAndy");
    protected NlsClient client = new NlsClient();
    protected String APP_ACCESS_KEY = Conf.str(Conf.Keys.APP_ACCESS_KEY, "");
    protected String APP_ACCESS_SECRET = Conf.str(Conf.Keys.APP_ACCESS_SECRET, "");

    protected BaseAliVoiceClient() {
//        if (APP_ACCESS_KEY.isEmpty()) {
//            log.error("No 'app.access.key' configured, please set 'app.secret.key' in system properties");
//        }
//        if (APP_ACCESS_SECRET.isEmpty()) {
//            log.error("No 'app.access.secret' configured, please set 'app.secret.secret' in system properties");
//        }
        System.out.println("init Nls client...");
        client.init();
    }


    protected void shutdown() {
        client.close();
    }
}
