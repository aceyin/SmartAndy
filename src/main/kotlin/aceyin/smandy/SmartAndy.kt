package aceyin.smandy

import aceyin.smandy.voice.VoiceDetector
import org.slf4j.LoggerFactory

/**
 * Created by ace on 2017/8/29.
 */
object SmartAndy {
    private val log = LoggerFactory.getLogger("Smart Andy")
    private val SNOWBOY_LIBRARY_NAME = "snowboy-detect-java"

    @JvmStatic fun main(args: Array<String>) {
        log.info("Smart Andy程序启动中...")
        checkSystemProperties()
        Thread(VoiceDetector, "语音监测线程").start()
//        AliTTSClient.startTTS()
    }

    /**
     * 检测必须的系统变量
     */
    @JvmStatic private fun checkSystemProperties() {
        var allReady = true
        val missedVarKeys = mutableListOf<String>()
        Conf.Keys.values().forEach {
            if (Conf.str(it.key).isNullOrEmpty()) {
                allReady = false
                missedVarKeys.add(it.key)
            }
        }

        if (!allReady) {
            missedVarKeys.forEach {
                log.error("缺少系统参数 '$it'")
            }
            System.exit(1)
        }
    }

    /**
     * 加载系统所需的程序库
     */
    @JvmStatic private fun loadSystemLibrary() {
        log.info("加载 snowboy 语音库: $SNOWBOY_LIBRARY_NAME")
        try {
            System.loadLibrary("snowboy-detect-java")
        } catch (e: Exception) {
            log.warn("加载 Snowboy 语音处理库失败")
            e.printStackTrace()
            System.exit(1)
        }
    }
}