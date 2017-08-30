package aceyin.smandy.voice;

import aceyin.smandy.Conf;
import ai.kitt.snowboy.SnowboyDetect;
import be.tarsos.dsp.SilenceDetector;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
import be.tarsos.dsp.io.TarsosDSPAudioFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.TargetDataLine;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ace on 2017/8/30.
 */
public class VoiceDetector implements Runnable {
    private Logger log = LoggerFactory.getLogger("SmartAndy");
    private String baseDir = Conf.str(Conf.Keys.BASE_DIR, "");
    private AudioFormat audioFormat;
    private DataLine.Info targetInfo;
    private SnowboyDetect detector;
    private TargetDataLine targetLine;
    // 连续100次静音检测之后，进入到待机状态
    private int SILENCE_TIMES_BEFORE_STANDBY = 100;
    private AtomicInteger sienceCounter = new AtomicInteger(0);
    // 待机状态
    private AtomicBoolean standby = new AtomicBoolean(true);
    private AtomicLong logTimeHolder = new AtomicLong(0);
    private volatile AtomicBoolean running = new AtomicBoolean(false);

    private VoiceHandler voiceHandler = new VoiceHandler();

    public VoiceDetector() {
        try {
            log.info("Loading snowboy native library: $SNOWBOY_LIBRARY_NAME");
            System.loadLibrary("snowboy-detect-java");

            audioFormat = new AudioFormat(16000F, 16, 1, true, false);
            targetInfo = new DataLine.Info(TargetDataLine.class, audioFormat);
            detector = new SnowboyDetect(baseDir + "/lib/resources/common.res", baseDir + "/lib/resources/alexa.umdl");

            detector.SetSensitivity("0.5");
            detector.SetAudioGain(1f);
            targetLine = (TargetDataLine) AudioSystem.getLine(targetInfo);
        } catch (Exception e) {
            log.warn("Error while loading Snowboy JNI library");
            e.printStackTrace();
            System.exit(1);
        }

        if (baseDir.isEmpty()) {
            log.warn("Cannot find system property '${Conf.Keys.BASE_DIR}', exit 1");
            System.exit(1);
        }
    }

    @Override
    public void run() {
        try {
            if (!running.get()) {
                log.info("Starting Record Audio Input ... ");
                targetLine.open(audioFormat);
                targetLine.start();
                running.set(true);

                while (true) {
                    if (standby.get()) {
                        listenOnWakeupWord();
                    } else {
                        listenOnUserCommand();
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Check if microphone is silence.
     *
     * @param data ByteArray - the data captured by microphone
     * @return true, if is silence
     */
    public Boolean isSilence(byte[] data) {
        TarsosDSPAudioFormat tdspFormat = new TarsosDSPAudioFormat(16000f, 16, 1, true, false);
        float[] voiceFloatArr = new float[data.length / tdspFormat.getFrameSize()];
        TarsosDSPAudioFloatConverter audioFloatConverter = TarsosDSPAudioFloatConverter.getConverter(tdspFormat);
        audioFloatConverter.toFloatArray(data.clone(), voiceFloatArr);
        SilenceDetector silenceDetector = new SilenceDetector(-35.0, false);
        return silenceDetector.isSilence(voiceFloatArr);
    }

    /**
     * 监听唤醒词。
     * 当系统刚刚启动，或者已经进入到候机状态之后，将语音检测切换到监听唤醒词状态。
     */
    public void listenOnWakeupWord() {
        printTimedLog("Listen on wakeup word", 10000);
        int frameLen = 3200;
        // Reads 0.2 second of audio in each call.
        byte[] targetData = new byte[frameLen];
        short[] snowboyData = new short[frameLen / 2];
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        int numBytesRead = targetLine.read(targetData, 0, targetData.length);

        if (numBytesRead == -1) {
            log.error("Fails to read audio data. Check if the audio hardware is OK");
            return;
        }

        boolean isSilence = isSilence(targetData);
        if (isSilence) {
            updateSilenceCounter();
            return;
        }

        log.info("Sound detected, starting to check wakeup words");
        // for test
        voiceHandler.onOtherVoice(targetData.clone());
        //

        // Converts bytes into int16 that Snowboy will read.
        ByteBuffer.wrap(targetData).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(snowboyData);

        // Detection.
        int result = detector.RunDetection(snowboyData, snowboyData.length);
        if (result > 0) {
            log.info("Wakeup words detected");
            standby.set(false);
            sienceCounter.set(0);
        }
    }

    /**
     * 监听用户指令。
     * 当程序被唤醒词激活之后，将语音检测切换到监听用户指令状态.
     * 处在检测用户命令状态的时候，侦听的语音时间会被设置为20秒钟
     */
    private void listenOnUserCommand() {
        printTimedLog("Listen on user command", 3000);
        // 侦听的语音长度为 每秒 16000帧*20秒
        int frameLen = 16000 * 20;
        // Reads 0.2 second of audio in each call.
        byte[] targetData = new byte[frameLen];
        // Reads the audio data in the blocking mode. If you are on a very slow
        // machine such that the hotword detector could not process the audio
        // data in real time, this will cause problem...
        int numBytesRead = targetLine.read(targetData, 0, targetData.length);

        if (numBytesRead == -1) {
            log.error("Fails to read audio data. Check if the audio hardware is OK");
            return;
        }

        boolean isSilence = isSilence(targetData);
        if (isSilence) {
            updateSilenceCounter();
            return;
        }
        voiceHandler.onOtherVoice(targetData);
    }

    private void updateSilenceCounter() {
        int num = sienceCounter.incrementAndGet();
        if (num > SILENCE_TIMES_BEFORE_STANDBY) {
            standby.set(true);
            sienceCounter.set(0);
            if (!standby.get()) {
                log.info("Switching to STANDBY model");
            }
        }
    }

    private void printTimedLog(String message, int timeGap) {
        long now = System.currentTimeMillis();
        if (now - logTimeHolder.get() > timeGap) {
            log.info(message);
            logTimeHolder.set(now);
        }
    }
}
