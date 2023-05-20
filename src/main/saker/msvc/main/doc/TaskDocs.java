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
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;

public class TaskDocs {
	public static final String COMPILE_INCLUDE_DIRECTORIES = "Specifies include directories that are used to resolve #include directives in the source code.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The compilation task doesn't specify any include directories by default.\n"
			+ "Corresponds to the /I command line option for cl.exe.";

	public static final String COMPILE_MACRO_DEFINITIONS = "Specifies key-value pairs which should be added as macro definitions for the compiled files.\n"
			+ "Each entry in this map will be defined as a preprocessor macro definition for the compiled files.\n"
			+ "Corresponds to the /D<key>=<value> command line option for cl.exe.\n"
			+ "If value is empty or null, it will be omitted, and -D <key> is used.";

	public static final String COMPILE_SIMPLE_PARAMETERS = "Specifies one or more arguments that should be directly passed to the compiler backend (cl.exe).\n"
			+ "Any arguments specified will be appended to the cl.exe invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The order of the specified arguments are kept.";

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

	public static final String COMPILE_FORCE_INCLUDE = "Specifies the files that should be force included in the compiled source files.\n"
			+ "The option corresponds to the /FI argument of cl.exe. The option acts as if the specified file was included with the #include directive "
			+ "at the start of the compiled source file.\n"
			+ "Multiple force included files can be specified. To force include the precompiled header file, use the ForceIncludePrecompiledHeader option set "
			+ "to true.";
	public static final String COMPILE_FORCE_INCLUDE_PRECOMPILED_HEADER = "Boolean that specifies if the precompiled header should be force included in the source files.\n"
			+ "If you're using precompiled headers, you will need to include them at the first line of your source files. Setting this option to true "
			+ "will cause the compiler to include it for you, so you don't need to start your source files with the #include precompiled header directive.\n"
			+ "(In general we don't recommend using this option as that could cause portability issues of your source code.)";

	public static final String COMPILE_FORCE_USING = "Specifies the files that should be force used in the compiled source files.\n"
			+ "The option corresponds to the /FU argument of cl.exe. The option acts as if the specified file was included with the #using directive "
			+ "at the start of the compiled source file.\n" + "Multiple force used files can be specified.";

