package eu.weischer.root.application;

import java.io.File;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import eu.weischer.root.R;

public class Logger{
    public enum LoggingLevel {
        all ("A"),
        verbose ("V"),
        information ("I"),
        warning ("W"),
        error ("E"),
        none ("");
        private final String abbreviation;
        LoggingLevel(String abbreviation) {
            this.abbreviation = abbreviation;
        }
        public boolean includes (LoggingLevel  otherLevel) {
            return (ordinal() <= otherLevel.ordinal());
        }
        public String getAbbreviation() {
            return abbreviation;
        }
        public static LoggingLevel getByName(String name) {
            LoggingLevel result = none;
            for (LoggingLevel loggingLevel : LoggingLevel.values())
                if (name.equalsIgnoreCase(loggingLevel.name()))
                    result = loggingLevel;
            return result;
        }
    }
    private static final String loggerTag = "Logger";
    private static final String logFolder = "logging";
    public static final class LogEntry {
        private static final String dateFormat = "dd.MMM.yyyy HH:mm:ss.SSS";
        private long time = 0;
        private Exception exception = null;
        private String tag;
        private LoggingLevel level;
        private String text;
        private long threadId;
        private String threadName;
        public LogEntry(LoggingLevel level, String tag, String text) {
            this.level = level;
            this.tag = tag;
            this.text = text;
            Thread thread = Thread.currentThread();
            threadId = thread.getId();
            threadName = thread.getName();
        }
        public LogEntry(LoggingLevel level, String tag, String text, Exception exception) {
            this.level = level;
            this.tag = tag;
            this.text = text;
            this.exception = exception;
            Thread thread = Thread.currentThread();
            threadId = thread.getId();
            threadName = thread.getName();
        }
        public void setTime(long time) {
            this.time = time;
        }
        public String toString() {
            String result = (new SimpleDateFormat(dateFormat)).format(new Date(time)) + " "
                    + threadName + " " + threadId + " "
                    + level.getAbbreviation() + " "
                    + tag + " " + text;
            if (exception != null)
                result += " " + (exception.getMessage()==null?exception.toString():exception.getMessage());
            result += "\n";
            return result;
        }
        public long getTime() {
            return time;
        }
        public Exception getException() {
            return exception;
        }
        public String getTag() {
            return tag;
        }
        public LoggingLevel getLevel() {
            return level;
        }
    }
    public static final class LogAdapter {
        private String tag;
        private LoggingLevel logLevel;
        private Logger logger;
        private boolean initialized = false;

        private LogAdapter(String tag, Logger logger) {
            this.tag = tag;
            this.logger = logger;
            logLevel = logger.logLevel;
        }
        public Logger getLogger() {
            return logger;
        }
        //region log methods
        public void a(String text) {
            if (logLevel.includes(LoggingLevel.all))
                log (new LogEntry(LoggingLevel.all, tag, text));
        }
        public void a(String tag, String text) {
            if (logLevel.includes(LoggingLevel.all))
                log (new LogEntry(LoggingLevel.all, tag, text));
        }
        public void a(Exception exception) {
            if (logLevel.includes(LoggingLevel.all))
                log (new LogEntry(LoggingLevel.all, this.tag, "", exception));
        }
        public void a(Exception exception, String text) {
            if (logLevel.includes(LoggingLevel.all))
                log (new LogEntry(LoggingLevel.all, tag, text, exception));
        }
        public void a(Exception exception, String tag, String text) {
            if (logLevel.includes(LoggingLevel.all))
                log (new LogEntry(LoggingLevel.all, tag, text, exception));
        }

        public void v(String text) {
            if (logLevel.includes(LoggingLevel.verbose))
                log (new LogEntry(LoggingLevel.verbose, this.tag, text));
        }
        public void v(String tag, String text) {
            if (logLevel.includes(LoggingLevel.verbose))
                log (new LogEntry(LoggingLevel.verbose, tag, text));
        }
        public void v(Exception exception) {
            if (logLevel.includes(LoggingLevel.verbose))
                log (new LogEntry(LoggingLevel.verbose, this.tag, "", exception));
        }
        public void v(Exception exception, String text) {
            if (logLevel.includes(LoggingLevel.verbose))
                log (new LogEntry(LoggingLevel.verbose, tag, text, exception));
        }
        public void v(Exception exception, String tag, String text) {
            if (logLevel.includes(LoggingLevel.verbose))
                log (new LogEntry(LoggingLevel.verbose, tag, text, exception));
        }

