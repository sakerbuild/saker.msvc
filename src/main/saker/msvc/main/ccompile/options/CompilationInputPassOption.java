package saker.msvc.main.ccompile.options;

public interface CompilationInputPassOption {
	public void accept(CompilationInputPassOptionVisitor visitor);
}
