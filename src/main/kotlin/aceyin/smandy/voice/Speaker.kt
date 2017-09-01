package aceyin.smandy.voice

import aceyin.smandy.Conf
import sun.audio.AudioPlayer
import sun.audio.AudioStream
import java.io.File


/**
 * 扬声器
 */
object Speaker {
    /* 从待机进入工作状态的唤醒提示音 */
    private val WAKEUP_VOICE = AudioStream(File("${Conf.str(Conf.Keys.BASE_DIR.key)}/lib/resources/ding.wav").inputStream())
    /* 从工作进入待机状态的唤醒提示音 */
    private val SLEEP_VOICE = AudioStream(File("${Conf.str(Conf.Keys.BASE_DIR.key)}/lib/resources/dong.wav").inputStream())

    private fun play(stream: AudioStream) {
        AudioPlayer.player.start(stream)
    }

    /**
     * 播放唤醒提示音
     */
    fun notifyWakeup() {
        play(WAKEUP_VOICE)
    }

    /**
     * 播放休眠提示音
     */
    fun notifySleep() {
        play(SLEEP_VOICE)
    }
}