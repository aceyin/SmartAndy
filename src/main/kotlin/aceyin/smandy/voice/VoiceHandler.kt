package aceyin.smandy.voice

import aceyin.smandy.client.socket.AliASRClient

/**
 * handle the voice
 */
object VoiceHandler {
    private val asrClient = AliASRClient
    /**
     * 处理唤醒词(预设的热词:alexa)
     */
    fun onWakeupWord() {

    }

    /**
     * 处理非热词语音
     */
    fun onUserCommand(data: ByteArray) {
        asrClient.startAsr(data)
    }
}