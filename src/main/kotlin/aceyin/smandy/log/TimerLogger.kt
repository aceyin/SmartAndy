package aceyin.smandy.log

import java.util.concurrent.atomic.AtomicLong
import java.util.logging.Logger

/**
 * Created by ace on 2017/8/29.
 */
object TimerLogger {

    private val log = Logger.getLogger("SmartAndy Logger")
    private val timeHolder = AtomicLong(0)

    /**
     * 每隔 timeGap 秒输出一条日志
     */
    fun log(message: String, timeGap: Long = 1000) {
        val now = System.currentTimeMillis()
        if (now - timeHolder.get() > timeGap) {
            log.info(message)
            timeHolder.set(now)
        }
    }
}
