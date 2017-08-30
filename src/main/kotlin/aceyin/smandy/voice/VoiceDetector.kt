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
    private val SNOWBOY_LIBRARY_NAME = "snowboy-detect-java"
    private val baseDir = Conf.str(Conf.Keys.BASE_DIR)
    private val audioFormat: AudioFormat
    private val targetInfo: DataLine.Info
    private val detector: SnowboyDetect
    private val targetLine: TargetDataLine
    // 连续100次静音检测之后，进入到待机状态
    private val SILENCE_TIMES_BEFORE_STANDBY = 100
    private val sienceCounter = AtomicInteger(0)
    // 待机状态
    private val standby = AtomicBoolean(true)
    private val logTimeHolder = AtomicLong(0)

    @Volatile private var running = AtomicBoolean(false)

    init {
        try {
            log.info("Loading snowboy native library: $SNOWBOY_LIBRARY_NAME")
            System.loadLibrary("snowboy-detect-java")
        } catch (e: Exception) {
            log.warn("Error while loading Snowboy JNI library")
            e.printStackTrace()
            System.exit(1)
        }

        if (baseDir.isNullOrEmpty()) {
            log.warn("Cannot find system property '${Conf.Keys.BASE_DIR}', exit 1")
            System.exit(1)
        }

        audioFormat = AudioFormat(16000F, 16, 1, true, false)
        targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        detector = SnowboyDetect("$baseDir/lib/resources/common.res", "$baseDir/lib/resources/alexa.umdl").apply {
            SetSensitivity("0.5")
            SetAudioGain(1f)
        }
        targetLine = AudioSystem.getLine(targetInfo) as TargetDataLine;
    }

    override fun run() {
        if (running.get()) return
        try {
            log.info("Starting Record Audio Input ... ")
            targetLine.open(audioFormat)
            targetLine.start()
            running.set(true)

            while (true) {
                if (standby.get()) {
                    listenOnWakeupWord()
                } else {
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
    fun isSilence(data: ByteArray): Boolean {
        val tdspFormat = TarsosDSPAudioFormat(16000f, 16, 1, true, false)
        val voiceFloatArr = FloatArray(data.size / tdspFormat.frameSize)
        val audioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(tdspFormat)
        audioFloatConverter.toFloatArray(data.clone(), voiceFloatArr)
        val silenceDetector = SilenceDetector(-35.0, false)
        return silenceDetector.isSilence(voiceFloatArr)
    }

    /**
     * 监听唤醒词。
     * 当系统刚刚启动，或者已经进入到候机状态之后，将语音检测切换到监听唤醒词状态。
     *
     */
    fun listenOnWakeupWord() {
        printTimedLog("Listen on wakeup word", 10000)
        val frameLen = 3200
        // Reads 0.2 second of audio in each call.
        val targetData = ByteArray(frameLen)
        val snowboyData = ShortArray(frameLen / 2)
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        val numBytesRead = targetLine.read(targetData, 0, targetData.size)

        if (numBytesRead == -1) {
            log.error("Fails to read audio data. Check if the audio hardware is OK")
            return
        }

        val isSilence = isSilence(targetData)
        if (isSilence) {
            updateSilenceCounter()
            return
        }

        log.info("Sound detected, starting to check wakeup words")
        // for test
        VoiceHandler.onOtherVoice(targetData.clone())
        //

        // Converts bytes into int16 that Snowboy will read.
        ByteBuffer.wrap(targetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)

        // Detection.
        val result = detector.RunDetection(snowboyData, snowboyData.size)
        if (result > 0) {
            log.info("Wakeup words detected")
            standby.set(false)
            sienceCounter.set(0)
        }
    }

    /**
     * 监听用户指令。
     * 当程序被唤醒词激活之后，将语音检测切换到监听用户指令状态.
     * 处在检测用户命令状态的时候，侦听的语音时间会被设置为20秒钟
     */
    fun listenOnUserCommand() {
        printTimedLog("Listen on user command", 3000)
        // 侦听的语音长度为 每秒 16000帧*20秒
        val frameLen = 16000 * 20
        // Reads 0.2 second of audio in each call.
        val targetData = ByteArray(frameLen)
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        val numBytesRead = targetLine.read(targetData, 0, targetData.size)

        if (numBytesRead == -1) {
            log.error("Fails to read audio data. Check if the audio hardware is OK")
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
        val num = sienceCounter.incrementAndGet()
        if (num > SILENCE_TIMES_BEFORE_STANDBY) {
            standby.set(true)
            sienceCounter.set(0)
            if (!standby.get()) {
                log.info("Switching to STANDBY model")
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
}