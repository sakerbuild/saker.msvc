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
import java.util.Map;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.coptions.CommonPresetCOptionsTaskOption;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.doc.TaskDocs.CompilationLanguage;
import saker.msvc.main.doc.TaskDocs.MacroDefinitionKeyOption;
import saker.msvc.main.doc.TaskDocs.MacroDefinitionValueOption;
import saker.msvc.main.doc.TaskDocs.SimpleCompilerParameterOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.FileLocationTaskOption;

@NestInformation("Represents an options configuration to be used with " + MSVCCCompileTaskFactory.TASK_NAME + "().\n"
		+ "The described options will be merged with the compilation input configuration based on the option qualifiers. "
		+ "The Identifier, Language, and Architecture fields are considered to be used as qualifiers for the option merging, "
		+ "in which case they are tested for mergeability with the input configuration.")

@NestFieldInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))
@NestFieldInformation(value = "Language",
		type = @NestTypeUsage(CompilationLanguage.class),
		info = @NestInformation(TaskDocs.OPTIONS_LANGUAGE))
@NestFieldInformation(value = "Architecture",
		type = @NestTypeUsage(ArchitectureType.class),
		info = @NestInformation(TaskDocs.OPTIONS_ARCHITECTURE))
@NestFieldInformation(value = "IncludeDirectories",
		type = @NestTypeUsage(value = Collection.class, elementTypes = IncludePathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_INCLUDE_DIRECTORIES))
@NestFieldInformation(value = "MacroDefinitions",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { MacroDefinitionKeyOption.class, MacroDefinitionValueOption.class }),
		info = @NestInformation(TaskDocs.COMPILE_MACRO_DEFINITIONS + "\n"
				+ "When merging, the macro definitions won't overwrite macro definitions specified previously."))
@NestFieldInformation(value = "SimpleCompilerParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleCompilerParameterOption.class),
		info = @NestInformation(TaskDocs.COMPILE_SIMPLE_PARAMETERS + "\n"
				+ "When merging, duplicate parameters are removed automatically."))
@NestFieldInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
				+ "When merging, duplicate SDK definitions are not overwritten."))

@NestFieldInformation(value = "PrecompiledHeader",
		type = @NestTypeUsage(FileLocationTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_PRECOMPILED_HEADER + "\n"
				+ "When merging, only a single precompiled header may be used. An exception "
				+ "is thrown in case of conflict."))
@NestFieldInformation(value = "ForceInclude",
		type = @NestTypeUsage(value = Collection.class, elementTypes = IncludePathTaskOption.class),
		info = @NestInformation(TaskDocs.COMPILE_FORCE_INCLUDE))
@NestFieldInformation(value = "ForceIncludePrecompiledHeader",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation(TaskDocs.COMPILE_FORCE_INCLUDE_PRECOMPILED_HEADER + "\n"
				+ "When merging, true will take precedence for this option."))
public interface MSVCCompilerOptions {
	public void accept(MSVCCompilerOptionsVisitor visitor);

	public default MSVCCompilerOptions clone() {
		return new SimpleMSVCCCompilerOptions(this);
	}

	public default CompilationIdentifierTaskOption getIdentifier() {
		return null;
	}

	public default String getLanguage() {
		return null;
	}

	public default String getArchitecture() {
		return null;
	}

	public default Collection<IncludePathTaskOption> getIncludeDirectories() {
		return null;
	}

	public default Map<String, String> getMacroDefinitions() {
		return null;
	}

	public default Collection<String> getSimpleCompilerParameters() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default FileLocationTaskOption getPrecompiledHeader() {
		return null;
	}

	public default Collection<IncludePathTaskOption> getForceInclude() {
		return null;
	}

	public default Boolean getForceIncludePrecompiledHeader() {
		return null;
	}

	public static boolean canMergeArchitectures(String targetarch, String optionsarch) {
		if (ObjectUtils.isNullOrEmpty(optionsarch)) {
			return true;
		}
		return optionsarch.equalsIgnoreCase(targetarch);
	}

	public static MSVCCompilerOptions valueOf(COptionsPresetTaskOutput preset) {
		return new CommonPresetCOptionsTaskOption(preset);
	}
}
