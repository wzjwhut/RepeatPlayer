package org.apache.logging.log4j;

/**
 * Created by zijiang.wu on 2018/2/2.
 */

public class LogManager {
    public static Logger getLogger(String name) {
        return new Logger(name);
    }
    public static Logger getLogger(Class<?> aClass) {
        return new Logger(aClass.getSimpleName());
    }
}
