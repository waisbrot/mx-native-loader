package com.wapmx.nativeutils;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MxSysInfo {
    /**
     * Find the mx.sysinfo string for the current jvm
     * <p>
     * Can be overridden by specifying a mx.sysinfo system property
     */
    public static String getMxSysInfo() {
        String mxSysInfo = System.getProperty("mx.sysinfo");
        if (mxSysInfo != null) {
            return mxSysInfo;
        }
        else {
            return guessMxSysInfo();
        }
    }
    
    /**
     * Alternative mx.sysinfos for a platform
     * Libraries compiled for one OS can often work on another. For example libraries compiled for etch will probably 
     * also work on lenny. 
     * @param mxSysInfo
     * @return
     */
    public static String[] alternativeMxSysInfos(String mxSysInfo) {
        if (mxSysInfo.endsWith("Linux-c27cxx610")) { // lenny
            return new String[] { 
                    mxSysInfo.replace("Linux-c27cxx610", "Linux-c23cxx6"), // etch
            };
        } 
        else {
            return new String[] {};
        }
    }
    
    /**
     * Make a spirited attempt at guessing what the mx.sysinfo for the current jvm might be.
     */
    public static String guessMxSysInfo() {
        String arch = System.getProperty("os.arch");
        String os = System.getProperty("os.name");
        String extra = "unknown";

        if ("Linux".equals(os)) {
            try {
                String libc_dest = new File("/lib/libc.so.6").getCanonicalPath();
                Matcher libc_m = Pattern.compile(".*/libc-(\\d+)\\.(\\d+)\\..*").matcher(libc_dest);
                if (!libc_m.matches()) throw new IOException("libc symlink contains unexpected destination: "
                                                             + libc_dest);

                File libstdcxx_file = new File("/usr/lib/libstdc++.so.6");
                if (!libstdcxx_file.exists()) libstdcxx_file = new File("/usr/lib/libstdc++.so.5");

                String libstdcxx_dest = libstdcxx_file.getCanonicalPath();
                Matcher libstdcxx_m =
                        Pattern.compile(".*/libstdc\\+\\+\\.so\\.(\\d+)\\.0\\.(\\d+)").matcher(libstdcxx_dest);
                if (!libstdcxx_m.matches()) throw new IOException("libstdc++ symlink contains unexpected destination: "
                                                                  + libstdcxx_dest);
                String cxxver;
                if ("5".equals(libstdcxx_m.group(1))) {
                    cxxver = "5";
                }
                else if ("6".equals(libstdcxx_m.group(1))) {
                    int minor_ver = Integer.parseInt(libstdcxx_m.group(2));
                    if (minor_ver < 9) {
                        cxxver = "6";
                    }
                    else {
                        cxxver = "6" + libstdcxx_m.group(2);
                    }
                }
                else {
                    cxxver = libstdcxx_m.group(1) + libstdcxx_m.group(2);
                }

                extra = "c" + libc_m.group(1) + libc_m.group(2) + "cxx" + cxxver;
            }
            catch (IOException e) {
                extra = "unknown";
            }
            finally {

            }
        }

        return arch + "-" + os + "-" + extra;
    }
    
    /**
     * From http://www.uofr.net/~greg/java/get-resource-listing.html
     * 
     * List directory contents for a resource folder. Not recursive.
     * This is basically a brute-force implementation.
     * Works for regular files and also JARs.
     * 
     * @author Greg Briggs
     * @param clazz Any java class that lives in the same place as the resources you want.
     * @param path Should end with "/", but not start with one.
     * @return Just the name of each member item, not the full paths.
     * @throws URISyntaxException 
     * @throws IOException 
     */
    private static Collection<String> getResourceListing(Class<?> clazz, String path) throws URISyntaxException, IOException {
        URL dirURL = clazz.getClassLoader().getResource(path);
        if (dirURL != null && dirURL.getProtocol().equals("file")) {
          /* A file path: easy enough */
          return Arrays.asList(new File(dirURL.toURI()).list());
        } 

        if (dirURL == null) {
          /* 
           * In case of a jar file, we can't actually find a directory.
           * Have to assume the same jar as clazz.
           */
          String me = clazz.getName().replace(".", "/")+".class";
          dirURL = clazz.getClassLoader().getResource(me);
        }
        
        if (dirURL.getProtocol().equals("jar")) {
          /* A JAR path */
          String jarPath = dirURL.getPath().substring(5, dirURL.getPath().indexOf("!")); //strip out only the JAR file
          JarFile jar = null;
          Set<String> result = null;
          try {
        	  jar = new JarFile(URLDecoder.decode(jarPath, "UTF-8"));
        	  Enumeration<JarEntry> entries = jar.entries(); //gives ALL entries in jar
        	  result = new HashSet<String>(); //avoid duplicates in case it is a subdirectory
        	  while(entries.hasMoreElements()) {
        		  String name = entries.nextElement().getName();
        		  if (name.startsWith(path)) { //filter according to the path
        			  String entry = name.substring(path.length());
        			  int checkSubdir = entry.indexOf("/");
        			  if (checkSubdir >= 0) {
        				  // if it is a subdirectory, we just return the directory name
        				  entry = entry.substring(0, checkSubdir);
        			  }
        			  result.add(entry);
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
}
