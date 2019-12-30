package saker.msvc.main.ccompile.options;

import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;

public interface MSVCCompilerOptionsVisitor {
	public default void visit(MSVCCompilerOptions options) {
		throw new UnsupportedOperationException("Unsupported compilar options: " + options);
	}

	public default void visit(COptionsPresetTaskOutput options) {
		throw new UnsupportedOperationException("Unsupported compiler options: " + options);
	}
}
