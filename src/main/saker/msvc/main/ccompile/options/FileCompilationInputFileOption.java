package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.Set;

import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;

public class FileCompilationInputFileOption
		implements FileCompilationInputPass, CompilationInputPassOption, CompilationInputPassTaskOption {

	private Set<FileLocation> fileLocations;

	public FileCompilationInputFileOption(Set<FileLocation> fileLocations) {
		this.fileLocations = fileLocations;
	}

	@Override
	public void accept(CompilationInputPassOptionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext taskcontext) {
		return fileLocations;
	}

	@Override
	public CompilationInputPassTaskOption clone() {
		return this;
	}

	@Override
	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileLocations == null) ? 0 : fileLocations.hashCode());
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
		FileCompilationInputFileOption other = (FileCompilationInputFileOption) obj;
		if (fileLocations == null) {
			if (other.fileLocations != null)
				return false;
		} else if (!fileLocations.equals(other.fileLocations))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + fileLocations + "]";
	}

}
