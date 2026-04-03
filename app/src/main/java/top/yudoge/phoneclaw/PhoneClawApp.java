package top.yudoge.phoneclaw;

import android.app.Application;

import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;

public class PhoneClawApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        configureLogging();
    }

    private void configureLogging() {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.reset();

        PatternLayoutEncoder encoder = new PatternLayoutEncoder();
        encoder.setContext(loggerContext);
        encoder.setPattern("[%thread] %msg%n");
        encoder.start();

        PatternLayoutEncoder tagEncoder = new PatternLayoutEncoder();
        tagEncoder.setContext(loggerContext);
        tagEncoder.setPattern("%logger{12}");
        tagEncoder.start();

        LogcatAppender logcatAppender = new LogcatAppender();
        logcatAppender.setContext(loggerContext);
        logcatAppender.setEncoder(encoder);
        logcatAppender.setTagEncoder(tagEncoder);
        logcatAppender.start();

        Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
        rootLogger.addAppender(logcatAppender);
        rootLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);

        Logger hanaiLogger = loggerContext.getLogger("top.yudoge.hanai");
        hanaiLogger.setLevel(ch.qos.logback.classic.Level.DEBUG);
    }
}
