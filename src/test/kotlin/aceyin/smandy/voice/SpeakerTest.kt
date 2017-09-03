package aceyin.smandy.voice

import aceyin.smandy.Conf

/**
 * Created by ace on 2017/9/1.
 */
object SpeakerTest {

    init {
        val path = SpeakerTest::class.java.getResource("/").path.replace("target/test-classes/", "")
        println(path)
        System.setProperty(Conf.Keys.BASE_DIR.key, path)
    }

    @JvmStatic fun main(args: Array<String>) {
        Speaker.playSleepSound()
        Thread.sleep(2000)
        Speaker.playWakeupSound()
        Thread.sleep(2000)

        Speaker.playSleepSound()
        Thread.sleep(2000)
        Speaker.playWakeupSound()
        Thread.sleep(2000)
    }
}