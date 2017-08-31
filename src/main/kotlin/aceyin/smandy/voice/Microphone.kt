package aceyin.smandy.voice

import org.slf4j.LoggerFactory
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * 麦克风处理类
 */
object Microphone {
    private val log = LoggerFactory.getLogger("SmartAndy")
    private val audioFormat: AudioFormat = AudioFormat(16000F, 16, 1, true, false)
    private val targetInfo: DataLine.Info
    private val targetLine: TargetDataLine

    init {
        log.info("启动麦克风读取程序...")
        targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        targetLine = AudioSystem.getLine(targetInfo) as TargetDataLine
        targetLine.open(audioFormat)
        targetLine.start()
    }

    /**
     * 获取麦克风的语音输入
     */
    fun readVoice(frameLen: Int): ByteArray {
        val voiceData = ByteArray(frameLen)
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        val numBytesRead = targetLine.read(voiceData, 0, voiceData.size)

        if (numBytesRead == -1) {
            log.error("从麦克风读取语音数据失败，请检查硬件设备是否正常。")
        }
        return voiceData
    }
}