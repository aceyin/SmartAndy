package aceyin.smandy.client.socket;

import com.alibaba.idst.nls.NlsFuture;
import com.alibaba.idst.nls.event.NlsEvent;
import com.alibaba.idst.nls.protocol.NlsRequest;
import com.alibaba.idst.nls.protocol.NlsResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by ace on 2017/8/30.
 */
public class AliASRClient extends BaseAliVoiceClient {
    private Logger log = LoggerFactory.getLogger("SmartAndy");
    // 调用语音服务的API的KEY，阿里云用来识别你是调用哪个语音服务
    // 参考： https://help.aliyun.com/document_detail/30420.html?spm=5176.doc58029.6.545.cj1lpd
    private String API_KEY = "nls-service";

    public void startAsr(byte[] data) {
        //开始发送语音
        log.info("Start to call Alibaba ASR service");

        try {
            NlsRequest req = new NlsRequest();
            // appkey请从 "快速开始" 帮助页面的appkey列表中获取
            req.setApp_key(API_KEY);
            // 设置语音文件格式为pcm,我们支持16k 16bit 的无头的pcm文件
            req.setAsrFormat("pcm");

            /* 热词相关配置 */
            // req.setAsrUserId("useridSetByUser")
            // 热词词表id
            // req.setAsrVocabularyId("热词词表id")
            /* 热词相关配置 */

            req.authorize(APP_ACCESS_KEY, APP_ACCESS_SECRET);
            NlsFuture future = client.createNlsFuture(req, this);
            // 发送语音数据
            future.sendVoice(data, 0, data.length);
            // 语音识别结束时，发送结束符
            future.sendFinishSignal();
            log.info("main thread enter waiting for less than 10s.");
            // 设置服务端结果返回的超时时间
            future.await(10000);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessageReceived(NlsEvent e) {
        //识别结果的回调
        NlsResponse response = e.getResponse();
        int statusCode = response.getStatus_code();
        String asr_ret = response.getAsr_ret();
        if (asr_ret != null) {
            log.info("get asr result: statusCode=[" + statusCode + "], " + asr_ret);
        } else {
            log.info(response.jsonResults.toString());
        }
    }

    @Override
    public void onOperationFailed(NlsEvent e) {
        //识别失败的回调
        log.warn("调用语音识别接口失败: statusCode=[" + e.getResponse().getStatus_code() + " ], message=" + e.getErrorMessage());
    }

    @Override
    public void onChannelClosed(NlsEvent e) {
        log.info("on web socket closed.");
    }
}
