package saker.msvc.impl.ccompile.option;

public interface IncludeDirectoryOption {
	public void accept(IncludeDirectoryVisitor visitor);

	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();
}
