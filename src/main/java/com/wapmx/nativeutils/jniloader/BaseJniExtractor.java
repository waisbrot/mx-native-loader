// $Id: BaseJniExtractor.java 451529 2010-12-23 15:30:18Z markjh $
// Copyright 2006 MX Telecom Ltd

package com.wapmx.nativeutils.jniloader;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;

import com.wapmx.nativeutils.MxSysInfo;
import com.wapmx.nativeutils.filters.IOFileFilterWrapper;
import com.wapmx.nativeutils.filters.PathFilter;

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
        String mxSysInfo = MxSysInfo.getMxSysInfo();
        
        if (mxSysInfo != null) {
            String[] alternatives = MxSysInfo.alternativeMxSysInfos(mxSysInfo);
            
            nativeResourcePaths = new String[alternatives.length + 2];
            nativeResourcePaths[0] = "META-INF/lib/" + mxSysInfo + "/";
            for (int i = 0; i < alternatives.length; i++) {
                nativeResourcePaths[i + 1] = "META-INF/lib/" + alternatives[i] + "/";
            }
            nativeResourcePaths[nativeResourcePaths.length - 1] = "META-INF/lib/";
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

    /**
     * Modified from http://www.uofr.net/~greg/java/get-resource-listing.html
     * 
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     * 
     * @author Greg Briggs
     * @param sameTree Any java class that lives in the same place as the resources you want.
     * @param path Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException 
     * @throws IOException 
     */
    private static Collection<URL> getResourceListing(Class<?> sameTree, final PathFilter filter) throws IOException {
    	String sameTreePath = sameTree.getName().replace('.', '/') + ".class";
        URL dirURL = sameTree.getClassLoader().getResource(sameTreePath);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
        	try {
        		ArrayList<URL> result = new ArrayList<URL>();
				File basePath = new File(dirURL.toURI()).getParentFile();
				int index = 0;
				while ((index = sameTreePath.indexOf('/', index) + 1) > 0) {
					basePath = basePath.getParentFile();
				}
				Iterator<File> iter = FileUtils.iterateFiles(basePath, 
													new IOFileFilterWrapper(filter, basePath), 
													TrueFileFilter.INSTANCE);
				while (iter.hasNext()) {
					result.add(iter.next().toURI().toURL());
				}
				return result;
			} catch (URISyntaxException e) {
				throw new IOException("Unable to convert the directory URL into a File", e);
			} catch (MalformedURLException e) {
				throw new IOException("Unable to convert the path File into a URL", e);
			}
        } 

        if (dirURL == null) {
          /* 
           * In case of a jar file, we can't actually find a directory.
           * Have to assume the same jar as clazz.
           */
          String me = sameTree.getName().replace(".", "/")+".class";
          dirURL = sameTree.getClassLoader().getResource(me);
        }
        
        if (dirURL.getProtocol().equals("jar")) {
          /* A JAR path */
          String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
          JarFile jar = null;
          ArrayList<URL> result = null;
          try {
        	  jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
        	  Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
        	  result = new ArrayList<URL>(); //avoid duplicates in case it is a subdirectory
        	  while(entries.hasMoreElements()) {
        		  URL name = new URL(entries.nextElement().getName());
        		  if (filter.apply(name.getFile())) { //filter according to the path
        			  result.add(name);
        		  }
        	  }
          } finally {
        	  if (jar != null)
        		  jar.close();
          }
          checkNotNull(result);
          return result;
        }      
        throw new UnsupportedOperationException("Cannot list files for URL "+dirURL);
    }
    
    /** {@inheritDoc} */
    public Collection<File> extractJni(Class<?> sameTree, PathFilter filter) throws IOException {
    	Collection<URL> paths = getResourceListing(sameTree, filter);
    	if (paths.isEmpty()) {
    		throw new IOException("No library found for filter "+filter);
    	} else {
    		return extractResource(getJniDir(), paths);
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
    ArrayList<File> extractResource(File dir, Collection<URL> resources) throws IOException {
    	ArrayList<File> extracted = new ArrayList<File>();
    	for (URL resource : resources) {
    		InputStream in = resource.openStream();
    		String outputname = FilenameUtils.getName(resource.getFile());
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
    		extracted.add(outfile);
    	}
        return extracted;
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
