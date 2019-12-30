package saker.msvc.main.clink.options;

import java.util.Collection;

import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;

public interface FileLinkerInputPass {
	public Collection<FileLocation> toFileLocations(TaskContext taskcontext);
}
