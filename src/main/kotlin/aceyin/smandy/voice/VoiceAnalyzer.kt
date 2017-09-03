package aceyin.smandy.voice

import aceyin.smandy.Conf
import ai.kitt.snowboy.SnowboyDetect
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 语音分析程序
 */
internal object VoiceAnalyzer {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val baseDir = Conf.str(Conf.Keys.BASE_DIR.key)
    private val detector: SnowboyDetect

    init {
        detector = SnowboyDetect("$baseDir/lib/resources/common.res", "$baseDir/lib/resources/alexa.umdl").apply {
            // 检测敏感度，会越高越容易识别，但也容易失败
            // 参考：http://docs.kitt.ai/snowboy/#what-is-detection-sensitivity
            SetSensitivity("0.5")
            // 设置麦克风获取的音量，越大获取的声音越高
            // 参考: http://docs.kitt.ai/snowboy/#what-is-detection-sensitivity 查找关键字 audio gain
            SetAudioGain(1f)
        }
    }

    /**
     * 分析是否当前语音是唤醒词
     */
    fun isWakeupWord(data: ByteArray, frameLen: Int): Boolean {
        // Converts bytes into int16 that Snowboy will read.
        val snowboyData = ShortArray(frameLen / 2)
        ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)

        // Detection.
        val result = detector.RunDetection(snowboyData, snowboyData.size)
        if (result > 0) {
            log.info("检测到唤醒词，开始唤醒服务")
            return true
        }

        return false
    }
}