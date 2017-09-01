package aceyin.smandy

import aceyin.smandy.voice.VoiceReader
import org.slf4j.LoggerFactory

/**
 * Created by ace on 2017/8/29.
 */
object SmartAndy {
    private val log = LoggerFactory.getLogger("Smart Andy")

    @JvmStatic fun main(args: Array<String>) {
        Thread.currentThread().name = "程序主线程"
        log.info("程序启动中")
        checkSystemProperties()
        loadSystemLibrary()
        Thread(VoiceReader, "语音监测线程").start()
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
        log.info("加载语音处理库")
        try {
            System.loadLibrary("snowboy-detect-java")
        } catch (e: Exception) {
            log.warn("加载语音处理库失败")
            e.printStackTrace()
            System.exit(1)
        }
    }
}