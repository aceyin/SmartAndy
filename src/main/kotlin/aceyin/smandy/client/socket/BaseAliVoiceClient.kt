package aceyin.smandy.client.socket

import aceyin.smandy.Conf
import com.alibaba.idst.nls.NlsClient
import com.alibaba.idst.nls.event.NlsEvent
import com.alibaba.idst.nls.event.NlsListener
import org.slf4j.LoggerFactory

/**
 * Created by ace on 2017/8/30.
 */
abstract class BaseAliVoiceClient : NlsListener {
    private val log = LoggerFactory.getLogger("SmartAndy")
    protected val client = NlsClient()
    protected val APP_ACCESS_KEY = Conf.str(Conf.Keys.APP_ACCESS_KEY.key)
    protected val APP_ACCESS_SECRET = Conf.str(Conf.Keys.APP_ACCESS_SECRET.key)

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

    override fun onChannelClosed(e: NlsEvent) {
        log.info("on web socket closed.")
    }


    /**
     * FIXME 如果在这个方法里面调用 response.jsonResults.toString() 将会导致调用失败。
     * 不知道为什么
     */
    override fun onMessageReceived(e: NlsEvent) {
        //识别结果的回调
        val response = e.response
        val statusCode = response.status_code
        val asr_ret = response.asr_ret
        log.info("get asr result: statusCode=[$statusCode], $asr_ret")
    }
}