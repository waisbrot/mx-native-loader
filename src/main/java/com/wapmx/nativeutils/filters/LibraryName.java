package com.wapmx.nativeutils.filters;

import java.util.HashSet;

import org.apache.commons.io.FilenameUtils;

import com.google.common.base.Joiner;
import com.mkyong.core.OSValidator;

public class LibraryName implements PathFilter {
	private final HashSet<String> names = new HashSet<String>();
	public LibraryName(String name) {
		names.add(name);
		String sysName = System.mapLibraryName(name);
		names.add(sysName);
	
		// If you ask for "libfoo" the system might suggest "liblibfoo.so"
		boolean liblib = false;
		if (sysName.startsWith("liblib")) {
			names.add(sysName.substring(3));
			liblib = true;
		}
		
		// It's not clear whether Mac OS libs should be .dylib or .jnilib, so just try both
		if (sysName.endsWith(".dylib")) {
			String jnilib = sysName.substring(0, sysName.length() - 6) + ".jnilib";
			names.add(jnilib);
			if (liblib) {
				names.add(jnilib.substring(3));
			}
		} else if (sysName.endsWith(".jnilib") && OSValidator.isMac()) {
			String dylib = sysName.substring(0, sysName.length() - 7) + ".dylib";
			names.add(dylib);
			if (liblib) {
				names.add(dylib.substring(3));
			}			
		}
	}
	public boolean apply(String input) {
		return names.contains(FilenameUtils.getName(input));
	}
	
	@Override
	public String toString() {
		return "LibraryName({"+Joiner.on(',').join(names)+"})";
	}
}
