package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.Collections;

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;

final class RelativePathLinkerInputTaskOption
		implements LinkerInputPassTaskOption, LinkerInputPassOption, FileLinkerInputPass {
	private final SakerPath path;

	public RelativePathLinkerInputTaskOption(SakerPath path) {
		this.path = path;
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext tc) {
		return Collections.singleton(ExecutionFileLocation.create(tc.getTaskWorkingDirectoryPath().resolve(path)));
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