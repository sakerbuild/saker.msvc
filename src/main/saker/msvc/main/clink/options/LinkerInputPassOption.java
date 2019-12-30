package saker.msvc.main.clink.options;

public interface LinkerInputPassOption {
	public void accept(LinkerInputPassOptionVisitor visitor);
}
