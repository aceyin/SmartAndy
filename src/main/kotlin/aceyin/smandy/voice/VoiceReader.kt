package aceyin.smandy.voice

import aceyin.smandy.client.socket.AliASRClient
import org.slf4j.LoggerFactory
import java.util.concurrent.atomic.AtomicInteger


/**
 * the main voice detector processor
 */
object VoiceReader : Runnable {
    private val log = LoggerFactory.getLogger("SmartAndy")
    // 连续100次静音检测之后，进入到待机状态
    private val SILENCE_TIMES_BEFORE_STANDBY = 100
    // 采样频率为每秒 16000 帧，因此采样 3200 帧数据，需要 0.2 秒
    private val frameLen = 3200
    // 用户指令时间间隔(单位：毫秒)。
    // 即: 连续静音时间超过这个值之后即被认为用户当前的语音输入已经完成
    private val STOP_TALKING_THRESHOLD = 1500
    private var running = false

    // 用户指令缓存
    private val voiceCache = VoiceCache()
    private val silenceCounter = SilenceCounter()
    private val stateHolder = ReaderState()


    /**
     * 启动语音监听线程，开始接受麦克风的语音数据
     */
    override fun run() {
        if (running) return

        running = true

        while (true) {
            val voiceData = Microphone.readVoice(frameLen)
            // 如果是静音，继续下一轮语音读取
            if (SilenceChecker.isSilence(voiceData)) {
                val count = silenceCounter.increase()
                if (count >= SILENCE_TIMES_BEFORE_STANDBY) {
                    stateHolder.switchTo(State.sleep)
                    // TODO 播放提示音，让用户知道系统进入休眠
                }
                continue
            }

            when (stateHolder.state) {
                State.sleep -> {
                    log.info("检测到声音，开始识别是否是唤醒词...")
                    val wakeup = VoiceAnalyzer.isWakeupWord(voiceData, frameLen)
                    // 只有识别到唤醒词之后，才会去处理用户语音指令，免得对杂音也进行处理
                    if (wakeup) {
                        // 系统被热词唤醒之后，将语音监听切换到 读取用户指令 模式
                        stateHolder.switchTo(State.listening)
                        silenceCounter.reset()
                        // TODO 播放提示音，让用户可以开始输入指令
                    }
                }
                State.listening -> {
                    log.info("等待用户指令...")
                    readUserCommand(voiceData)
                }
                State.understanding -> {
                    // TODO 播放提示音，告诉用户正在理解指令
                    log.info("处理用户指令...")
                }
                else -> {
                    log.info("未知状态...")
                }
            }
        }
    }

    /* 读取用户指令 */
    private fun readUserCommand(data: ByteArray) {
        // TODO 控制最长只能输入多久的语音，避免内存被撑爆
        // 静音时间超过了阈值，系统认为用户已经完成整个命令输入
        // 将捕获到的语音合并起来，发送给处理程序处理
        if (silenceCounter.silenceTime() > STOP_TALKING_THRESHOLD) {
            val command = voiceCache.takeAll()
            // 调用阿里云的语音转文字接口，解析语音
            AliASRClient.startAsr(command)
        }
        // 还在静音阈值之内，继续将语音数据存入队列
        else {
            voiceCache.add(data)
        }
    }

    /* 状态 */
    private enum class State(val mean: String) {
        sleep("待机中"), listening("听取用户指令"), understanding("理解用户输入")
    }

    /* 状态机 */
    private class ReaderState {
        var state: State = State.sleep
            private set

        /* 切换状态 */
        fun switchTo(state: State) {
            this.state = state
            when (state) {
                State.sleep -> {
                    log.info("切换到待机模式...")
                }
                State.listening -> {
                    log.info("切换到工作模式...")
                }
                else -> {
                }
            }
        }
    }

    /* 静音计数器 */
    private class SilenceCounter {
        /* 麦克风连续静音次数。按照预设的麦克风数据读取频率(每次读取0.2秒数据) */
        private val continuousSilenceCount = AtomicInteger(0)

        /* 增加静音次数 */
        fun increase(): Int {
            return continuousSilenceCount.incrementAndGet()
        }

        /* 重置静音次数 */
        fun reset() {
            continuousSilenceCount.set(0)
        }

        /* 获取连续静音的时间. 采样频率为每秒 16000 帧，因此采样 3200 帧数据，需要 0.2 秒 */
        fun silenceTime(): Long {
            return continuousSilenceCount.get() * 200L
        }
    }

    /* 语音数据缓存 */
    private class VoiceCache() {
        private val CACHE = mutableListOf<ByteArray>()
        private var CACHE_SIZE = 0

        /* 将语音数据存入缓存 */
        fun add(data: ByteArray) {
            CACHE.add(data)
            CACHE_SIZE += data.size
        }

        /* 从缓存中取出所有数据 */
        fun takeAll(): ByteArray {
            var all = ByteArray(CACHE_SIZE)
            CACHE.forEach {
                all += it
            }
            CACHE.clear()
            return all
        }

    }

}