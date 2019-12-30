package saker.msvc.main.clink.options;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.msvc.api.ccompile.MSVCCompilerWorkerTaskOutput;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Represents an input for the " + MSVCCLinkTaskFactory.TASK_NAME + "() task.\n"
		+ "The configuration specifies which files should be added to the input files for the link.exe invocation.\n"
		+ "The configuration accepts simple paths, wildcards, file locations, file collections, and outputs of the "
		+ MSVCCCompileTaskFactory.TASK_NAME + "() task.")
public interface LinkerInputPassTaskOption {
	public LinkerInputPassTaskOption clone();

	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext);

	public static LinkerInputPassTaskOption valueOf(MSVCCompilerWorkerTaskOutput compilationoutput) {
		return new CompilerOutputLinkerInputOption(compilationoutput);
	}

	public static LinkerInputPassTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new FileLinkerInputOption(Collections.singleton(filelocation));
	}

	public static LinkerInputPassTaskOption valueOf(FileCollection files) {
		Set<FileLocation> filelist = ObjectUtils.addAll(new LinkedHashSet<>(), files);
		return new FileLinkerInputOption(ImmutableUtils.unmodifiableSet(filelist));
	}

	public static LinkerInputPassTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardLinkerInputTaskOption(path);
	}

	public static LinkerInputPassTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathLinkerInputTaskOption(path);
		}
		return new FileLinkerInputOption(Collections.singleton(ExecutionFileLocation.create(path)));
	}

	public static LinkerInputPassTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}
}
