package aceyin.smandy.client.socket

import aceyin.smandy.Conf
import com.alibaba.idst.nls.NlsClient
import com.alibaba.idst.nls.event.NlsListener
import org.slf4j.LoggerFactory

/**
 * Created by ace on 2017/8/30.
 */
abstract class BaseAliVoiceClient : NlsListener {
    private val log = LoggerFactory.getLogger("SmartAndy")
    protected val client = NlsClient()
    protected val APP_ACCESS_KEY = Conf.str(Conf.Keys.APP_ACCESS_KEY)
    protected val APP_ACCESS_SECRET = Conf.str(Conf.Keys.APP_ACCESS_SECRET)

    init {
        if (APP_ACCESS_KEY.isNullOrEmpty()) {
            log.error("No 'app.access.key' configured, please set 'app.secret.key' in system properties")
        }
        if (APP_ACCESS_SECRET.isNullOrEmpty()) {
            log.error("No 'app.access.secret' configured, please set 'app.secret.secret' in system properties")
        }
        client.init()
    }


    fun shutdown() {
        client.close()
    }
}