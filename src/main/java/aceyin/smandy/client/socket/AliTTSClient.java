package aceyin.smandy.client.socket;

import com.alibaba.idst.nls.NlsFuture;
import com.alibaba.idst.nls.event.NlsEvent;
import com.alibaba.idst.nls.protocol.NlsRequest;
import com.alibaba.idst.nls.protocol.NlsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;

/**
 * Created by ace on 2017/8/30.
 */
public class AliTTSClient extends BaseAliVoiceClient {

    private Logger log = LoggerFactory.getLogger("SmartAndy");
    private String API_KEY = "nls-service";
    //    private NlsClient client = new NlsClient();
    private String tts_text = "回乡偶书。少小离家老大回，乡音无改鬓毛衰。儿童相见不相识，笑问客从何处来。";


    public AliTTSClient() {
        System.out.println("init Nls client...");
        // 初始化NlsClient
//        client.init();
    }

    public void shutDown() {
        System.out.println("close NLS client");
        // 关闭客户端并释放资源
        client.close();
        System.out.println("demo done");
    }


    public void startTTS() {
        File file = new File("/tmp/tts123.wav");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


//        File file = createVoiceFile();

        NlsRequest req = new NlsRequest();
        // 设置语音文件格式
        req.setAppKey(API_KEY);
        //传入测试文本，返回语音结果
        req.setTtsRequest(tts_text);
        //返回语音数据格式，支持pcm,wav.alaw
        req.setTtsEncodeType("wav");
        //音量大小默认50，阈值0-100
        req.setTtsVolume(30);
        //语速，阈值-500~500
        req.setTtsSpeechRate(0);
        //背景音乐编号,偏移量
        req.setTtsBackgroundMusic(1, 0);
        // Access Key ID和Access Key Secret
        req.authorize("LTAIsr2SrukJKTh1", "2IyzOJKDUm1phkCBou9T6ZWBiCTGpR");

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            // 实例化请求,传入请求和监听器
            NlsFuture future = client.createNlsFuture(req, this);
            int total_len = 0;

            byte[] data = future.read();

            do {
                if (data != null) {
                    fileOutputStream.write(data, 0, data.length);
                    total_len += data.length;
                    log.info("tts length " + data.length);
                    data = future.read();
                }
            } while (data != null);

            fileOutputStream.close();
            log.info("tts audio file size is :" + total_len);
            // 设置服务端结果返回的超时时间
            future.await(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
        //////////////////////////////////
//        NlsRequest req = new NlsRequest();
//        String appkey = "nls-service";
//        req.setAppKey(appkey); // 设置语音文件格式
//        req.setTtsRequest(tts_text); //传入测试文本，返回语音结果
//        req.setTtsEncodeType("wav");//返回语音数据格式，支持pcm,wav.alaw
//        req.setTtsVolume(30);       //音量大小默认50，阈值0-100
//        req.setTtsSpeechRate(0);    //语速，阈值-500~500
//        req.setTtsBackgroundMusic(1, 0);//背景音乐编号,偏移量
//        req.authorize("LTAIsr2SrukJKTh1", "2IyzOJKDUm1phkCBou9T6ZWBiCTGpR"); // 请替换为用户申请到的Access Key ID和Access Key Secret
//        try {
//            FileOutputStream fileOutputStream = new FileOutputStream(file);
//            NlsFuture future = client.createNlsFuture(req, this); // 实例化请求,传入请求和监听器
//            int total_len = 0;
//            byte[] data = future.read();
//            do {
//                if (data != null) {
//                    fileOutputStream.write(data, 0, data.length);
//                    total_len += data.length;
//                    System.out.println("tts length " + data.length);
//                    data = future.read();
//                }
//            } while (data != null);
//            fileOutputStream.close();
//            System.out.println("tts audio file size is :" + total_len);
//            future.await(10000); // 设置服务端结果返回的超时时间
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }

    private File createVoiceFile() {
        File file = new File("/tmp/tts.wav");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return file;
    }

    @Override
    public void onMessageReceived(NlsEvent e) {
        NlsResponse response = e.getResponse();
        int statusCode = response.getStatus_code();
        String result = "";
        if (response.getTts_ret() != null) {
            result += "get tts result: statusCode=[" + statusCode + "], " + response.getTts_ret();
        }
//        if (result != null) {
//            System.out.println(result);
//        } else {
//            System.out.println(response.jsonResults.toString());
//        }

        ////////////////
//        NlsResponse response = e.getResponse();
//        int statusCode = response.getStatus_code();
//        String result = "";
//        if (response.getTts_ret() != null) {
//            result += "\nget tts result: statusCode=[" + statusCode + "], " + response.getTts_ret();
//        }
//
//        if (result != null) {
//            System.out.println(result);
//        } else {
//            System.out.println(response.jsonResults.toString());
//        }
    }

    @Override
    public void onChannelClosed(NlsEvent e) {
        log.info("client web socket closed");
    }

    @Override
    public void onOperationFailed(NlsEvent e) {
        log.info("调用文字转语音接口失败：response json:${e.response?.jsonResults},message=${e.errorMessage}");

        //识别失败的回调
//        System.out.print("on operation failed: ");
//        System.out.println(e.getErrorMessage());
    }
}
