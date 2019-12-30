package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.NavigableMap;

import saker.build.file.SakerFile;
import saker.build.file.path.SakerPath;
import saker.build.file.path.WildcardPath;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.TaskContext;
import saker.build.task.dependencies.FileCollectionStrategy;
import saker.build.task.utils.dependencies.WildcardFileCollectionStrategy;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.msvc.impl.ccompile.option.FileIncludeDirectoryOption;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.std.api.file.location.ExecutionFileLocation;

final class WildcardIncludeDirectoryTaskOption implements IncludeDirectoryTaskOption {
	private final WildcardPath path;

	public WildcardIncludeDirectoryTaskOption(WildcardPath path) {
		this.path = path;
	}

	@Override
	public Collection<IncludeDirectoryOption> toIncludeDirectories(TaskContext tc) {
		FileCollectionStrategy collectionstrategy = WildcardFileCollectionStrategy.create(path);
		NavigableMap<SakerPath, SakerFile> files = tc.getTaskUtilities().collectFilesReportAdditionDependency(null,
				collectionstrategy);
		tc.getTaskUtilities().reportInputFileDependency(null,
				ObjectUtils.singleValueMap(files.navigableKeySet(), CommonTaskContentDescriptors.PRESENT));
		LinkedHashSet<IncludeDirectoryOption> result = new LinkedHashSet<>();
		for (SakerPath filepath : files.navigableKeySet()) {
			result.add(new FileIncludeDirectoryOption(ExecutionFileLocation.create(filepath)));
		}
		return result;
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
		WildcardIncludeDirectoryTaskOption other = (WildcardIncludeDirectoryTaskOption) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}
	
}