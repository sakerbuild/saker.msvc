package saker.msvc.main.clink.options;

import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;

public interface MSVCLinkerOptionsVisitor {
	public default void visit(MSVCLinkerOptions options) {
		throw new UnsupportedOperationException("Unsupported linker options: " + options);
	}

	public default void visit(COptionsPresetTaskOutput options) {
		throw new UnsupportedOperationException("Unsupported linker options: " + options);
	}
}
