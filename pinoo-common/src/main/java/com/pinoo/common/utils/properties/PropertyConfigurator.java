package com.pinoo.common.utils.properties;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertyConfigurator {

    private static final Logger logger = LoggerFactory.getLogger(PropertyConfigurator.class);

    private static Map<String, PropertyConfigurator> systemPropertieMap = new HashMap<String, PropertyConfigurator>();

    private Properties properties;

    private String fileName;

    private long lastFreshTime;

    private final boolean autoLoad = false;

    private static boolean closeThread = true;

    public static void openThread() {
        closeThread = false;
    }

    static {
        newThreadLoadProperties();
    }

    /** the seconds between two times of calling init() */
    static final int refreshDistanceInSecond = 60000;

    public String getProperty(String propertyName) {
        if (properties.getProperty(propertyName) != null) {
            return properties.getProperty(propertyName).trim();
        } else {
            return null;
        }
    }

    public boolean getBooleanProperty(String propertyName) {
        try {
            String value = this.getProperty(propertyName);
            return Boolean.valueOf(value);
        } catch (Exception e) {
            logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
            throw new RuntimeException(this.fileName + " [" + propertyName + "]参数转换错误", e);
        }

    }

    public int getIntProperty(String propertyName) {
        int x = 0;
        try {
            String value = this.getProperty(propertyName);
            x = Integer.parseInt(value);
        } catch (Exception e) {
            logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
        }
        return x;
    }

    public long getLongProperty(String propertyName) {
        long x = 0;
        try {
            String value = this.getProperty(propertyName);
            x = Long.parseLong(value);
        } catch (Exception e) {
            logger.error(this.fileName + " [" + propertyName + "]参数转换错误", e);
        }
        return x;
    }

    public Properties getProperties() {
        return properties;
    }

    public static PropertyConfigurator getInstance(String fileName) {
        if (!systemPropertieMap.containsKey(fileName)) {
            systemPropertieMap.put(fileName, init(fileName));
        }
        return systemPropertieMap.get(fileName);
    }

    static synchronized PropertyConfigurator init(String fileName) {

        PropertyConfigurator systemProperties = new PropertyConfigurator(fileName);

        PropertyConfigurator.logger
                .info("init() - systemProperties=" + systemProperties + ", refreshDistanceInSecond=" + PropertyConfigurator.refreshDistanceInSecond / 1000); //$NON-NLS-1$ //$NON-NLS-2$
        return systemProperties;
    }

    private PropertyConfigurator(String fileName) {
        this.fileName = fileName;
        InputStream inputStream = getClassLoader().getResourceAsStream(fileName);
        if (inputStream == null) {
            throw new RuntimeException("can not read  file : " + fileName);
        }

        try {
            Properties properties = new Properties();
            properties.load(inputStream);
            this.properties = properties;
            this.lastFreshTime = System.currentTimeMillis();
            this.fileName = fileName;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static ClassLoader getClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        if (classLoader == null) {
            classLoader = PropertyConfigurator.class.getClassLoader();
        }
        return classLoader;
    }

    private static void newThreadLoadProperties() {
        new Thread(new Runnable() {

            public void run() {
                for (;;) {

                    try {

                        Thread.sleep(refreshDistanceInSecond);
                        if (closeThread) {
                            break;
                        }
                        for (String key : systemPropertieMap.keySet()) {
                            String fileName = key;
                            PropertyConfigurator systemProperties = systemPropertieMap.get(key);
                            logger.debug("{} reloaded with the expiration:{}s", fileName, new Date());
                            final URL url = getClassLoader().getResource(fileName);
                            final Properties p = new Properties();
                            try {
                                File resultFile = new File(url.toURI());
                                InputStream in = new FileInputStream(resultFile);
                                p.load(in);
                                try {
                                    in.close();
                                } catch (Exception e) {
                                }
                                systemProperties.properties = p;
                            } catch (Exception e) {
                                if (url.getPath().contains(".jar")) {
                                    return;
                                } else {
                                    logger.warn(fileName + " load failed", e);
                                }
                                throw new IllegalStateException(e.getMessage());
                            }

                            systemProperties.lastFreshTime = System.currentTimeMillis();
                        }

                    } catch (Exception e) {
                        logger.error("读取配置文件线程错误", e);
                    }
                }
            }
        }).start();
    }

    public static Map<String, String> getPropertiesParams(String param) {
        Map<String, String> paramsMap = new HashMap<String, String>();
        if (StringUtils.isNotBlank(param)) {
            String[] subs = param.split(";");
            for (String sub : subs) {
                if (StringUtils.isNotBlank(sub) && sub.contains(":")) {
                    sub = sub.trim();
                    String[] params = sub.split(":");
                    paramsMap.put(params[0].trim(), params[1].trim());
                }
            }
        }
        return paramsMap;
    }

}
