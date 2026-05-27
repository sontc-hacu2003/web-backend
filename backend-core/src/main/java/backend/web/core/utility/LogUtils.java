package backend.web.core.utility;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class LogUtils {

    private LogUtils() {
    }

    public static void info(String message) {
        log.info(message);
    }

    public static void error(String message) {
        log.error(message);
    }

    public static void error(Exception e) {
        log.error("{}: {}", e.getClass().getSimpleName(), e.getMessage());
    }

    public static void error(String message, Exception e) {
        log.error(message, e);
    }
}
