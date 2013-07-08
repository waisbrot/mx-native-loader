mx-native-loader
================

A Java library to extract and load native libraries from the classpath.

Most of the code originates from Richard van der Hoff: http://docs.codehaus.org/display/MAVENUSER/Projects+With+JNI

My changes have to do with more flexibility in the naming scheme of the native library.  
Rather than a set library name and a specific pattern (architecture, OS, extras), I'd like to have the
native-loader lib take some filters that describe what the correct native library looks like.

For example, this looks for a library whose name is similar to "jhdf5" (eg "libjhdf5.jnilib" or "libjhdf5.dylib"),
and which is located under /META-INF/lib within the same package tree as the loader-class:

  public class Loader {
    public static void load() {
      AndFilter filter = new AndFilter();
      filter.and(new RootPath("/META-INF/lib")).and(new LibraryName("jhdf5"));
      NativeLoader.loadLibrary(Loader.class, filter);
    }
  }
