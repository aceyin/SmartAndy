package aceyin.smandy.voice

import aceyin.smandy.Conf
import sun.audio.AudioPlayer
import sun.audio.AudioStream
import java.io.File
import java.io.InputStream


/**
 * 扬声器
 */
object Speaker {
    /* 从待机进入工作状态的唤醒提示音 */
    private val WAKEUP_VOICE = File("${Conf.str(Conf.Keys.BASE_DIR.key)}/lib/resources/ding.wav")
    /* 从工作进入待机状态的唤醒提示音 */
    private val SLEEP_VOICE = File("${Conf.str(Conf.Keys.BASE_DIR.key)}/lib/resources/dong.wav")

    private fun play(stream: InputStream) {
        AudioPlayer.player.start(AudioStream(stream))
    }

    /**
     * 播放唤醒提示音
     */
    fun playWakeupSound() {
        play(WAKEUP_VOICE.inputStream())
    }

    /**
     * 播放休眠提示音
     */
    fun playSleepSound() {
        play(SLEEP_VOICE.inputStream())
    }
}