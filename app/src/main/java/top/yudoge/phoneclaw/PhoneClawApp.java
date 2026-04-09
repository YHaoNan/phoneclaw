package top.yudoge.phoneclaw;

import android.app.Application;
import android.content.Intent;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.android.LogcatAppender;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import top.yudoge.phoneclaw.service.KeepaliveService;

public class PhoneClawApp extends Application {
    private static final String PREFS_NAME = "phoneclaw";
    private static final String PREFS_KEY_KEEPALIVE = "keepalive_enabled";
    private static final String KEY_SKILLS_INITIALIZED = "skills_initialized";

    @Override
    public void onCreate() {
        super.onCreate();
        configureLogging();
        initializeSkills();
        checkKeepaliveService();
    }

    private void checkKeepaliveService() {
        boolean isKeepaliveEnabled = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(PREFS_KEY_KEEPALIVE, false);
        
        if (isKeepaliveEnabled) {
            Intent intent = new Intent(this, KeepaliveService.class);
            startForegroundService(intent);
        }
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

    private void initializeSkills() {
        boolean initialized = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getBoolean(KEY_SKILLS_INITIALIZED, false);

        if (initialized) {
            return;
        }

        try {
            copySkillsFromAssets();
            getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putBoolean(KEY_SKILLS_INITIALIZED, true)
                    .apply();
            android.util.Log.d("PhoneClawApp", "Skills initialized successfully");
        } catch (IOException e) {
            android.util.Log.e("PhoneClawApp", "Failed to initialize skills", e);
        }
    }

    private void copySkillsFromAssets() throws IOException {
        String[] skillDirs = getAssets().list("skills");
        if (skillDirs == null || skillDirs.length == 0) {
            return;
        }

        File skillsDir = new File(getFilesDir(), "skills");
        if (!skillsDir.exists()) {
            skillsDir.mkdirs();
        }

        for (String skillDir : skillDirs) {
            copySkillDirectory("skills/" + skillDir, new File(skillsDir, skillDir));
        }
    }

    private void copySkillDirectory(String assetPath, File destDir) throws IOException {
        if (!destDir.exists()) {
            destDir.mkdirs();
        }

        String[] files = getAssets().list(assetPath);
        if (files == null) {
            return;
        }

        for (String file : files) {
            String srcPath = assetPath + "/" + file;
            File destFile = new File(destDir, file);

            String[] subFiles = getAssets().list(srcPath);
            if (subFiles != null && subFiles.length > 0) {
                copySkillDirectory(srcPath, destFile);
            } else {
                copyFile(srcPath, destFile);
            }
        }
    }

    private void copyFile(String assetPath, File destFile) throws IOException {
        try (InputStream is = getAssets().open(assetPath);
             OutputStream os = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }
}
