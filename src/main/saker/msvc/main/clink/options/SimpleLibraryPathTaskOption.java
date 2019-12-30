package saker.msvc.main.clink.options;

import java.util.Collection;

import saker.build.task.TaskContext;
import saker.msvc.impl.clink.option.LibraryPathOption;

public class SimpleLibraryPathTaskOption implements LibraryPathTaskOption {

	private Collection<LibraryPathOption> libraryPath;

	public SimpleLibraryPathTaskOption(Collection<LibraryPathOption> libraryPath) {
		this.libraryPath = libraryPath;
	}

	@Override
	public LibraryPathTaskOption clone() {
		return this;
	}

	@Override
	public Collection<LibraryPathOption> toLibraryPath(TaskContext taskcontext) {
		return libraryPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((libraryPath == null) ? 0 : libraryPath.hashCode());
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
		SimpleLibraryPathTaskOption other = (SimpleLibraryPathTaskOption) obj;
		if (libraryPath == null) {
			if (other.libraryPath != null)
				return false;
		} else if (!libraryPath.equals(other.libraryPath))
			return false;
		return true;
	}

}
