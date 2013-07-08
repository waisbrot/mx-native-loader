package com.wapmx.nativeutils.filters;

import static com.google.common.base.Preconditions.checkArgument;

public class RootPath implements PathFilter {
	private String root;
	public RootPath() {
		this("/META-INF/");
	}
	public RootPath(String path) {
		checkArgument(path.startsWith("/"), 
				"RootPath must be given a path that begins with '/', but the provided path was %s", 
				path);
		this.root = path;
	}
	public boolean apply(String input) {
		return input.startsWith(root);
	}
	
	@Override
	public String toString() {
		return "RootPath("+root+')';
	}
}