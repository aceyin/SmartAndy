package aceyin.smandy

import ai.kitt.snowboy.SnowboyDetect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

object Demo {
    @JvmStatic fun main(args: Array<String>) {

        val baseDir = Conf.str(Conf.Keys.BASE_DIR.key)
        val path = Demo::class.java.getResource("/").path.replace("target/test-classes/", "")
        System.setProperty(Conf.Keys.BASE_DIR.key, "/Users/ace/Documents/workspace/github/SmartAndy")
        System.setProperty("java.library.path", "/Users/ace/Documents/workspace/github/SmartAndy/lib")
        //        String java_lib_path = System.getProperty("java.library.path", "");
        //        String path = Demo.class.getResource("/lib/libsnowboy-detect-java.dylib").getPath();
        //        System.setProperty("java.library.path", java_lib_path + ":" + path.substring(0, path.lastIndexOf("libsnowboy-detect-java.dylib")));

        System.loadLibrary("snowboy-detect-java")

        println("Starting Java Demo")
        val hotword = "alexa"
        println("Starting Java Demo, Listening '$hotword' ...")
        // Sets up audio.
        val format = AudioFormat(16000f, 16, 1, true, false)
        val targetInfo = DataLine.Info(TargetDataLine::class.java, format)

        // Sets up Snowboy.
        val detector = SnowboyDetect("$baseDir/lib/resources/common.res", "$baseDir/lib/resources/alexa.umdl")
        detector.SetSensitivity("0.5")
        detector.SetAudioGain(1f)

        try {
            println("Starting Record Audio Input...")
            val targetLine = AudioSystem.getLine(targetInfo) as TargetDataLine
            targetLine.open(format)
            targetLine.start()
            println("Record Audio Input Started ")

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
                    print("Fails to read audio data.")
                    break
                }

//                if (SilenceChecker.isSilence(targetData.clone())) continue
//                println("检测到声音，开始识别是否是唤醒词...")

                // Converts bytes into int16 that Snowboy will read.
                ByteBuffer.wrap(targetData).order(
                        ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData)

                // Detection.
                val result = detector.RunDetection(snowboyData, snowboyData.size)
                if (result > 0) {
                    //System.out.print("Hotword " + result + " detected!\n");
                    print("Hello, Andy \n")
                }
            }
        } catch (e: Exception) {
            System.err.println(e)
        }


    }
}
