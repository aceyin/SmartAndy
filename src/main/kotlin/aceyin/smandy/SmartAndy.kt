package aceyin.smandy

import aceyin.smandy.client.socket.AliTTSClient

/**
 * Created by ace on 2017/8/29.
 */
object SmartAndy {
    @JvmStatic
    fun main(args: Array<String>) {
//        Thread(VoiceDetector, "Voice Detector Thread").start()
        AliTTSClient.startTTS()
    }
}