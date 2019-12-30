package saker.msvc.impl.clink.option;

import saker.sdk.support.api.SDKPathReference;

public interface LibraryPathVisitor {
	public default void visit(FileLibraryPath libpath) {
		throw new UnsupportedOperationException("Unsupported library path: " + libpath);
	}

	public default void visit(SDKPathReference libpath) {
		throw new UnsupportedOperationException("Unsupported library path: " + libpath);
	}

}
