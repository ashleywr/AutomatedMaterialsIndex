package org.slf4j;

public interface Logger {
    default void debug(String message) {
    }

    default void debug(String message, Object arg) {
    }

    default void debug(String message, Object arg1, Object arg2) {
    }

    default void debug(String message, Object... args) {
    }

    default void info(String message) {
    }

    default void info(String message, Object arg) {
    }

    default void info(String message, Object arg1, Object arg2) {
    }

    default void info(String message, Object... args) {
    }

    default void warn(String message) {
    }

    default void warn(String message, Object arg) {
    }

    default void warn(String message, Object arg1, Object arg2) {
    }

    default void warn(String message, Throwable throwable) {
    }

    default void warn(String message, Object... args) {
    }

    default void error(String message) {
    }

    default void error(String message, Object arg) {
    }

    default void error(String message, Object arg1, Object arg2) {
    }

    default void error(String message, Throwable throwable) {
    }

    default void error(String message, Object... args) {
    }
}
