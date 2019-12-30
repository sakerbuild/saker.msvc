package saker.msvc.main.ccompile.options;

import java.util.Collection;

import saker.build.task.TaskContext;
import saker.std.api.file.location.FileLocation;

public interface FileCompilationInputPass {
	public Collection<FileLocation> toFileLocations(TaskContext taskcontext);
}
