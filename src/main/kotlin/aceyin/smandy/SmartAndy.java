package aceyin.smandy;

import ai.kitt.snowboy.SnowboyDetect;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class SmartAndy {
    static {
        String java_lib_path = System.getProperty("java.library.path", "");
        System.out.println("java.library.path=" + java_lib_path);
        String path = SmartAndy.class.getResource("/lib/libsnowboy-detect-java.dylib").getPath();
//        System.setProperty("java.library.path", java_lib_path + ":" + path.substring(0, path.lastIndexOf("libsnowboy-detect-java.dylib")));
        System.loadLibrary("snowboy-detect-java");
    }

    public static void main(String[] args) {
        String baseDir = SmartAndy.class.getResource("/").getPath();
        System.out.println("Starting Java SmartAndy @ " + baseDir);
        // Sets up audio.
        AudioFormat format = new AudioFormat(16000, 16, 1, true, false);
        DataLine.Info targetInfo = new DataLine.Info(TargetDataLine.class, format);

        // Sets up Snowboy.
        SnowboyDetect detector = new SnowboyDetect(baseDir + "common.res",
                baseDir + "alexa.umdl");
        detector.SetSensitivity("0.5");
        detector.SetAudioGain(1);

        try {
            System.out.println("Starting Record Audio Input...");
            TargetDataLine targetLine =
                    (TargetDataLine) AudioSystem.getLine(targetInfo);
            targetLine.open(format);
            targetLine.start();
            System.out.println("Record Audio Input Started ");

            // Reads 0.1 second of audio in each call.
            byte[] targetData = new byte[3200];
            short[] snowboyData = new short[1600];
            int numBytesRead;

            while (true) {
                // Reads the audio data in the blocking mode. If you are on a very slow
                // machine such that the hotword detector could not process the audio
                // data in real time, this will cause problem...
                numBytesRead = targetLine.read(targetData, 0, targetData.length);

                if (numBytesRead == -1) {
                    System.out.print("Fails to read audio data.");
                    break;
                }

                // Converts bytes into int16 that Snowboy will read.
                ByteBuffer.wrap(targetData).order(
                        ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData);

                // Detection.
                int result = detector.RunDetection(snowboyData, snowboyData.length);
                if (result > 0) {
                    //System.out.print("Hotword " + result + " detected!\n");
                    System.out.print("Hello, Andy \n");
                }
            }
        } catch (Exception e) {
            System.err.println(e);
        }
    }
}
