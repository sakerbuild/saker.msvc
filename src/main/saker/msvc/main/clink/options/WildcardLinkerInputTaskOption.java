package saker.msvc.main.clink.options;

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
import saker.msvc.main.util.TaskTags;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;

final class WildcardLinkerInputTaskOption
		implements LinkerInputPassTaskOption, LinkerInputPassOption, FileLinkerInputPass {
	private final WildcardPath path;

	public WildcardLinkerInputTaskOption(WildcardPath path) {
		this.path = path;
	}

	@Override
	public Collection<FileLocation> toFileLocations(TaskContext tc) {
		FileCollectionStrategy collectionstrategy = WildcardFileCollectionStrategy.create(path);
		NavigableMap<SakerPath, SakerFile> files = tc.getTaskUtilities()
				.collectFilesReportAdditionDependency(TaskTags.TASK_INPUT_FILE, collectionstrategy);
		tc.getTaskUtilities().reportInputFileDependency(TaskTags.TASK_INPUT_FILE,
				ObjectUtils.singleValueMap(files.navigableKeySet(), CommonTaskContentDescriptors.PRESENT));
		LinkedHashSet<FileLocation> result = new LinkedHashSet<>();
		for (SakerPath filepath : files.navigableKeySet()) {
			result.add(ExecutionFileLocation.create(filepath));
		}
		return result;
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