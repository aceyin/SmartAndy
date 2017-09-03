package aceyin.smandy.voice

import be.tarsos.dsp.SilenceDetector
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter
import be.tarsos.dsp.io.TarsosDSPAudioFormat

/**
 * 静音检车器
 */
internal object SilenceChecker {

    private val tdspFormat = TarsosDSPAudioFormat(16000f, 16, 1, true, false)
    private val audioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(tdspFormat)
    // 检测静音的分贝数量，数字越小则敏感度越高
    private val silenceDb = -70.0

    /**
     * Check if microphone is silence.
     * @param data ByteArray - the data captured by microphone
     * @return true, if is silence
     */
    fun isSilence(data: ByteArray): Boolean {
        val voiceFloatArr = FloatArray(data.size / tdspFormat.frameSize)
        audioFloatConverter.toFloatArray(data.clone(), voiceFloatArr)
        val silenceDetector = SilenceDetector(silenceDb, false)
        return silenceDetector.isSilence(voiceFloatArr)
    }
}