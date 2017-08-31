package aceyin.smandy.voice

import aceyin.smandy.Conf
import ai.kitt.snowboy.SnowboyDetect
import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat
import org.slf4j.LoggerFactory
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine


/**
 * the main voice detector processor
 */
object VoiceDetector : Runnable {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val baseDir = Conf.str(Conf.Keys.BASE_DIR.key)
    private val audioFormat: AudioFormat
    private val targetInfo: DataLine.Info
    private val detector: SnowboyDetect
    private val targetLine: TargetDataLine
    // 连续100次静音检测之后，进入到待机状态
    private val SILENCE_TIMES_BEFORE_STANDBY = 100
    private val silenceCounter = AtomicInteger(0)
    // 待机状态
    private val standby = AtomicBoolean(true)
    private val logTimeHolder = AtomicLong(0)

    @Volatile private var running = AtomicBoolean(false)

    init {
        log.info("启动语音监测程序...")
        audioFormat = AudioFormat(16000F, 16, 1, true, false)
        targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        detector = SnowboyDetect("$baseDir/lib/resources/common.res", "$baseDir/lib/resources/alexa.umdl").apply {
            // 检测敏感度，会越高越容易识别，但也容易失败
            // 参考：http://docs.kitt.ai/snowboy/#what-is-detection-sensitivity
            SetSensitivity("0.9")
            // 设置麦克风获取的音量，越大获取的声音越高
            // 参考: http://docs.kitt.ai/snowboy/#what-is-detection-sensitivity 查找关键字 audio gain
            SetAudioGain(5f)
        }
        targetLine = AudioSystem.getLine(targetInfo) as TargetDataLine
    }

    override fun run() {
        if (running.get()) return
        try {
            log.info("等待语音输入 ... ")
            targetLine.open(audioFormat)
            targetLine.start()
            running.set(true)

            while (true) {
                if (standby.get()) {
                    listenOnWakeupWord()
                } else {
                    log.info("等待用户指令...")
                    listenOnUserCommand()
                }
            }
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Check if microphone is silence.
     * @param data ByteArray - the data captured by microphone
     * @return true, if is silence
     */
    private val tdspFormat = TarsosDSPAudioFormat(16000f, 16, 1, true, false)
    private val audioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(tdspFormat)
    // 检测静音的分贝数量，数字越小则敏感度越高
    private val silenceDb = -50.0

    fun isSilence(data: ByteArray): Boolean {
        val voiceFloatArr = FloatArray(data.size / tdspFormat.frameSize)
        audioFloatConverter.toFloatArray(data.clone(), voiceFloatArr)
        val silenceDetector = SilenceDetector(silenceDb, false)
        return silenceDetector.isSilence(voiceFloatArr)
    }

    /**
     * 监听唤醒词。
     * 当系统刚刚启动，或者已经进入到候机状态之后，将语音检测切换到监听唤醒词状态。
     *
     */
    fun listenOnWakeupWord() {
        val frameLen = 3200
        // Reads 0.2 second of audio in each call.
        val targetData = ByteArray(frameLen)
        val snowboyData = ShortArray(frameLen / 2)
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        val numBytesRead = targetLine.read(targetData, 0, targetData.size)

        if (numBytesRead == -1) {
            log.error("从麦克风读取语音数据失败，请检查硬件设备是否正常。")
            return
        }

        val isSilence = isSilence(targetData)
        if (isSilence) {
            updateSilenceCounter()
            return
        }

        log.info("检测到声音，开始识别是否是唤醒词...")

        // Converts bytes into int16 that Snowboy will read.
        ByteBuffer.wrap(targetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)

        // Detection.
        val result = detector.RunDetection(snowboyData, snowboyData.size)
        if (result > 0) {
            log.info("检测到唤醒词，开始唤醒服务")
            standby.set(false)
            silenceCounter.set(0)
        }
    }

    /**
     * 监听用户指令。
     * 当程序被唤醒词激活之后，将语音检测切换到监听用户指令状态.
     * 处在检测用户命令状态的时候，侦听的语音时间会被设置为20秒钟
     */
    fun listenOnUserCommand() {
        // 侦听的语音长度为 每秒 16000帧*20秒
        val frameLen = 16000 //* 20
        // Reads 0.2 second of audio in each call.
        val targetData = ByteArray(frameLen)
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        val numBytesRead = targetLine.read(targetData, 0, targetData.size)

        if (numBytesRead == -1) {
            log.error("从麦克风读取语音数据失败，请检查硬件设备是否正常。")
            return
        }

        val isSilence = isSilence(targetData)
        if (isSilence) {
            updateSilenceCounter()
            return
        }
        VoiceHandler.onOtherVoice(targetData)
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

    private fun printTimedLog(message: String, timeGap: Long = 1000) {
        val now = System.currentTimeMillis()
        if (now - logTimeHolder.get() > timeGap) {
            log.info(message)
            logTimeHolder.set(now)
        }
    }

//    private fun originalSnowboyCode() {
    //原始代码
//        try {
//            targetLine.open(audioFormat)
//            targetLine.start()
//            println("Record Audio Input Started ")
//
//            // Reads 0.1 second of audio in each call.
//            val targetData = ByteArray(3200)
//            val snowboyData = ShortArray(1600)
//            var numBytesRead: Int
//
//            while (true) {
//                // Reads the audio data in the blocking mode. If you are on a very slow
//                // machine such that the hotword detector could not process the audio
//                // data in real time, this will cause problem...
//                numBytesRead = targetLine.read(targetData, 0, targetData.size)
//
//                if (numBytesRead == -1) {
//                    print("Fails to read audio data.")
//                    break
//                }
//
//                // Converts bytes into int16 that Snowboy will read.
//                ByteBuffer.wrap(targetData).order(
//                        ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)
//
//                // Detection.
//                val result = detector.RunDetection(snowboyData, snowboyData.size)
//                if (result > 0) {
//                    //System.out.print("Hotword " + result + " detected!\n");
//                    print("Hello, Andy \n")
//                }
//            }
//        } catch(e: Exception) {
//            e.printStackTrace()
//        }
//    }
}