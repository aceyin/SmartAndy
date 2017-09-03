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
    private val SILENCE_COUNT_BEFORE_SLEEP = 100
    // 采样频率为 16000 Hz
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

            when (stateHolder.state) {
                State.sleep -> {
                    val wakeup = VoiceAnalyzer.isWakeupWord(voiceData, frameLen)
                    // 只有识别到唤醒词之后，才会去处理用户语音指令，免得对杂音也进行处理
                    if (wakeup) {
                        // 系统被热词唤醒之后，将语音监听切换到 读取用户指令 模式
                        stateHolder.switchTo(State.listening)
                        silenceCounter.reset()
                    }
                }
                State.listening -> {
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

    var speak_started = false
    /* 读取用户指令 */
    private fun readUserCommand(data: ByteArray) {
        // TODO 控制最长只能输入多久的语音，避免内存被撑爆
        // 静音时间超过了阈值，系统认为用户已经完成整个命令输入
        // 将捕获到的语音合并起来，发送给处理程序处理
        if (silenceCounter.silenceTime() > STOP_TALKING_THRESHOLD && speak_started) {
            val command = voiceCache.takeAll()
            // 调用阿里云的语音转文字接口，解析语音
            stateHolder.switchTo(State.understanding)
            // 阻塞模式调用阿里云的语音接口
            AliASRClient.startAsr(command)
            silenceCounter.reset()
            // 调用完之后，切换到监听状态
            stateHolder.switchTo(State.listening)
        }
        // 还在静音阈值之内，继续将语音数据存入队列
        else {
            // 检查当前语音数据是否是静音
            if (SilenceChecker.isSilence(data)) {
                // 如果还没有开始说话，则等待10秒，10秒之内不说话，切换到待机状态
                if (!speak_started) {
                    val count = silenceCounter.increase()
                    if (silenceCounter.silenceTime() > 10000 && stateHolder.state == State.listening) {
                        log.info("长时间没有声音，进入待机状态...")
                        stateHolder.switchTo(State.sleep)
                        silenceCounter.reset()
                    }
                }
            } else {
                speak_started = true
                voiceCache.add(data)
                if (SilenceChecker.isSilence(data)) {
                    silenceCounter.increase()
                }
            }
        }
    }

    /* 状态 */
    private enum class State(val mean: String) {
        sleep("待机中"), listening("等待用户指令"), understanding("理解用户输入")
    }

    /* 状态机 */
    private class ReaderState {
        var state: State = State.sleep
            private set

        /* 切换状态 */
        fun switchTo(state: State) {
            this.state = state
            log.info("进入'${state.mean}'状态")
            when (this.state) {
                State.listening -> {
                    Speaker.playWakeupSound()
                }
                State.sleep -> {
                    Speaker.playSleepSound()
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

        /* 获取连续静音的时间 */
        fun silenceTime(): Long {
            // TODO 更改计算连续静音时间的方式
            // 平均来说，每次读取 3200 采样(16000Hz的1/5) 差不多要用130多毫秒
            // 因此这里计算连续静音的时间就简单的将 连续静音次数*200
            return continuousSilenceCount.get() * 200L
        }
    }

    /* 语音数据缓存 */
    private class VoiceCache {
        private val CACHE = mutableListOf<ByteArray>()
        private var CACHE_SIZE = 0

        /* 将语音数据存入缓存 */
        fun add(data: ByteArray) {
            CACHE.add(data)
            CACHE_SIZE += data.size
        }

        /* 从缓存中取出所有数据 */
        fun takeAll(): ByteArray {
            if (CACHE_SIZE == 0) return ByteArray(0)

            var all = ByteArray(CACHE_SIZE)
            CACHE.forEach { all += it }
            CACHE.clear()
            CACHE_SIZE = 0
            return all
        }

    }

}