	public static final String LINK_INPUT = "Specifies one or more inputs for the link operation.\n"
			+ "The inputs may be either simple paths, wildcards, file locations, file collections or task output from "
			+ MSVCCCompileTaskFactory.TASK_NAME + "().";
	public static final String LINK_LIBRARY_PATH = "Specifies the library path that will be searched for libraries.\n"
			+ "The values may be simple paths, wildcards, file locations, file collections, or SDK paths.\n"
			+ "The link task doesn't specify any library paths by default.\n"
			+ "Corresponds to the /LIBPATH command line option for link.exe.";
	public static final String LINK_SIMPLE_PARAMETERS = "Specifies one or more arguments that should be directly passed to the linker backend (link.exe).\n"
			+ "Any arguments specified will be appended to the link.exe invocation for the inputs. Take care when using this option "
			+ "as specifying files here may result in incorrect incremental builds.\n"
			+ "The order of the specified arguments are kept.";
	public static final String LINK_GENERATE_WINMD = "Sets if Windows Metadata (.winmd) should be generated as part of the linking process.\n"
			+ "If set to true, this option causes the /WINMD flag to be passed for the linker. Windows Metadata file will be generated "
			+ "alongside the output product.\n"
			+ "Setting the option to false will cause /WINMD:NO flag to be passed to the linker.";
	public static final String LINK_BINARY_NAME = "Specifies the file name of the link product without the extension.\n"
			+ "The specified string will be used as the name part of the generated executable or library. The extension "
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
			qualifiedName = "CompilationLanguage",
			enumValues = {

					@NestFieldInformation(value = DocCompilationLanguage.C,
							info = @NestInformation("Represents the programming language C.")),
					@NestFieldInformation(value = DocCompilationLanguage.CPP,
							info = @NestInformation("Represents the programming language C++.")),

			})
	public static class DocCompilationLanguage {
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
	@NestFieldInformation(value = "SDKs",
			type = @NestTypeUsage(value = Map.class,
					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class, DocSDKDescription.class }),
			info = @NestInformation("Contains the SDKs that were used for linking the inputs.\n"
					+ "The map contains all SDKs (explicit or implicit) that was used during the configuration of the linker."))
	@NestTypeInformation(qualifiedName = "saker.msvc.MSVCLinkerWorkerTaskOutput")
	public interface DocCLinkerWorkerTaskOutput {
	}

	@NestInformation("Output of the " + MSVCCCompileTaskFactory.TASK_NAME + "() task.\n"
			+ "The object is a reference to the compilation results using the Microsoft Visual C++ toolchain.\n"
			+ "It can be passed to the " + MSVCCLinkTaskFactory.TASK_NAME
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

	@NestTypeInformation(kind = TypeInformationKind.OBJECT, qualifiedName = "COptionsPresetTaskOutput")
	@NestInformation("Contains one or multiple C option presets.\n"
			+ "C option presets are pre-made C configurations to be used with compiler and linker tasks "
			+ "to for given use-case. Presets can be acquired by using the " + COptionsPresetTaskFactory.TASK_NAME
			+ "() task.\n" + "The object can be directly passed to the " + MSVCCCompileTaskFactory.TASK_NAME + "() and "
			+ MSVCCLinkTaskFactory.TASK_NAME + "() compiler options parameters.")
//	@NestFieldInformation(value = "Presets",
//			type = @NestTypeUsage(value = Collection.class, elementTypes = { DocPresetCOptions.class }),
//			info = @NestInformation("Gets the contained C option presets."))
	public static class DocCOptionsPresetTaskOutput {
	}

	@NestTypeInformation(qualifiedName = "SimpleCompilerParameter", kind = TypeInformationKind.ENUM, enumValues = {
			//(yet) incomplete list of compiler options 

//https://learn.microsoft.com/en-us/cpp/build/reference/bigobj-increase-number-of-sections-in-dot-obj-file?view=msvc-170
			@NestFieldInformation(value = "/bigobj",
					info = @NestInformation("Increases the number of sections that an object file can contain.\n"
							+ "By default, an object file can hold up to 65,279 (almost 2^16) addressable sections. "
							+ "This limit applies no matter which target platform is specified. "
							+ "/bigobj increases that address capacity to 4,294,967,296 (2^32).")),

//https://learn.microsoft.com/en-us/cpp/build/reference/eh-exception-handling-model?view=msvc-170
			@NestFieldInformation(value = "/EHa",
					info = @NestInformation("Enables standard C++ stack unwinding. Catches both structured (asynchronous) and "
							+ "standard C++ (synchronous) exceptions when you use catch(...) syntax. /EHa overrides both /EHs and /EHc arguments.")),
			@NestFieldInformation(value = "/EHs",
					info = @NestInformation("Enables standard C++ stack unwinding. Catches only standard C++ exceptions "
							+ "when you use catch(...) syntax. Unless /EHc is also specified, "
							+ "the compiler assumes that functions declared as extern \"C\" may throw a C++ exception.")),
			@NestFieldInformation(value = "/EHc",
					info = @NestInformation("When used with /EHs, the compiler assumes that functions declared as extern \"C\" "
							+ "never throw a C++ exception. It has no effect when used with /EHa (that is, /EHca is equivalent to /EHa). "
							+ "/EHc is ignored if /EHs or /EHa aren't specified.")),
			@NestFieldInformation(value = "/EHr",
					info = @NestInformation("Tells the compiler to always generate runtime termination checks for all noexcept functions. "
							+ "By default, runtime checks for noexcept may be optimized away if the compiler determines the function calls only non-throwing functions. "
							+ "This option gives strict C++ conformance at the cost of some extra code. /EHr is ignored if /EHs or /EHa aren't specified.")),
			@NestFieldInformation(value = "/EHar", info = @NestInformation("Includes both /EHa and /EHr.")),
			@NestFieldInformation(value = "/EHsc", info = @NestInformation("Includes both /EHs and /EHc.")),
			@NestFieldInformation(value = "/EHscr", info = @NestInformation("Includes /EHs, /EHc and /EHr.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/ga-optimize-for-windows-application?view=msvc-170
			@NestFieldInformation(value = "/GA",
					info = @NestInformation("Optimize for Windows Application.\n"
							+ "Results in more efficient code for an .exe file for accessing thread-local storage (TLS) variables.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gd-gr-gv-gz-calling-convention?view=msvc-170
			@NestFieldInformation(value = "/Gd",
					info = @NestInformation("Determines the order in which function arguments are pushed onto the stack, "
							+ "whether the caller function or called function removes the arguments from the stack at the end of the call, "
							+ "and the name-decorating convention that the compiler uses to identify individual functions.\n"

							+ "/Gd, the default setting, specifies the __cdecl calling convention for all functions except C++ member functions "
							+ "and functions that are marked __stdcall, __fastcall, or __vectorcall.")),
			@NestFieldInformation(value = "/Gr",
					info = @NestInformation("Determines the order in which function arguments are pushed onto the stack, "
							+ "whether the caller function or called function removes the arguments from the stack at the end of the call, "
							+ "and the name-decorating convention that the compiler uses to identify individual functions.\n"

							+ "/Gr specifies the __fastcall calling convention for all functions except C++ member functions, "
							+ "functions named main, and functions that are marked __cdecl, __stdcall, or __vectorcall. "
							+ "All __fastcall functions must have prototypes. "
							+ "This calling convention is only available in compilers that target x86, and is ignored by compilers that target other architectures.")),
			@NestFieldInformation(value = "/Gv",
					info = @NestInformation("Determines the order in which function arguments are pushed onto the stack, "
							+ "whether the caller function or called function removes the arguments from the stack at the end of the call, "
							+ "and the name-decorating convention that the compiler uses to identify individual functions.\n"

							+ "/Gz specifies the __stdcall calling convention for all functions except C++ member functions, "
							+ "functions named main, and functions that are marked __cdecl, __fastcall, or __vectorcall. "
							+ "All __stdcall functions must have prototypes. "
							+ "This calling convention is only available in compilers that target x86, and is ignored by compilers that target other architectures.")),
			@NestFieldInformation(value = "/Gz",
					info = @NestInformation("Determines the order in which function arguments are pushed onto the stack, "
							+ "whether the caller function or called function removes the arguments from the stack at the end of the call, "
							+ "and the name-decorating convention that the compiler uses to identify individual functions.\n"

							+ "/Gv specifies the __vectorcall calling convention for all functions except C++ member functions, "
							+ "functions named main, functions with a vararg variable argument list, "
							+ "or functions that are marked with a conflicting __cdecl, __stdcall, or __fastcall attribute. "
							+ "This calling convention is only available on x86 and x64 architectures that support /arch:SSE2 and above, "
							+ "and is ignored by compilers that target the ARM architecture.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gf-eliminate-duplicate-strings?view=msvc-170
			@NestFieldInformation(value = "/GF",
					info = @NestInformation("Eliminate Duplicate Strings.\n"
							+ "Enables the compiler to create a single copy of identical strings in the program image and in memory during execution. "
							+ "This is an optimization called string pooling that can create smaller programs.\n"
							+ "/GF is in effect when /O1 or /O2 is used.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gl-whole-program-optimization?view=msvc-170
			@NestFieldInformation(value = "/GL",
					info = @NestInformation("Enables whole program optimization.\n"
							+ "Whole program optimization allows the compiler to perform optimizations with information on all modules in the program. "
							+ "Without whole program optimization, optimizations are performed on a per-module (compiland) basis.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gr-enable-run-time-type-information?view=msvc-170
			@NestFieldInformation(value = "/GR",
					info = @NestInformation("Enable Run-Time Type Information.\n"
							+ "Adds code to check object types at run time.\n"
							+ "When /GR is on, the compiler defines the _CPPRTTI preprocessor macro. By default, /GR is on.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gs-buffer-security-check?view=msvc-170
			@NestFieldInformation(value = "/GS",
					info = @NestInformation("Buffer Security Check.\n"
							+ "Detects some buffer overruns that overwrite a function's return address, "
							+ "exception handler address, or certain types of parameters. "
							+ "Causing a buffer overrun is a technique used by hackers to exploit code that does not enforce buffer size restrictions.\n"
							+ "/GS is on by default.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gt-support-fiber-safe-thread-local-storage?view=msvc-170
			@NestFieldInformation(value = "/GT",
					info = @NestInformation("Support fiber-safe thread-local storage.\n"
							+ "Supports fiber safety for data allocated using static thread-local storage, that is, data allocated with __declspec(thread).\n"
							+ "Data declared with __declspec(thread) is referenced through a thread-local storage (TLS) array. "
							+ "The TLS array is an array of addresses that the system maintains for each thread. "
							+ "Each address in this array gives the location of thread-local storage data.\n"
							+ "A fiber is a lightweight object that consists of a stack and a register context "
							+ "and can be scheduled on various threads. A fiber can run on any thread. "
							+ "Because a fiber may get swapped out and restarted later on a different thread, "
							+ "the compiler mustn't cache the address of the TLS array, "
							+ "or optimize it as a common subexpression across a function call. /GT prevents such optimizations.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gw-optimize-global-data?view=msvc-170
			@NestFieldInformation(value = "/Gw",
					info = @NestInformation("Optimize Global Data.\n"
							+ "Package global data in COMDAT sections for optimization.\n"
							+ "The /Gw option causes the compiler to package global data in individual COMDAT sections. "
							+ "By default, /Gw is off and must be explicitly enabled. When both /Gw and /GL are enabled, "
							+ "the linker uses whole-program optimization to compare COMDAT sections across multiple object files "
							+ "in order to exclude unreferenced global data or to merge identical read-only global data. "
							+ "This can significantly reduce the size of the resulting binary executable.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/gy-enable-function-level-linking?view=msvc-170
			@NestFieldInformation(value = "/Gy",
					info = @NestInformation("Enable Function-Level Linking.\n"
							+ "Allows the compiler to package individual functions in the form of packaged functions (COMDATs).")),

//https://learn.microsoft.com/en-us/cpp/build/reference/md-mt-ld-use-run-time-library?view=msvc-170
			@NestFieldInformation(value = "/LD",
					info = @NestInformation("Creates a DLL.\n"
							+ "Passes the /DLL option to the linker. The linker looks for, but does not require, a DllMain function. "
							+ "If you do not write a DllMain function, the linker inserts a DllMain function that returns TRUE.\n"
							+ "Links the DLL startup code.\n"
							+ "Creates an import library (.lib), if an export (.exp) file is not specified on the command line. You link the import library to applications that call your DLL.\n"
							+ "Implies /MT unless you explicitly specify /MD.")),
			@NestFieldInformation(value = "/LDd",
					info = @NestInformation("Creates a debug DLL. Defines _MT and _DEBUG.")),
			@NestFieldInformation(value = "/MD",
					info = @NestInformation("Causes the application to use the multithread-specific and DLL-specific version of the run-time library. "
							+ "Defines _MT and _DLL and causes the compiler to place the library name MSVCRT.lib into the .obj file.")),
			@NestFieldInformation(value = "/MDd",
					info = @NestInformation("Defines _DEBUG, _MT, and _DLL and causes the application to use the debug multithread-specific "
							+ "and DLL-specific version of the run-time library. It also causes the compiler to place the library name MSVCRTD.lib into the .obj file.")),
			@NestFieldInformation(value = "/MT",
					info = @NestInformation("Causes the application to use the multithread, static version of the run-time library. "
							+ "Defines _MT and causes the compiler to place the library name LIBCMT.lib into the .obj file "
							+ "so that the linker will use LIBCMT.lib to resolve external symbols.")),
			@NestFieldInformation(value = "/MTd",
					info = @NestInformation("Defines _DEBUG and _MT. This option also causes the compiler to place the library name LIBCMTD.lib into the .obj file "
							+ "so that the linker will use LIBCMTD.lib to resolve external symbols.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/o-options-optimize-code?view=msvc-170
			@NestFieldInformation(value = "/O1",
					info = @NestInformation("Optimization option to minimize size.\n"
							+ "Equivalent to /Og /Os /Oy /Ob2 /GF /Gy.")),
			@NestFieldInformation(value = "/O2",
					info = @NestInformation("Optimization option to maximize speed.\n"
							+ "Equivalent to /Og /Oi /Ot /Oy /Ob2 /GF /Gy.")),

			@NestFieldInformation(value = "/Ob0",
					info = @NestInformation("Controls inline expansion of functions.\n"
							+ "The default value under /Od. Disables inline expansions.")),
			@NestFieldInformation(value = "/Ob1",
					info = @NestInformation("Controls inline expansion of functions.\n"
							+ "Allows expansion only of functions marked inline, __inline, or __forceinline, or in a C++ member function defined in a class declaration.")),
			@NestFieldInformation(value = "/Ob2",
					info = @NestInformation("Controls inline expansion of functions.\n"
							+ "The default value under /O1 and /O2. Allows the compiler to expand any function not explicitly marked for no inlining.")),
			@NestFieldInformation(value = "/Ob3",
					info = @NestInformation("Controls inline expansion of functions.\n"
							+ "This option specifies more aggressive inlining than /Ob2, but has the same restrictions. The /Ob3 option is available starting in Visual Studio 2019.")),

			@NestFieldInformation(value = "/Od",
					info = @NestInformation("Disable optimization (Debug mode).\n"
							+ "Turns off all optimizations in the program and speeds compilation.")),
			@NestFieldInformation(value = "/Og",
					deprecated = true,
					info = @NestInformation("Provides local and global optimizations, automatic-register allocation, and loop optimization. Deprecated.\n"
							+ "It's recommend that you use either /O1 (Minimize Size) or /O2 (Maximize Speed) instead.")),
			@NestFieldInformation(value = "/Oi",
					info = @NestInformation("Generate Intrinsic Functions.\n"
							+ "Replaces some function calls with intrinsic or otherwise special forms of the function that help your application run faster.")),
			@NestFieldInformation(value = "/Os",
					info = @NestInformation("Favor Small Code.\n"
							+ "Minimizes the size of EXEs and DLLs by instructing the compiler to favor size over speed.")),
			@NestFieldInformation(value = "/Ot",
					info = @NestInformation("Favor Fast Code\n"
							+ "Maximizes the speed of EXEs and DLLs by instructing the compiler to favor speed over size. /Ot is the default when optimizations are enabled.")),
			@NestFieldInformation(value = "/Ox",
					info = @NestInformation("Enable Most Speed Optimizations.\n"
							+ "The /Ox compiler option enables the /O compiler options that favor speed. "
							+ "The /Ox compiler option doesn't include the additional /GF (Eliminate Duplicate Strings) "
							+ "and /Gy (Enable Function-Level Linking) options enabled by /O1 or /O2 (Minimize Size, Maximize Speed). "
							+ "The additional options applied by /O1 and /O2 can cause pointers to strings or to functions to share a target address, "
							+ "which can affect debugging and strict language conformance. The /Ox option is an easy way to enable most optimizations "
							+ "without including /GF and /Gy. For more information, see the descriptions of the /GF and /Gy options.")),
			@NestFieldInformation(value = "/Oy",
					info = @NestInformation("Suppresses creation of frame pointers on the call stack.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/rtc-run-time-error-checks?view=msvc-170
			@NestFieldInformation(value = "/RTC1",
					info = @NestInformation("Run-time error checks. Used to enable and disable the run-time error checks feature, in conjunction with the runtime_checks pragma.\n"
							+ "Equivalent to /RTCsu.")),
			@NestFieldInformation(value = "/RTCc",
					info = @NestInformation("Run-time error checks. Used to enable and disable the run-time error checks feature, in conjunction with the runtime_checks pragma.\n"
							+ "Reports when a value is assigned to a smaller data type and results in a data loss. "
							+ "For example, it reports if a short type value of 0x0101 is assigned to a variable of type char.")),
			@NestFieldInformation(value = "/RTCs",
					info = @NestInformation("Run-time error checks. Used to enable and disable the run-time error checks feature, in conjunction with the runtime_checks pragma.\n"
							+ "Enables stack frame run-time error checking, such as: \n"
							+ "- Initialization of local variables to a nonzero value.\n"
							+ "- Detection of overruns and underruns of local variables such as arrays.\n"
							+ "- Stack pointer verification, which detects stack pointer corruption.")),
			@NestFieldInformation(value = "/RTCu",
					info = @NestInformation("Run-time error checks. Used to enable and disable the run-time error checks feature, in conjunction with the runtime_checks pragma.\n"
							+ "Reports when a variable is used without having been initialized.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/sdl-enable-additional-security-checks?view=msvc-170
			@NestFieldInformation(value = "/sdl",
					info = @NestInformation("Enable Additional Security Checks.\n"
							+ "Enables recommended Security Development Lifecycle (SDL) checks. "
							+ "These checks change security-relevant warnings into errors, "
							+ "and set additional secure code-generation features.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/utf-8-set-source-and-executable-character-sets-to-utf-8?view=msvc-170
			@NestFieldInformation(value = "/utf-8",
					info = @NestInformation("Specifies both the source character set and the execution character set as UTF-8.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/volatile-volatile-keyword-interpretation?view=msvc-170
			@NestFieldInformation(value = "/volatile:iso",
					info = @NestInformation("Specifies how the volatile keyword is to be interpreted.\n"
							+ "Selects strict volatile semantics as defined by the ISO-standard C++ language. "
							+ "Acquire/release semantics are not guaranteed on volatile accesses. "
							+ "If the compiler targets ARM (except ARM64EC), this is the default interpretation of volatile.")),
			@NestFieldInformation(value = "/volatile:ms",
					info = @NestInformation("Specifies how the volatile keyword is to be interpreted.\n"
							+ "Selects Microsoft extended volatile semantics, which add memory ordering guarantees beyond the ISO-standard C++ language. "
							+ "Acquire/release semantics are guaranteed on volatile accesses. "
							+ "However, this option also forces the compiler to generate hardware memory barriers, "
							+ "which might add significant overhead on ARM and other weak memory-ordering architectures. "
							+ "If the compiler targets ARM64EC or any non-ARM platform, this is default interpretation of volatile.")),

//https://learn.microsoft.com/en-us/cpp/build/reference/compiler-option-warning-level?view=msvc-170		
			@NestFieldInformation(value = "/w", info = @NestInformation("Suppresses all compiler warnings.")),
			@NestFieldInformation(value = "/W0",
					info = @NestInformation("/W0 suppresses all warnings. It's equivalent to /w.")),
			@NestFieldInformation(value = "/W1",
					info = @NestInformation("/W1 displays level 1 (severe) warnings. /W1 is the default setting in the command-line compiler.")),
			@NestFieldInformation(value = "/W2",
					info = @NestInformation("/W2 displays level 1 and level 2 (significant) warnings.")),
			@NestFieldInformation(value = "/W3",
					info = @NestInformation("/W3 displays level 1, level 2, and level 3 (production quality) warnings.")),
			@NestFieldInformation(value = "/W4",
					info = @NestInformation("/W4 displays level 1, level 2, and level 3 warnings, and all level 4 (informational) warnings that aren't off by default.")),
			@NestFieldInformation(value = "/Wall",
					info = @NestInformation("Displays all warnings displayed by /W4 and all other warnings that /W4 doesn't include—for example, warnings that are off by default.")),
			@NestFieldInformation(value = "/WX", info = @NestInformation("Treats all compiler warnings as errors.")),

	})
	@NestInformation("Simple compiler option that is directly passed to cl.exe on the command line.")
	public class DocSimpleCompilerParameterTaskOption {
	}

	@NestTypeInformation(qualifiedName = "SimpleLinkerParameter")
	@NestInformation("Simple linker option that is directly passed to link.exe on the command line.")
	public class DocSimpleLinkerParameterTaskOption {
	}

//	@NestTypeInformation(kind = TypeInformationKind.OBJECT, qualifiedName = "PresetCOptions")
//	@NestInformation("A C options preset object.\n" + "Contains C options to be applied to "
//			+ MSVCCCompileTaskFactory.TASK_NAME + "() task CompilerOptions parameter and "
//			+ MSVCCLinkTaskFactory.TASK_NAME
//			+ "() task LinkerOptions parameter in order to apply the requested configuration.")
//	@NestFieldInformation(value = "PresetIdentifier",
//			type = @NestTypeUsage(COptionsPresetType.class),
//			info = @NestInformation("The identifier of the C options preset.\n"
//					+ "This is only for display purposes, the preset identifier is not used in any way to configure "
//					+ "the associated tasks."))
//	@NestFieldInformation(value = "Identifier",
//			type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
//			info = @NestInformation(TaskDocs.OPTIONS_IDENTIFIER))
//	@NestFieldInformation(value = "Language",
//			type = @NestTypeUsage(DocCompilationLanguage.class),
//			info = @NestInformation("Specifies the Language to which the options should be merged into in case of compilation. "
//					+ "The Language qualifier is not taken into account when the preset is applied to the link task.\n"
//					+ "The associated inputs will only be merged into the target configuration if the target Language "
//					+ "is the same as the Language defined in this options. If no Language is specified for this options, "
//					+ "the Language is not considered as a qualifier."))
//	@NestFieldInformation(value = "Architecture",
//			type = @NestTypeUsage(ArchitectureType.class),
//			info = @NestInformation(TaskDocs.OPTIONS_ARCHITECTURE))
//	@NestFieldInformation(value = "IncludeDirectories",
//			type = @NestTypeUsage(value = Collection.class, elementTypes = DocIncludeDirectoryPathTaskOption.class),
//			info = @NestInformation(TaskDocs.COMPILE_INCLUDE_DIRECTORIES))
//	@NestFieldInformation(value = "SDKs",
//			type = @NestTypeUsage(value = Map.class,
//					elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
//							SDKDescriptionTaskOption.class }),
//			info = @NestInformation(TaskDocs.OPTION_SDKS + "\n"
//					+ "When merging, duplicate SDK definitions are not overwritten."))
//	@NestFieldInformation(value = "MacroDefinitions",
//			type = @NestTypeUsage(value = Map.class,
//					elementTypes = { MacroDefinitionKeyOption.class, MacroDefinitionValueOption.class }),
//			info = @NestInformation(TaskDocs.COMPILE_MACRO_DEFINITIONS + "\n"
//					+ "When merging, the macro definitions won't overwrite macro definitions specified previously."))
//	@NestFieldInformation(value = "CompileSimpleParameters",
//			type = @NestTypeUsage(value = Collection.class, elementTypes = DocSimpleCompilerParameterTaskOption.class),
//			info = @NestInformation(TaskDocs.COMPILE_SIMPLE_PARAMETERS + "\n"
//					+ "When merging, duplicate parameters are removed automatically."))
//	@NestFieldInformation(value = "LibraryPath",
//			type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
//			info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
//	@NestFieldInformation(value = "LinkSimpleParameters",
//			type = @NestTypeUsage(value = Collection.class, elementTypes = DocSimpleLinkerParameterTaskOption.class),
//			info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS + "\n"
//					+ "When merging, duplicate parameters are removed automatically."))
//	public static class DocPresetCOptions {
//	}

}
