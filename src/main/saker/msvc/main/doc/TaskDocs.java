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
package saker.msvc.main.doc;

import java.util.Collection;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.build.scripting.model.info.TypeInformationKind;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.coptions.COptionsPresetTaskFactory;
import saker.msvc.main.coptions.COptionsPresetType;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class TaskDocs {
	public static final String COMPILE_INCLUDE_DIRECTORIES = "Specifies include directories that are used to resolve #include directives in the source code.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The compilation task doesn't specify any include directories by default.\n"
			+ "Corresponds to the /I command line option for cl.exe.";

	public static final String COMPILE_MACRO_DEFINITIONS = "Specifies key-value pairs which should be added as macro definitions for the compiled files.\n"
			+ "Each entry in this map will be defined as a preprocessor macro definition for the compiled files.\n"
			+ "Corresponds to the /D<key>=<value> command line option for cl.exe.";

	public static final String COMPILE_SIMPLE_PARAMETERS = "Specifies one or more singular arguments that should be directly passed to the compiler backend (cl.exe).\n"
			+ "Any arguments specified will be appended to the cl.exe invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The parameters should be singular, meaning that arguments that require multiple command line options shouldn't be "
			+ "specified here. The order of the specified arguments are irrelevant, and may be changed by the task.";

	public static final String OPTION_SDKS = "Specifies the SDKs (Software Development Kits) used by the task.\n"
			+ "SDKs represent development kits that are available in the build environment and to the task. "
			+ "SDKs are used to determine the appropriate build environment to execute the task, as well "
			+ "to resolve paths against them (e.g. IncludeDirectory, LibraryPath).\n"
			+ "The \"MSVC\" SDK is used to determine the location of the backend executables (cl.exe, link.exe). If it is "
			+ "not specified, then the task will attempt to determine it automatically.";

	public static final String COMPILE_PRECOMPILED_HEADER = "Specifies the path or file location of a header file that should be precompiled.\n"
			+ "Precompiled headers files can be preprocessed by the compiler and included in multiple source files. "
			+ "Using them can result in faster builds as the compiler can reuse the result of the precompilation.\n"
			+ "It is recommended that the precompiled headers contains infrequently changing source files.\n"
			+ "The precompiled header should be included in the compiled source files in the first line. They "
			+ "can be included with the #include \"header_file_name.h\" directive. (Substitute the header_file_name.h with "
			+ "the actual simple file name of the header.)";

	public static final String COMPILE_FORCE_INCLUDE = "Specifies paths or file locations that should be force included in the compiled source files.\n"
			+ "The option corresponds to the /FI argument of cl.exe. The option acts as if the specified file was included with the #include directive "
			+ "at the start of the compiled source file.\n"
			+ "Multiple force included files can be specified. To force include the precompiled header file, use the ForceIncludePrecompiledHeader option set "
			+ "to true.";
	public static final String COMPILE_FORCE_INCLUDE_PRECOMPILED_HEADER = "Boolean that specifies if the precompiled header should be force included in the source files.\n"
			+ "If you're using precompiled headers, you will need to include them at the first line of your source files. Setting this option to true "
			+ "will cause the compiler to include it for you, so you don't need to start your source files with the #include precompiled header directive.\n"
			+ "(In general we don't recommend using this option as that could cause portability issues of your source code.)";

	public static final String LINK_INPUT = "Specifies one or more inputs for the link operation.\n"
			+ "The inputs may be either simple paths, wildcards, file locations, file collections or task output from "
			+ MSVCCCompileTaskFactory.TASK_NAME + "().";

	public static final String LINK_LIBRARY_PATH = "Specifies the library path that will be searched for libraries.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The link task doesn't specify any library paths by default.\n"
			+ "Corresponds to the /LIBPATH command line option for link.exe.";
	public static final String LINK_SIMPLE_PARAMETERS = "Specifies one or more singular arguments that should be directly passed to the linker backend (link.exe).\n"
			+ "Any arguments specified will be appended to the link.exe invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The parameters should be singular, meaning that arguments that require multiple command line options shouldn't be "
			+ "specified here. The order of the specified arguments are irrelevant, and may be changed by the task.";
	public static final String LINK_GENERATE_WINMD = "Sets if Windows Metadata (.winmd) should be generated as part of the linking process.\n"
			+ "If set to true, this option causes the /WINMD flag to be passed for the linker. Windows Metadata file will be generated "
			+ "alongside the output product.\n"
			+ "Setting the option to false will cause /WINMD:NO flag to be passed to the linker.";
	public static final String LINK_BINARY_NAME = "Specifies the file name of the link product without the extension.\n"
			+ "The specified string will be used as the name part of the generated executable or library. The extension \n"
			+ "is determined automatically.\n"
			+ "If not specified, the file name will be generated based on the compilation Identifier.";

	public static final String OPTIONS_IDENTIFIER = "Specifies the Identifier to which the options should be merged into.\n"
			+ "The associated options will only be merged into the target configuration if the target Identifier "
			+ "contains all parts as this Identifier. If no Identifier specified for this options, the Identifier "
			+ "is not considered as a qualifier.";
	public static final String OPTIONS_ARCHITECTURE = "Specifies the Architecture to which the options should be merged into.\n"
			+ "The associated options will only be merged into the target configuration if the target Architecture "
			+ "is the same as the Architecture defined in this options. If no Architecture is specified for this options, "
			+ "the Architecture is not considered as a qualifier.";
	public static final String OPTIONS_LANGUAGE = "Specifies the Language to which the options should be merged into.\n"
			+ "The associated options will only be merged into the target configuration if the target Language "
			+ "is the same as the Language defined in this options. If no Language is specified for this options, "
			+ "the Language is not considered as a qualifier.";

	@NestInformation("Represents the CPU architecture for which a given operation should be performed.")
	@NestTypeInformation(kind = TypeInformationKind.ENUM,
			enumValues = {

					@NestFieldInformation(value = ArchitectureType.X64,
							info = @NestInformation("Architecture for x64.")),
					@NestFieldInformation(value = ArchitectureType.X86,
							info = @NestInformation("Architecture for x86.")),
					@NestFieldInformation(value = ArchitectureType.ARM,
							info = @NestInformation("Architecture for ARM.")),
					@NestFieldInformation(value = ArchitectureType.ARM64,
							info = @NestInformation("Architecture for ARM64.")),

			})
	public static class ArchitectureType {
		public static final String X64 = "x64";
		public static final String X86 = "x86";
		public static final String ARM = "arm";
		public static final String ARM64 = "arm64";
	}

	@NestInformation("Represents the programming language that should be used for compilation.")
	@NestTypeInformation(kind = TypeInformationKind.ENUM,
			enumValues = {

					@NestFieldInformation(value = CompilationLanguage.C,
							info = @NestInformation("Represents the programming language C.")),
					@NestFieldInformation(value = CompilationLanguage.CPP,
							info = @NestInformation("Represents the programming language C++.")),

			})
	public static class CompilationLanguage {
		public static final String C = "C";
		public static final String CPP = "C++";
	}

	@NestInformation("Represents the output of the " + MSVCCLinkTaskFactory.TASK_NAME + "() task.\n"
			+ "The object is a reference to the link operation results using the Microsoft Visual C++ toolchain.\n"
			+ "The result can be consumed in any way the developer sees fit. The OutputPath field contains the path to the "
			+ "linker result binary.")
	@NestFieldInformation(value = "OutputPath",
			type = @NestTypeUsage(SakerPath.class),
			info = @NestInformation("Contains the path to the link operation output.\n"
					+ "The type of the file is based on the configuration of the linker task."))
	@NestFieldInformation(value = "OutputWinmdPath",
			type = @NestTypeUsage(SakerPath.class),
			info = @NestInformation("The output path to the generated Windows Metadata (.winmd) file.\n"
					+ "This path is available only when GenerateWinmd option is set to true for the linker process."))
	@NestFieldInformation(value = "Architecture",
			type = @NestTypeUsage(ArchitectureType.class),
			info = @NestInformation("Contains the target Architecture for which the link operation was done.\n"
					+ "The architecture was either explicitly specified to the compilation task, or was inferred "
					+ "from the inputs."))
	@NestFieldInformation(value = "Identifier",
			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
			info = @NestInformation("Contains the Identifier that the linker task was assigned "
					+ "with when linking the inputs."))
	@NestTypeInformation(qualifiedName = "saker.msvc.MSVCLinkerWorkerTaskOutput")
	public interface DocCLinkerWorkerTaskOutput {
	}

	@NestInformation("Represents the output of the " + MSVCCCompileTaskFactory.TASK_NAME + "() task.\n"
			+ "The object is a reference to the compilation results using the Microsoft Visual C++ toolchain.\n"
			+ "It can be used to the " + MSVCCLinkTaskFactory.TASK_NAME
			+ "() linker task to produce the resulting binary, or "
			+ "consume the compilation results in some other way.")
	@NestFieldInformation(value = "ObjectFilePaths",
			type = @NestTypeUsage(value = Collection.class, elementTypes = { SakerPath.class }),
			info = @NestInformation("Contains the paths to the compilation result object files.\n"
					+ "Each path is the result of the compilation of an input file."))
	@NestFieldInformation(value = "Architecture",
			type = @NestTypeUsage(ArchitectureType.class),
			info = @NestInformation("Contains the target Architecture for which the compilation was done.\n"
					+ "The architecture was either explicitly specified to the compilation task, or was inferred "
					+ "from the enclosing build environment."))
	@NestFieldInformation(value = "Identifier",
			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
			info = @NestInformation("Contains the compilation Identifier that the compilation task was assigned "
					+ "with when compiling the inputs."))
	@NestFieldInformation(value = "SDKs",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class, DocSDKDescription.class }),
			info = @NestInformation("Contains the SDKs that were used for compiling the inputs.\n"
					+ "The map contains all SDKs (explicit or implicit) that was used during the configuration of the compilation."))
	@NestTypeInformation(qualifiedName = "saker.msvc.MSVCCompilerWorkerTaskOutput")
	public interface DocCCompilerWorkerTaskOutput {
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "CMacroName")
	@NestInformation("Name of the defined macro for the C/C++ preprocessor.")
	public static class MacroDefinitionKeyOption {
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "CMacroValue")
	@NestInformation("Value of the defined macro for the C/C++ preprocessor.")
	public static class MacroDefinitionValueOption {
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "MSVCCCompilerParameter")
	@NestInformation("Singular option for the MSVC C compiler (cl.exe) that is directly passed to it on the command line.")
	public static class SimpleCompilerParameterOption {
	}

	@NestTypeInformation(kind = TypeInformationKind.LITERAL, qualifiedName = "MSVCLinkParameter")
	@NestInformation("Singular option for the MSVC linker (link.exe) that is directly passed to it on the command line.")
	public static class SimpleLinkerParameterOption {
	}

	@NestTypeInformation(kind = TypeInformationKind.OBJECT, qualifiedName = "COptionsPresetTaskOutput")
	@NestInformation("Contains one or multiple C option presets.\n"
			+ "C option presets are pre-made C configurations to be used with compiler and linker tasks "
			+ "to for given use-case. Presets can be acquired by using the " + COptionsPresetTaskFactory.TASK_NAME
			+ "() task.\n" + "The object can be directly passed to the " + MSVCCCompileTaskFactory.TASK_NAME + "() and "
			+ MSVCCLinkTaskFactory.TASK_NAME + "() compiler options parameters.")
	@NestFieldInformation(value = "Presets",
			type = @NestTypeUsage(value = Collection.class, elementTypes = { DocPresetCOptions.class }),
			info = @NestInformation("Gets the contained C option presets."))
	public static class DocCOptionsPresetTaskOutput {
	}

	@NestTypeInformation(kind = TypeInformationKind.OBJECT, qualifiedName = "PresetCOptions")
	@NestInformation("A C options preset object.\n" + "Contains C options to be applied to "
			+ MSVCCCompileTaskFactory.TASK_NAME + "() task CompilerOptions parameter and "
			+ MSVCCLinkTaskFactory.TASK_NAME
			+ "() task LinkerOptions parameter in order to apply the requested configuration.")
	@NestFieldInformation(value = "PresetIdentifier",
			type = @NestTypeUsage(COptionsPresetType.class),
			info = @NestInformation("The identifier of the C options preset.\n"
					+ "This is only for display purposes, the preset identifier is not used in any way to configure "
					+ "the associated tasks."))

	@NestFieldInformation(value = "Identifier",
			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
			info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))
	@NestFieldInformation(value = "Language",
			type = @NestTypeUsage(CompilationLanguage.class),
			info = @NestInformation("Specifies the Language to which the options should be merged into in case of compilation. "
					+ "The Language qualifier is not taken into account when the preset is applied to the link task.\n"
					+ "The associated inputs will only be merged into the target configuration if the target Language "
					+ "is the same as the Language defined in this options. If no Language is specified for this options, "
					+ "the Language is not considered as a qualifier."))
	@NestFieldInformation(value = "Architecture",
			type = @NestTypeUsage(ArchitectureType.class),
			info = @NestInformation(TaskDocs.OPTIONS_ARCHITECTURE))

	@NestFieldInformation(value = "IncludeDirectories",
			type = @NestTypeUsage(value = Collection.class, elementTypes = DocIncludeDirectoryPathTaskOption.class),
			info = @NestInformation(TaskDocs.COMPILE_INCLUDE_DIRECTORIES))
	@NestFieldInformation(value = "SDKs",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
							SDKDescriptionTaskOption.class }),
			info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
					+ "When merging, duplicate SDK definitions are not overwritten."))
	@NestFieldInformation(value = "MacroDefinitions",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { MacroDefinitionKeyOption.class, MacroDefinitionValueOption.class }),
			info = @NestInformation(TaskDocs.COMPILE_MACRO_DEFINITIONS + "\n"
					+ "When merging, the macro definitions won't overwrite macro definitions specified previously."))
	@NestFieldInformation(value = "CompileSimpleParameters",
			type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleCompilerParameterOption.class),
			info = @NestInformation(TaskDocs.COMPILE_SIMPLE_PARAMETERS + "\n"
					+ "When merging, duplicate parameters are removed automatically."))

	@NestFieldInformation(value = "LibraryPath",
			type = @NestTypeUsage(value = Collection.class, elementTypes = DocLibraryPathTaskOption.class),
			info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
	@NestFieldInformation(value = "LinkSimpleParameters",
			type = @NestTypeUsage(value = Collection.class, elementTypes = SimpleLinkerParameterOption.class),
			info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS + "\n"
					+ "When merging, duplicate parameters are removed automatically."))
	public static class DocPresetCOptions {
	}

	@NestInformation("Represents a library path that is searched for libraries when linking objects.\n"
			+ "The option accepts simple paths, wildcards, file locations, file collections, and SDK paths.")
	@NestTypeInformation(qualifiedName = "LibraryPathTaskOption")
	public static class DocLibraryPathTaskOption {
	}

	@NestInformation("Represents an include directory for C/C++ compilation.\n"
			+ "Include directories are used to resolve #include directives in source code by the preprocessor.\n"
			+ "The option accepts simple paths, wildcards, file locations, file collections, and SDK paths.")
	@NestTypeInformation(qualifiedName = "IncludeDirectoryPathTaskOption")
	public static class DocIncludeDirectoryPathTaskOption {
	}

	@NestInformation("Represents an include file path for C/C++ compilation.\n")
	@NestTypeInformation(qualifiedName = "ForceIncludeFilePathTaskOption")
	public static class DocIncludeFilePathTaskOption {
	}

}
