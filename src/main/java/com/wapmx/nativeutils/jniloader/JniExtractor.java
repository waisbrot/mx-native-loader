// $Id: JniExtractor.java 276979 2009-04-23 21:48:19Z maxb $
// Copyright 2006 MX Telecom Ltd

package com.wapmx.nativeutils.jniloader;

import java.io.File;
import java.io.IOException;
import java.util.Collection;

import com.wapmx.nativeutils.filters.PathFilter;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public interface JniExtractor {
    /**
     * Extract a JNI library from the classpath to a temporary file.
     * @param sameTree 
     * @param filter 
     * 
     * @param libname System.loadLibrary() compatible library name
     * @return the extracted file
     * @throws IOException
     */
    public Collection<File> extractJni(Class<?> sameTree, PathFilter filter) throws IOException;
}
