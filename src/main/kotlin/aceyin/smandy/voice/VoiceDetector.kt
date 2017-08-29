package aceyin.smandy.voice

import aceyin.smandy.Conf
import ai.kitt.snowboy.SnowboyDetect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * the main voice detector processor
 */
object VoiceDetector : Runnable {
    private val log = Logger.getLogger("VoiceDetector")
    private val SNOWBOY_LIBRARY_NAME = "snowboy-detect-java"
    private val baseDir = Conf.str(Conf.Keys.BASE_DIR)
    private val audioFormat: AudioFormat
    private val targetInfo: DataLine.Info
    private val detector: SnowboyDetect
    private val targetLine: TargetDataLine
    @Volatile private var running = AtomicBoolean(false)

    init {
        try {
            log.info("Loading snowboy native library: $SNOWBOY_LIBRARY_NAME")
            System.loadLibrary("snowboy-detect-java")
        } catch (e: Exception) {
            log.warning("Error while loading Snowboy JNI library")
            e.printStackTrace()
            System.exit(1)
        }

        if (baseDir.isNullOrEmpty()) {
            log.warning("Cannot find system property '${Conf.Keys.BASE_DIR}', exit 1")
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

            // Reads 0.1 second of audio in each call.
            val targetData = ByteArray(3200)
            val snowboyData = ShortArray(1600)
            var numBytesRead: Int

            while (true) {
                // Reads the audio data in the blocking mode. If you are on a very slow
                // machine such that the hotword detector could not process the audio
                // data in real time, this will cause problem...
                numBytesRead = targetLine.read(targetData, 0, targetData.size)

                if (numBytesRead == -1) {
                    log.warning("Fails to read audio data.")
                    break
                }

                // Converts bytes into int16 that Snowboy will read.
                ByteBuffer.wrap(targetData).order(
                        ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)

                // Detection.
                val result = detector.RunDetection(snowboyData, snowboyData.size)
                if (result > 0) {
                    print("Hello, Andy \n")
                }
            }
            running.set(true)
        } catch(e: Exception) {
            e.printStackTrace()
        }
    }
}