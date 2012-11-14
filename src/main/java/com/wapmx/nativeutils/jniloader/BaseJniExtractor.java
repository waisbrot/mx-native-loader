// $Id: BaseJniExtractor.java 287466 2009-05-27 15:07:32Z markjh $
// Copyright 2006 MX Telecom Ltd

package com.wapmx.nativeutils.jniloader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * @author Richard van der Hoff <richardv@mxtelecom.com>
 */
public abstract class BaseJniExtractor implements JniExtractor {
    private static boolean debug = false;
    static {
        // initialise the debug switch
        String s = System.getProperty("java.library.debug");
        if (s != null && (s.toLowerCase().startsWith("y") || s.startsWith("1")))
            debug = true;
    }

    /**
     * We use a resource path of the form META-INF/lib/${mx.sysinfo}/ This way native builds for multiple architectures
     * can be packaged together without interfering with each other And by setting mx.sysinfo the jvm can pick the
     * native libraries appropriate for itself.
     */
    private final String[] nativeResourcePaths;


    /**
     * @param classloaderName is a friendly name for your classloader which will be embedded in the directory name
     * of the classloader-specific subdirectory which will be created.
     */
    public BaseJniExtractor() throws IOException {
        if (System.getProperty("mx.sysinfo") != null) {
            nativeResourcePaths = new String[] { "META-INF/lib/" + System.getProperty("mx.sysinfo") + "/",
                    "META-INF/lib/" };
        }
        else {
            nativeResourcePaths = new String[] { "META-INF/lib/" };
        }
    }

    /**
     * this is where native dependencies are extracted to (e.g. tmplib/).
     * 
     * @return native working dir
     */
    public abstract File getNativeDir();

    /**
     *  this is where JNI libraries are extracted to (e.g. tmplib/classloaderName.1234567890000.0/).
     * 
     * @return jni working dir
     */
    public abstract File getJniDir();

    /** {@inheritDoc} */
    public File extractJni(String libname) throws IOException {
        String mappedlib = System.mapLibraryName(libname);
        /*
         * On Darwin, the default mapping is to .jnilib; but we use .dylibs so that library interdependencies are
         * handled correctly. if we don't find a .jnilib, try .dylib instead.
         */
        URL lib = null;

        for (int i = 0; i < nativeResourcePaths.length; i++) {
            lib = this.getClass().getClassLoader().getResource(nativeResourcePaths[i] + mappedlib);
            if (lib != null)
                break;
            if (mappedlib.endsWith(".jnilib")) {
                lib = this.getClass().getClassLoader().getResource(
                        nativeResourcePaths[i] + mappedlib.substring(0, mappedlib.length() - 7) + ".dylib");
                if (lib != null) {
                    mappedlib = mappedlib.substring(0, mappedlib.length() - 7) + ".dylib";
                    break;
                }
            }
        }

        if (lib != null) {
            return extractResource(getJniDir(), lib, mappedlib);
        }
        else {
            throw new IOException("Couldn't find jni library " + mappedlib + " on the classpath");
        }
    }

    /** {@inheritDoc} */
    public void extractRegistered() throws IOException {
        if (debug) System.err.println("Extracting libraries registered in classloader " + this.getClass().getClassLoader());
        for (int i = 0; i < nativeResourcePaths.length; i++) {
            Enumeration<URL> resources = this.getClass().getClassLoader().getResources(
                    nativeResourcePaths[i] + "AUTOEXTRACT.LIST");
            while (resources.hasMoreElements()) {
                URL res = resources.nextElement();
                if (debug) System.err.println("Extracting libraries listed in " + res);
                BufferedReader r = new BufferedReader(new InputStreamReader(res.openStream(), "UTF-8"));
                String line;
                while ((line = r.readLine()) != null) {
                    URL lib = null;
                    for (int j = 0; j < nativeResourcePaths.length; j++) {
                        lib = this.getClass().getClassLoader().getResource(nativeResourcePaths[j] + line);
                        if (lib != null)
                            break;
                    }
                    if (lib != null) {
                        extractResource(getNativeDir(), lib, line);
                    }
                    else {
                        throw new IOException("Couldn't find native library " + line + "on the classpath");
                    }
                }
            }
        }
    }

    /**
     * Extract a resource to the tmp dir (this entry point is used for unit testing)
     * 
     * @param dir the directory to extract the resource to
     * @param resource the resource on the classpath
     * @param outputname the filename to copy to (within the tmp dir)
     * @return the extracted file
     * @throws IOException
     */
    File extractResource(File dir, URL resource, String outputname) throws IOException {
        InputStream in = resource.openStream();
        File outfile = new File(dir, outputname);
        // Create a new file rather than writing into old file
        File outfiletemp = File.createTempFile(outputname, null, getJniDir());
        if (debug)
            System.err.println("Extracting '" + resource + "' to '" + outfile.getAbsolutePath() + "'");
        FileOutputStream out = new FileOutputStream(outfiletemp);
        copy(in, out);
        out.close();
        in.close();
        outfiletemp.renameTo(outfile);
        outfile.deleteOnExit();
        return outfile;
    }

    /**
     * copy an InputStream to an OutputStream.
     * 
     * @param in InputStream to copy from
     * @param out OutputStream to copy to
     * @throws IOException if there's an error
     */
    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] tmp = new byte[8192];
        int len = 0;
        while (true) {
            len = in.read(tmp);
            if (len <= 0) {
                break;
            }
            out.write(tmp, 0, len);
        }
    }
}
