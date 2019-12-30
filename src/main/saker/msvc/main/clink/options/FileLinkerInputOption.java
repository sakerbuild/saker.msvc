package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.Set;

import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;

final class FileLinkerInputOption implements LinkerInputPassTaskOption, FileLinkerInputPass, LinkerInputPassOption {
	private final Set<FileLocation> fileLocations;

	public FileLinkerInputOption(Set<FileLocation> fileLocations) {
		this.fileLocations = fileLocations;
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext tc) {
		return fileLocations;
	}

	@Override
	public LinkerInputPassTaskOption clone() {
		return this;
	}

	@Override
	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public void accept(LinkerInputPassOptionVisitor visitor) {
		visitor.visit(this);
	}
}