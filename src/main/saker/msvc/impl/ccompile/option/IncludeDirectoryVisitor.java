package saker.msvc.impl.ccompile.option;

import saker.sdk.support.api.SDKPathReference;

public interface IncludeDirectoryVisitor {
	public default void visit(FileIncludeDirectory includedir) {
		throw new UnsupportedOperationException("Unsupported include directory: " + includedir);
	}

	public default void visit(SDKPathReference includedir) {
		throw new UnsupportedOperationException("Unsupported include directory: " + includedir);
	}
}
