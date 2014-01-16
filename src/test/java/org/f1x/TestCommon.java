package org.f1x;

import org.gflogger.config.xml.XmlLogFactoryConfigurator;

public class TestCommon {
    static {
        try {
            XmlLogFactoryConfigurator.configure("/config/gflogger.xml");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }}
