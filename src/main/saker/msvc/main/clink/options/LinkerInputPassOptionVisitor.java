package saker.msvc.main.clink.options;

public interface LinkerInputPassOptionVisitor {
	public default void visit(FileLinkerInputPass input) {
		throw new UnsupportedOperationException("Unsupported input: " + input);
	}

	public default void visit(CompilerOutputLinkerInputPass input) {
		throw new UnsupportedOperationException("Unsupported input: " + input);
	}
}
