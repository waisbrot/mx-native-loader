/*
 * $Id: JniExtractor.java 155283 2008-05-04 21:22:22Z maxb $
 *
 * Copyright 2006 MX Telecom Ltd.
 */

package com.wapmx.nativeutils.jniloader;

import java.io.File;
import java.io.IOException;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public interface JniExtractor {
    /**
     * Extract a JNI library from the classpath to a temporary file.
     *
     * @param libname System.loadLibrary() compatible library name
     * @return the extracted file
     * @throws IOException
     */
    public File extractJni(String libname) throws IOException;

    /**
     * Extract all libraries which are registered for auto-extraction
     * to files in the temporary directory.
     * 
     * @throws IOException
     */
    public void extractRegistered() throws IOException;
}
