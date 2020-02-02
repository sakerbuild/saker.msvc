/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.file.path.WildcardPath.ReducedWildcardPath;
import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.msvc.impl.ccompile.option.FileIncludePathOption;
import saker.msvc.impl.ccompile.option.IncludePathOption;
import saker.msvc.impl.sdk.option.CommonSDKPathReferenceOption;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.sdk.support.api.SDKPathReference;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileCollection;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Represents an include directory for C/C++ compilation.\n"
		+ "Include directories are used to resolve #include directives in source code by the preprocessor.\n"
		+ "The option accepts simple paths, wildcards, file locations, file collections, and SDK paths.")
public interface IncludePathTaskOption {
	public IncludePathTaskOption clone();

	public Collection<IncludePathOption> toIncludeDirectories(TaskContext taskcontext);

	public static IncludePathTaskOption valueOf(FileLocation filelocation) {
		FileLocationTaskOption.validateFileLocation(filelocation);
		return new SimpleIncludePathTaskOption(
				Collections.singleton(new FileIncludePathOption(filelocation)));
	}

	public static IncludePathTaskOption valueOf(FileCollection files) {
		Set<IncludePathOption> filelist = new LinkedHashSet<>();
		for (FileLocation fl : files) {
			filelist.add(new FileIncludePathOption(fl));
		}
		return new SimpleIncludePathTaskOption(ImmutableUtils.unmodifiableSet(filelist));
	}

	public static IncludePathTaskOption valueOf(SakerPath path) {
		if (!path.isAbsolute()) {
			return new RelativePathIncludePathTaskOption(path);
		}
		return new SimpleIncludePathTaskOption(
				Collections.singleton(new FileIncludePathOption(ExecutionFileLocation.create(path))));
	}

	public static IncludePathTaskOption valueOf(WildcardPath path) {
		ReducedWildcardPath reduced = path.reduce();
		if (reduced.getWildcard() == null) {
			return valueOf(reduced.getFile());
		}
		return new WildcardIncludePathTaskOption(path);
	}

	public static IncludePathTaskOption valueOf(String path) {
		return valueOf(WildcardPath.valueOf(path));
	}

	public static IncludePathTaskOption valueOf(IncludePathOption option) {
		return new SimpleIncludePathTaskOption(Collections.singleton(option));
	}

	public static IncludePathTaskOption valueOf(SDKPathReference pathreference) {
		return valueOf(new CommonSDKPathReferenceOption(pathreference));
	}
}
