package aceyin.smandy

import aceyin.smandy.voice.VoiceDetector

/**
 * Created by ace on 2017/8/29.
 */
object SmartAndy {
    @JvmStatic
    fun main(args: Array<String>) {
        val detector = Thread(VoiceDetector, "Voice Detector Thread")
        detector.start()
    }
}