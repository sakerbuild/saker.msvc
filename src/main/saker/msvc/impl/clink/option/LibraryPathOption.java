package saker.msvc.impl.clink.option;

public interface LibraryPathOption {
	public void accept(LibraryPathVisitor visitor);

	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();
}
