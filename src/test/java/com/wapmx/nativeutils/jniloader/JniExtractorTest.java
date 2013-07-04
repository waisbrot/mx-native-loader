/*
 * $Id: JniExtractorTest.java 287492 2009-05-27 15:12:30Z markjh $
 *
 * Copyright 2006 MX Telecom Ltd.
 */

package com.wapmx.nativeutils.jniloader;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import junit.framework.TestCase;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public class JniExtractorTest extends TestCase {
    public void testExtract() throws IOException {
        File output = new File("target/extractortest/foo/outputtest");
        output.delete();
        new File("target/extractortest/foo").delete();
        new File("target/extractortest").delete();
        assertFalse(output.exists());
        
        System.setProperty("java.library.tmpdir", "target/extractortest/foo");
        System.setProperty("java.library.debug", "1");
        
        BaseJniExtractor jniExtractor = new WebappJniExtractor("ExtractorTest");
        URL test = getClass().getClassLoader().getResource("com/wapmx/nativeutils/jniloader/JniExtractorTest.class");
        jniExtractor.extractResource(jniExtractor.getNativeDir(), test, "outputtest");
        assertTrue(output.exists());
    }
}
