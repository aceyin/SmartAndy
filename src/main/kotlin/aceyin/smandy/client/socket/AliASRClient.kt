package aceyin.smandy.client.socket

import com.alibaba.idst.nls.event.NlsEvent
import com.alibaba.idst.nls.protocol.NlsRequest
import org.slf4j.LoggerFactory


/**
 * 阿里云 语音识别(ASR,语音转文字) 客户端
 */
object AliASRClient : BaseAliVoiceClient() {
    private val log = LoggerFactory.getLogger("SmartAndy")
    // 调用语音服务的API的KEY，阿里云用来识别你是调用哪个语音服务
    // 参考： https://help.aliyun.com/document_detail/30420.html?spm=5176.doc58029.6.545.cj1lpd
    private val API_KEY = "nls-service"

    fun startAsr(data: ByteArray) {
        //开始发送语音
        log.info("Start to call Alibaba ASR service")

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
        log.warn("调用语音识别接口失败: statusCode=[${e.response.status_code} ], message=${e.errorMessage}")
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

    override fun onChannelClosed(e: NlsEvent) {
        log.info("on web socket closed.")
    }
}