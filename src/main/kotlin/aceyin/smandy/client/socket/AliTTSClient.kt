package aceyin.smandy.client.socket

import com.alibaba.idst.nls.event.NlsEvent
import com.alibaba.idst.nls.protocol.NlsRequest
import org.slf4j.LoggerFactory
import java.io.File
import java.io.FileOutputStream


/**
 * 阿里云 文字转语音(TTS)客户端
 */
object AliTTSClient : BaseAliVoiceClient() {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val API_KEY = "nls-service"
    private val tts_text = "回乡偶书。少小离家老大回，乡音无改鬓毛衰。儿童相见不相识，笑问客从何处来。"

    fun startTTS() {
        val file = createVoiceFile()

        val req = NlsRequest().apply {
            // 设置语音文件格式
            appKey = API_KEY
            //传入测试文本，返回语音结果
            ttsRequest = tts_text
            //返回语音数据格式，支持pcm,wav.alaw
            setTtsEncodeType("wav")
            //音量大小默认50，阈值0-100
            setTtsVolume(30)
            //语速，阈值-500~500
            setTtsSpeechRate(0)
            //背景音乐编号,偏移量
            setTtsBackgroundMusic(1, 0)
            // Access Key ID和Access Key Secret
            // "LTAIsr2SrukJKTh1", "2IyzOJKDUm1phkCBou9T6ZWBiCTGpR"
            authorize(APP_ACCESS_KEY, APP_ACCESS_SECRET)
        }

        try {
            val fileOutputStream = FileOutputStream(file)
            // 实例化请求,传入请求和监听器
            val future = client.createNlsFuture(req, this)
            var total_len = 0

            var data = future.read()

            do {
                if (data != null) {
                    fileOutputStream.write(data, 0, data.size)
                    total_len += data.size
                    log.info("tts length ${data.size}")
                    data = future.read()
                }
            } while (data != null)

            fileOutputStream.close()
            log.info("tts audio file size is :$total_len")
            // 设置服务端结果返回的超时时间
            future.await(10000)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createVoiceFile(): File {
        val file = File("/tmp/tts33333.wav")
        if (!file.exists()) {
            try {
                file.createNewFile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return file
    }

    override fun onOperationFailed(e: NlsEvent) {
        log.info("调用文字转语音接口失败：response json:${e.response?.jsonResults},message=${e.errorMessage}")
    }
}