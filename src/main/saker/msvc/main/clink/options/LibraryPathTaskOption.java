package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.msvc.impl.clink.option.FileLibraryPathOption;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.sdk.option.CommonSDKPathReferenceOption;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.sdk.support.api.SDKPathReference;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Represents a library path that is searched for libraries when linking objects.\n"
		+ "The option accepts simple paths, wildcards, file locations, file collections, and SDK paths.")
public interface LibraryPathTaskOption {
	public LibraryPathTaskOption clone();

	public Collection<LibraryPathOption> toLibraryPath(TaskContext taskcontext);

	public static LibraryPathTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new SimpleLibraryPathTaskOption(Collections.singleton(new FileLibraryPathOption(filelocation)));
	}

	public static LibraryPathTaskOption valueOf(FileCollection files) {
		Set<LibraryPathOption> filelist = new LinkedHashSet<>();
		for (FileLocation fl : files) {
			filelist.add(new FileLibraryPathOption(fl));
		}
		return new SimpleLibraryPathTaskOption(ImmutableUtils.unmodifiableSet(filelist));
	}

	public static LibraryPathTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathLibraryPathTaskOption(path);
		}
		return new SimpleLibraryPathTaskOption(
				Collections.singleton(new FileLibraryPathOption(ExecutionFileLocation.create(path))));
	}

	public static LibraryPathTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardLibraryPathTaskOption(path);
	}

	public static LibraryPathTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}

	public static LibraryPathTaskOption valueOf(LibraryPathOption option) {
		return new SimpleLibraryPathTaskOption(Collections.singleton(option));
	}

	public static LibraryPathTaskOption valueOf(SDKPathReference pathreference) {
		return valueOf(new CommonSDKPathReferenceOption(pathreference));
	}
}
