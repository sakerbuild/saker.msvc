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
package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.coptions.CommonPresetCOptionsTaskOption;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.msvc.main.options.SimpleParameterTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

@NestInformation("Options configuration to be used with " + MSVCCLinkTaskFactory.TASK_NAME + "().\n"
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
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
@NestFieldInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
				+ "When merging, duplicate SDK definitions are not overwritten."))
@NestFieldInformation(value = "SimpleLinkerParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleParameterTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS + "\n"
				+ "When merging, duplicate parameters are removed automatically."))
@NestFieldInformation(value = "GenerateWinmd",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation(TaskDocs.LINK_GENERATE_WINMD))
@NestFieldInformation(value = "BinaryName",
		type = @NestTypeUsage(String.class),
		info = @NestInformation(TaskDocs.LINK_BINARY_NAME))
public interface MSVCLinkerOptions {
	public void accept(Visitor visitor);

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

	public default Collection<CompilationPathTaskOption> getLibraryPath() {
		return null;
	}

	public default Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	public default List<SimpleParameterTaskOption> getSimpleLinkerParameters() {
		return null;
	}

	public default Boolean getGenerateWinmd() {
		return null;
	}

	public default String getBinaryName() {
		return null;
	}

	public static MSVCLinkerOptions valueOf(COptionsPresetTaskOutput preset) {
		return new CommonPresetCOptionsTaskOption(preset);
	}

	public interface Visitor {
		public default void visit(MSVCLinkerOptions options) {
			throw new UnsupportedOperationException("Unsupported linker options: " + options);
		}

		public default void visit(COptionsPresetTaskOutput options) {
			throw new UnsupportedOperationException("Unsupported linker options: " + options);
		}
	}

}
