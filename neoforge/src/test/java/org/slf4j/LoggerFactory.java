package org.slf4j;

public final class LoggerFactory {
    private static final Logger LOGGER = new Logger() {
    };

    private LoggerFactory() {
    }

    public static Logger getLogger(String name) {
        return LOGGER;
    }
}
