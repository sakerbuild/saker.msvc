package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.Set;

import saker.build.task.TaskContext;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;

public final class SimpleIncludeDirectoryTaskOption implements IncludeDirectoryTaskOption {
	private final Set<IncludeDirectoryOption> directoryoptions;

	public SimpleIncludeDirectoryTaskOption(Set<IncludeDirectoryOption> directoryoptions) {
		this.directoryoptions = directoryoptions;
	}

	@Override
	public Collection<IncludeDirectoryOption> toIncludeDirectories(TaskContext tc) {
		return directoryoptions;
	}

	@Override
	public IncludeDirectoryTaskOption clone() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((directoryoptions == null) ? 0 : directoryoptions.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		SimpleIncludeDirectoryTaskOption other = (SimpleIncludeDirectoryTaskOption) obj;
		if (directoryoptions == null) {
			if (other.directoryoptions != null)
				return false;
		} else if (!directoryoptions.equals(other.directoryoptions))
			return false;
		return true;
	}
}