package com.wapmx.nativeutils.filters;

import java.util.ArrayList;

import com.google.common.base.Joiner;

public class AndFilter implements PathFilter {
	private final ArrayList<PathFilter> predicates = new ArrayList<PathFilter>();
	public AndFilter and(PathFilter pred) {
		predicates.add(pred);
		return this;
	}
	public boolean apply(String input) {
		for (PathFilter pred : predicates) {
			if (!pred.apply(input))
				return false;
		}
		return true;
	}
	
	@Override
	public String toString() {
		return "AND("+Joiner.on(',').join(predicates)+")";
	}
}
