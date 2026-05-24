package backend.web.core.utility;

import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.ThreadContext;

@Slf4j
public class LogUtils {
    public static void info(String message) {
        log.info("{}", getLogContent(message));
    }

    public static void error(String message) {
        log.error("{}", getLogContent(message));
    }

    public static void error(Exception e) {
        error(String.format("%s: %s", e.getClass().getName(), e.getMessage()));
    }

    public static void error(String message, Exception e) {
        log.error("{}", getLogContent(message), e);
    }

    private static String getLogContent(String message) {
        return String.format("[ip - %s] [path - %s] [req - %s] [user - %s] - %s",
                ThreadContext.get("ip"), ThreadContext.get("uri"),
                ThreadContext.get("traceId"), ThreadContext.get("userName"), message);
    }
}
