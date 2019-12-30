package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.Collections;

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.msvc.impl.ccompile.option.FileIncludeDirectoryOption;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.std.api.file.location.ExecutionFileLocation;

final class RelativePathIncludeDirectoryTaskOption implements IncludeDirectoryTaskOption {
	private final SakerPath path;

	public RelativePathIncludeDirectoryTaskOption(SakerPath path) {
		this.path = path;
	}

	@Override
	public Collection<IncludeDirectoryOption> toIncludeDirectories(TaskContext tc) {
		return Collections.singleton(new FileIncludeDirectoryOption(
				ExecutionFileLocation.create(tc.getTaskWorkingDirectoryPath().resolve(path))));
	}

	@Override
	public IncludeDirectoryTaskOption clone() {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		RelativePathIncludeDirectoryTaskOption other = (RelativePathIncludeDirectoryTaskOption) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}