package aceyin.smandy.client

import aceyin.smandy.Conf
import com.alibaba.idst.nls.NlsClient
import com.alibaba.idst.nls.event.NlsEvent
import com.alibaba.idst.nls.event.NlsListener
import com.alibaba.idst.nls.protocol.NlsRequest
import org.slf4j.LoggerFactory


/**
 * 阿里云 语音识别(ASR) Client
 */
object AliASRClient : NlsListener {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val client = NlsClient()
    private val APP_ACCESS_KEY = Conf.str(Conf.Keys.APP_ACCESS_KEY)
    private val APP_ACCESS_SECRET = Conf.str(Conf.Keys.APP_ACCESS_SECRET)
    // 调用语音服务的API的KEY，阿里云用来识别你是调用哪个语音服务
    // 参考： https://help.aliyun.com/document_detail/30420.html?spm=5176.doc58029.6.545.cj1lpd
    private const val API_KEY = "nls-service"

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

    fun startAsr(data: ByteArray) {
        //开始发送语音
        log.info("Start to call Alibaba ASR service")

        println("create NLS future")
        try {
            val req = NlsRequest()
            // appkey请从 "快速开始" 帮助页面的appkey列表中获取
            req.appKey = API_KEY
            // 设置语音文件格式为pcm,我们支持16k 16bit 的无头的pcm文件
            req.setAsrFormat("pcm")

            /* 热词相关配置 */
            // req.setAsrUserId("useridSetByUser")
            // 热词词表id
            // req.setAsrVocabularyId("热词词表id")
            /* 热词相关配置 */

            req.authorize(APP_ACCESS_KEY, APP_ACCESS_SECRET)
            val future = client.createNlsFuture(req, this)
            // 发送语音数据
            future.sendVoice(data, 0, data.size)
            // 语音识别结束时，发送结束符
            future.sendFinishSignal()
            log.info("main thread enter waiting for less than 10s.")
            // 设置服务端结果返回的超时时间
            future.await(10000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOperationFailed(e: NlsEvent) {
        //识别失败的回调
        println("on operation failed: statusCode=[${e.response.status_code} ], ${e.errorMessage}")
    }

    override fun onMessageReceived(e: NlsEvent) {
        //识别结果的回调
        val response = e.response
        val statusCode = response.status_code
        if (response.asr_ret != null) {
            log.info("get asr result: statusCode=[" + statusCode + "], " + response.asr_ret)
        } else {
            log.info(response.jsonResults.toString())
        }
    }

    override fun onChannelClosed(p0: NlsEvent?) {
        log.info("on websocket closed.")
    }
}