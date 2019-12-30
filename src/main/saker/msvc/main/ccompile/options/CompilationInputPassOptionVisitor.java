package saker.msvc.main.ccompile.options;

public interface CompilationInputPassOptionVisitor {
	public default void visit(FileCompilationInputPass input) {
		throw new UnsupportedOperationException("Unsupported input: " + input);
	}

	public default void visit(OptionCompilationInputPass input) {
		throw new UnsupportedOperationException("Unsupported input: " + input);
	}
}
