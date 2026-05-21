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
        var traceBuilder = new StringBuilder();
        var stackTraceElements = e.getStackTrace();
        for (var stackTraceElement : stackTraceElements) {
            var trace = String.format("\n    at %s.%s (%s:%s)", stackTraceElement.getClassName(),
                    stackTraceElement.getMethodName(),
                    stackTraceElement.getFileName(), stackTraceElement.getLineNumber());
            traceBuilder.append(trace);
        }
        var message = String.format("%s: %s%s", e.getClass().getName(), e.getMessage(), traceBuilder);
        error(message);
    }

    private static String getLogContent(String message) {
        String callerInfo = "";
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        for (StackTraceElement element : stackTrace) {
            if (!element.getClassName().equals(LogUtils.class.getName())
                    && !element.getClassName().equals(Thread.class.getName())) {
                callerInfo = String.format("%s:%d", element.getMethodName(), element.getLineNumber());
                break;
            }
        }
        return String.format("[ip - %s] [path - %s] [req - %s] [user - %s] [at - %s] - %s",
                ThreadContext.get("ip"), ThreadContext.get("uri"),
                ThreadContext.get("traceId"), ThreadContext.get("userName"), callerInfo, message);
    }
}
