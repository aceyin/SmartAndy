package aceyin.smandy.client.socket

import com.alibaba.idst.nls.event.NlsEvent
import com.alibaba.idst.nls.protocol.NlsRequest
import org.slf4j.LoggerFactory


/**
 * 阿里云自然语义理解(NLU) 客户端
 */
object AliNLUClient : BaseAliVoiceClient() {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val API_KEY = "nls-service"

    fun startNLU(data: ByteArray) {
        try {
            val req = NlsRequest().apply {
                // appkey请从 "快速开始" 帮助页面的appkey列表中获取
                appKey = API_KEY
                setAsrFormat("pcm") // 设置语音文件格式为pcm,我们支持16k 16bit 的无头的pcm文件。

                /*热词相关配置*/
                // setAsrUserId("useridSetByUser")
                // setAsrVocabularyId("热词词表id")//热词词表id
                /*热词相关配置*/

                /*或者单独使用nlu*/
                // 使用文本请求nlu结果
                setAsrFake("text")
                /*或者单独使用nlu*/

                // 设置nlu请求
                enableNLUResult()
                authorize(APP_ACCESS_KEY, APP_ACCESS_SECRET)
            }

            val future = client.createNlsFuture(req, this)
            if (req.asrFake == null) {
                future.sendVoice(data, 0, data.size)
                // 语音识别结束时，发送结束符
                future.sendFinishSignal()
            }
            future.await(10000) // 设置服务端结果返回的超时时间
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onOperationFailed(e: NlsEvent) {
        //识别失败的回调
        log.warn("调用自然语义理解接口失败: statusCode=[${e.response.status_code} ], message=${e.errorMessage}")
    }


}