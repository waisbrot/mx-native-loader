package com.wapmx.nativeutils.filters;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;

/**
 * Wrap a PathFilter inside an IOFileFilter
 */
public class IOFileFilterWrapper implements IOFileFilter {
	private final String basePath;
	private final PathFilter filter;

	/**
	 * Paths will be passed to the PathFilter relative to the basepath
	 * @throws IOException 
	 */
	public IOFileFilterWrapper(PathFilter filter, File baseFile) throws IOException {
		checkArgument(baseFile.isDirectory());
		String canonicalPath = baseFile.getCanonicalPath();
		checkState(!canonicalPath.endsWith("/"));
		this.basePath = baseFile.getCanonicalPath() + '/';
		this.filter = filter;
	}
	public boolean accept(File file) {
		try {
			String filePath = file.getCanonicalPath();
			checkArgument(FilenameUtils.directoryContains(basePath, filePath),
					"File must be a subdirectory of the base path (%s) but the passed file (%s) is not",
					basePath, filePath);
			filePath = filePath.substring(basePath.length()-1, filePath.length());
			return filter.apply(filePath);
		} catch (IOException e) {
			throw new RuntimeException("Failed to convert file paths and strings while looking at "+file, e);
		}
	}

	public boolean accept(File dir, String name) {
		return accept(new File(dir, name));
	}

}