        public void i(String text) {
            if (logLevel.includes(LoggingLevel.information))
                log (new LogEntry(LoggingLevel.information, this.tag, text));
        }
        public void i(String tag, String text) {
            if (logLevel.includes(LoggingLevel.information))
                log (new LogEntry(LoggingLevel.information, tag, text));
        }
        public void i(Exception exception) {
            if (logLevel.includes(LoggingLevel.information))
                log (new LogEntry(LoggingLevel.information, this.tag, "", exception));
        }
        public void i(Exception exception, String text) {
            if (logLevel.includes(LoggingLevel.information))
                log (new LogEntry(LoggingLevel.information, tag, text, exception));
        }
        public void i(Exception exception, String tag, String text) {
            if (logLevel.includes(LoggingLevel.information))
                log (new LogEntry(LoggingLevel.information, tag, text, exception));
        }

        public void w(String text) {
            if (logLevel.includes(LoggingLevel.warning))
                log (new LogEntry(LoggingLevel.warning, this.tag, text));
        }
        public void w(String tag, String text) {
            if (logLevel.includes(LoggingLevel.warning))
                log (new LogEntry(LoggingLevel.warning, tag, text));
        }
        public void w(Exception exception) {
            if (logLevel.includes(LoggingLevel.warning))
                log (new LogEntry(LoggingLevel.warning, this.tag, "", exception));
        }
        public void w(Exception exception, String text) {
            if (logLevel.includes(LoggingLevel.warning))
                log (new LogEntry(LoggingLevel.warning, tag, text, exception));
        }
        public void w(Exception exception, String tag, String text) {
            if (logLevel.includes(LoggingLevel.warning))
                log (new LogEntry(LoggingLevel.warning, tag, text, exception));
        }

        public void e(String text) {
            if (logLevel.includes(LoggingLevel.error))
                log (new LogEntry(LoggingLevel.error, this.tag, text));
        }
        public void e(String tag, String text) {
            if (logLevel.includes(LoggingLevel.error))
                log (new LogEntry(LoggingLevel.error, tag, text));
        }
        public void e(Exception exception) {
            if (logLevel.includes(LoggingLevel.error))
                log (new LogEntry(LoggingLevel.error, this.tag, "", exception));
        }
        public void e(Exception exception, String text) {
            if (logLevel.includes(LoggingLevel.error))
                log (new LogEntry(LoggingLevel.error, tag, text, exception));
        }
        public void e(Exception exception, String tag, String text) {
            if (logLevel.includes(LoggingLevel.error))
                log (new LogEntry(LoggingLevel.error, tag, text, exception));
        }
        //endregion

        private void log(LogEntry logEntry) {
            if (! initialized)
                if (logger.initialized) {
                    initialized = true;
                    logLevel = logger.logLevel;
                }
            if (logLevel.includes(logEntry.getLevel()))
                logger.add(logEntry);
        }
    }
    private static Logger theLogger = null;
    private static LogAdapter log = null;
    public static synchronized LogAdapter getLogAdapter(String tag) {
        if (theLogger == null) {
            theLogger = new Logger();
            log = new LogAdapter(loggerTag, theLogger);
            log.i("Logger created");
        }
        log.v("new Logadapter (tag=" + tag +")");
        return new LogAdapter(tag, theLogger)    ;
    }

    private LinkedList<LogEntry> entries = new LinkedList<>();
    private LoggingLevel logLevel = LoggingLevel.all;
    private String logFile = null;
    private FileWriter writer = null;
    private boolean initialized = false;

    public synchronized void add(LogEntry logEntry) {
        if (logLevel.includes(logEntry.getLevel())) {
            logEntry.setTime(System.currentTimeMillis());
            if (initialized)
                log(logEntry);
            else
                entries.addLast(logEntry);
        }
    }
    public synchronized void initialize() {
        if (! initialized) {
            try {
                log.i("Logger initialization");
                initialized = true;
                logLevel = Logger.LoggingLevel.getByName(App.getResourceString(R.string.root_logging_level));
                logFile = App.getRootFolder() + File.separator + logFolder + File.separator + App.getApplicationName() + ".log";
                if (!(new File(logFile).getParentFile().exists())) {
                    File logFolder = new File(logFile).getParentFile();
                    logFolder.mkdirs();
                    log.v("Logging folder " + logFolder.getName() + " created.");
                }
                if (logLevel != LoggingLevel.none)
                    for (LogEntry logEntry : entries)
                        log(logEntry);
                entries.clear();
            } catch (Exception ex) {
                log.e(ex, "Error during initialization");
                initialized = false;
            }
        }
    }

    private void log(LogEntry logEntry) {
        try {
            if (writer == null)
                writer = new FileWriter(logFile, true);
            if (logLevel.includes(logEntry.getLevel())) {
                writer.write(logEntry.toString());
                writer.flush();
            }
        } catch (Exception ex) {
            try{
                writer.close();
            } catch (Exception e) {}
            writer = null;
        }
    }
}
