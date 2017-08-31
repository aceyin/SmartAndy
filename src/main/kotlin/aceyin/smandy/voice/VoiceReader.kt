package aceyin.smandy.voice

import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong


/**
 * the main voice detector processor
 */
object VoiceReader : Runnable {
    private val log = LoggerFactory.getLogger("SmartAndy")

    // 连续100次静音检测之后，进入到待机状态
    private val SILENCE_TIMES_BEFORE_STANDBY = 100
    private val silenceCounter = AtomicInteger(0)
    // 待机状态
    private val standby = AtomicBoolean(true)
    // 是否在读取用户指令
    private val readingUserCommand = AtomicBoolean(false)
    private val logTimeHolder = AtomicLong(0)
    // 侦听的语音长度为 0.2秒 (3200帧)
    private val frameLen = 3200


    @Volatile private var running = AtomicBoolean(false)

    override fun run() {
        if (running.get()) return
        running.set(true)
        while (true) {
            val voiceData = Microphone.readVoice(frameLen)
            // 根据当前是否为待机模式来决定应该是去识别 热词 还是 去识别 用户指令
            if (standby.get()) {
                checkHotWord(voiceData)
            } else {
                log.info("等待用户指令...")
                listenOnUserCommand(voiceData)
            }
        }
    }


    /**
     * 监听唤醒词。
     * 当系统刚刚启动，或者已经进入到候机状态之后，将语音检测切换到监听唤醒词状态。
     *
     */
    fun checkHotWord(data: ByteArray) {
        val isSilence = SilenceChecker.isSilence(data)
        if (isSilence) {
            updateSilenceCounter()
            return
        }
        log.info("检测到声音，开始识别是否是唤醒词...")
        val isHotWord = HotWordChecker.isHotWord(data, frameLen)
        if (isHotWord) {
            standby.set(false)
            silenceCounter.set(0)
            // 系统被热词唤醒之后，将语音监听切换到 读取用户指令 模式
            readingUserCommand.set(true)
        }
    }

    /**
     * 监听用户指令。
     * 当程序被唤醒词激活之后，将语音检测切换到监听用户指令状态.
     * 检测用户指令时，如果microphone 在2秒之内没有输入(silence) 则认为当前指令输入完毕，
     * 然后将在这期间内所有获取的语音数据合并，一起提交给 handler 处理
     */
    fun listenOnUserCommand(data: ByteArray) {
        val isSilence = SilenceChecker.isSilence(data)
        if (isSilence) {
            updateSilenceCounter()
            return
        }
        VoiceHandler.onUserCommand(data)
    }

    private fun updateSilenceCounter() {
        val num = silenceCounter.incrementAndGet()
        if (num > SILENCE_TIMES_BEFORE_STANDBY) {
            standby.set(true)
            silenceCounter.set(0)
            if (!standby.get()) {
                log.info("切换到待机模式...")
            }
        }
    }

    /*状态*/
    private enum class State(val mean: String) {
        standby("待机中"), read_user_command("读取用户指令")
    }

    /**
     * 状态机
     */
    private class ReaderState(val state: State) {
    }
}