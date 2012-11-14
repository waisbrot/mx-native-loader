/*
 * $Id: NativeLoader.java 155283 2008-05-04 21:22:22Z maxb $
 *
 * Copyright 2006 MX Telecom Ltd.
 */

package com.wapmx.nativeutils.jniloader;

import java.io.File;
import java.io.IOException;

/**
 * Provides a means of loading JNI libraries which are stored within a jar.
 * <p>
 * The library is first extracted to a temporary file, and then loaded with <code>System.load()</code>
 * <p>
 * The extractor can be replaced, but the default implementation expects to find the library in META-INF/lib/, with its
 * os-dependent name. It extracts the library to a temporary directory, whose name is given by the System property
 * "java.library.tmpdir", defaulting to "tmplib".
 * <p>
 * Debugging can be enabled for the jni extractor by setting the System property "java.library.debug" to 1.
 * <p>
 * Libraries can be registered for auto-extraction by listing the base filename in a META-INF/lib/AUTOEXTRACT.LIST
 * classpath resource. This is useful for shipping libraries which are used by code which is not itself aware of the
 * NativeLoader system. To allow this to work, the application must call {@link #extractRegistered()} at some suitably
 * early point in its initialization.
 * 
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public class NativeLoader
{
    private static JniExtractor jniExtractor = new DefaultJniExtractor();

    /**
     * Extract the given library from a jar, and load it.
     * <p>
     * The default jni extractor expects libraries to be in META-INF/lib/, with their platform-dependent name.
     *
     * @param libname   platform-independent library name (as would be passed to System.loadLibrary)
     *
     * @throws IOException if there is a problem extracting the jni library
     * @throws SecurityException  if a security manager exists and its
     *             <code>checkLink</code> method doesn't allow
     *             loading of the specified dynamic library
     *
     */
    public static void loadLibrary(String libname) throws IOException {
        File lib = jniExtractor.extractJni(libname);
        System.load(lib.getAbsolutePath());
    }

    /**
     * Extract all libraries registered for auto-extraction by way of META-INF/lib/AUTOEXTRACT.LIST resources. The
     * application must call {@link #extractRegistered()} at some suitably early point in its initialization if it is
     * using libraries packaged in this way.
     * 
     * @throws IOException if there is a problem extracting the libraries
     */
    public static void extractRegistered() throws IOException {
        jniExtractor.extractRegistered();
    }

    /**
     * @return the jniExtractor
     */
    public static JniExtractor getJniExtractor() {
        return jniExtractor;
    }

    /**
     * @param jniExtractor the jniExtractor to set
     */
    public static void setJniExtractor(JniExtractor jniExtractor) {
        NativeLoader.jniExtractor = jniExtractor;
    }
}
