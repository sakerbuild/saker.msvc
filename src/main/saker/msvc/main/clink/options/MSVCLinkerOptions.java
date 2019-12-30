package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.coptions.CommonPresetCOptionsTaskOption;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.doc.TaskDocs.SimpleLinkerParameterOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

@NestInformation("Represents an options configuration to be used with " + MSVCCLinkTaskFactory.TASK_NAME + "().\n"
		+ "The described options will be merged with the linker input configuration based on the option qualifiers. "
		+ "The Identifier and Architecture fields are considered to be used as qualifiers for the option merging, "
		+ "in which case they are tested for mergeability with the input configuration.")

@NestFieldInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))
@NestFieldInformation(value = "Architecture",
		type = @NestTypeUsage(ArchitectureType.class),
		info = @NestInformation(TaskDocs.OPTIONS_ARCHITECTURE))

@NestFieldInformation(value = "LinkerInput",
		type = @NestTypeUsage(value = Collection.class, elementTypes = LinkerInputPassTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_INPUT))

@NestFieldInformation(value = "LibraryPath",
		type = @NestTypeUsage(value = Collection.class, elementTypes = LibraryPathTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
@NestFieldInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class, SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
				+ "When merging, duplicate SDK definitions are not overwritten."))
@NestFieldInformation(value = "SimpleLinkerParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleLinkerParameterOption.class),
		info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS + "\n"
				+ "When merging, duplicate parameters are removed automatically."))
public interface MSVCLinkerOptions {
	public void accept(MSVCLinkerOptionsVisitor visitor);

	public default MSVCLinkerOptions clone() {
		return new SimpleMSVCLinkerOptions(this);
	}

	public default CompilationIdentifierTaskOption getIdentifier() {
		return null;
	}

	public default String getArchitecture() {
		return null;
	}

	public default Collection<LinkerInputPassTaskOption> getLinkerInput() {
		return null;
	}

	public default Collection<LibraryPathTaskOption> getLibraryPath() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default Collection<String> getSimpleLinkerParameters() {
		return null;
	}

	public static MSVCLinkerOptions valueOf(COptionsPresetTaskOutput preset) {
		return new CommonPresetCOptionsTaskOption(preset);
	}
}
