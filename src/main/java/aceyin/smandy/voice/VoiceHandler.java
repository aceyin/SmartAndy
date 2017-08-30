package aceyin.smandy.voice;

import aceyin.smandy.client.socket.AliASRClient;

/**
 * Created by ace on 2017/8/30.
 */
public class VoiceHandler {
    private AliASRClient asrClient = new AliASRClient();

    /**
     * 处理唤醒词(预设的热词:alexa)
     */
    public void onWakeupWord() {

    }

    /**
     * 处理非热词语音
     */
    public void onOtherVoice(byte[] data) {
        asrClient.startAsr(data);
    }
}
