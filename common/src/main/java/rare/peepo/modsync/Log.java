package rare.peepo.modsync;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Log {
    static final String id = ModSync.ID;
    static final Logger logger = LoggerFactory.getLogger(id);
    
    public static void info(Object o) {
            logger.info("[{}] {}", id, o);
    }
    
    public static void info(String format, Object... arguments) {
        if (!logger.isInfoEnabled())
            return;
        logger.info(
            String.format("[%s] %s", id, format), arguments
        );
    }
    
    public static void warn(Object o) {
        logger.warn("[{}] {}", id, o);
    }
    
    public static void warn(String format, Object... arguments) {
        if (!logger.isWarnEnabled())
            return;
        logger.warn(
            String.format("[%s] %s", id, format), arguments
        );
    }
    
    public static void error(Object o ) {
        logger.error("[{}] {}", id, o);
    }
    
    public static void error(String format, Object... arguments) {
        if (!logger.isErrorEnabled())
            return;
        logger.error(
            String.format("[%s] %s", id, format), arguments
        );
    }
}
