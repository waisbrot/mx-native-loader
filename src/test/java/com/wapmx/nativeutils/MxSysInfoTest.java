// $Id$
// Copyright 2009 MX Telecom Ltd
package com.wapmx.nativeutils;

import com.wapmx.nativeutils.MxSysInfo;

import junit.framework.TestCase;

public class MxSysInfoTest extends TestCase {

    /**
     * Make sure that the mx.sysinfo guessed is the same as the one supplied to maven in /etc/mavenrc
     */
    public void testGuess() {
        assertEquals(MxSysInfo.guessMxSysInfo(), System.getProperty("mx.sysinfo"));
    }
    
    /**
     * Make sure the mx.sysinfo can be overridden by the System property 
     */
    public void testOverride() {
        String oldMxSysInfo = System.getProperty("mx.sysinfo");
        String mxSysInfo = "I am probably not a sysinfo string";
        System.setProperty("mx.sysinfo", mxSysInfo);
        assertEquals(mxSysInfo, MxSysInfo.getMxSysInfo());
        System.setProperty("mx.sysinfo", oldMxSysInfo);
    }
}
