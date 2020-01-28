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
package saker.msvc.main.ccompile;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.build.exception.PropertyComputationFailedException;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.io.FileUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.api.CompilerUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.FileCompilationConfiguration;
import saker.msvc.impl.ccompile.MSVCCCompileWorkerTaskFactory;
import saker.msvc.impl.ccompile.MSVCCCompileWorkerTaskIdentifier;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.impl.coptions.preset.PresetCOptions;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.ccompile.options.CompilationInputPassOptionVisitor;
import saker.msvc.main.ccompile.options.CompilationInputPassTaskOption;
import saker.msvc.main.ccompile.options.FileCompilationInputPass;
import saker.msvc.main.ccompile.options.IncludeDirectoryTaskOption;
import saker.msvc.main.ccompile.options.MSVCCompilerOptions;
import saker.msvc.main.ccompile.options.MSVCCompilerOptionsVisitor;
import saker.msvc.main.ccompile.options.OptionCompilationInputPass;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.coptions.COptionsPresetTaskFactory;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.doc.TaskDocs.DocCCompilerWorkerTaskOutput;
import saker.msvc.main.util.FileLocationFileNameVisitor;
import saker.msvc.main.util.TaskTags;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.std.main.file.utils.TaskOptionUtils;

@NestInformation("Compiles C/C++ sources using the Microsoft Visual C++ toolchain.\n"
		+ "The task provides access to the MSVC compiler (cl.exe). It can be used to compile C/C++ source files which "
		+ "can be later linked using the " + MSVCCLinkTaskFactory.TASK_NAME + "() task.\n"
		+ "The task supports distributing its workload using build clusters.")
@NestTaskInformation(returnType = @NestTypeUsage(DocCCompilerWorkerTaskOutput.class))

@NestParameterInformation(value = "Input",
		aliases = { "" },
		required = true,
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationInputPassTaskOption.class),
		info = @NestInformation("Specifies one or more inputs for the compilation.\n"
				+ "The inputs may be either simple paths, wildcards, file locations, file collections or complex configuration specifying the "
				+ "input source files passed to the backed compiler.\n"
				+ "If not specified, the compilation language will be determined based on the extension of an input file. "
				+ "If the file extension ends with \"pp\" or \"xx\", C++ is used by default. In any other cases, the file is "
				+ "compiled for the C language."))
@NestParameterInformation(value = "Architecture",
		type = @NestTypeUsage(ArchitectureType.class),
		info = @NestInformation("Specifies the compilation target Architecture.\n"
				+ "The compilation will target the specified architecture. If no architecture is "
				+ "specified, the one that the build is execution on will be used. If it cannot be determined, "
				+ "an exception is thrown and the Architecture option must be specified in that case.\n"
				+ "The used Architecture will be taken into account as a qualifier when merging CompilerOptions."))

@NestParameterInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation("The Identifier of the compilation.\n"
				+ "Each compilation task has an identifier that uniquely identifies it during a build execution. "
				+ "The identifier is used to determine the output directory of the compilation. It is also used "
				+ "to merge the appropriate options specified in CompilerOptions parameter.\n"
				+ "An identifier constists of dash separated parts of character sequences of a-z, A-Z, 0-9, _, ., (), [], @.\n"
				+ "An option specification in the CompilerOptions can be merged if "
				+ "the compilation identifier contains all parts of the option Identifier.\n"
				+ "If not specified, the identifier is determined based on the current working directory, "
				+ "or assigned to \"default\", however, it won't be subject to option merging."))
@NestParameterInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS))
@NestParameterInformation(value = "CompilerOptions",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MSVCCompilerOptions.class),
		info = @NestInformation("Specifies one or more option specifications that are merged with the inputs when applicable.\n"
				+ "The parameter can be used to indirectly specify various compilation arguments independent of the actual inputs. "
				+ "This is generally useful when common options need to be specified to multiple compilation inputs.\n"
				+ "When compilation arguments are determined, each option specification will be merged into the used argumnets if applicable. "
				+ "An option is considered to be applicable to merging if all of the Identifier parts are contained in the compilation task Identifier, "
				+ "and the Language and Architecture arguments can be matched.\n"
				+ "In case of unresolveable merge conflicts, the task will throw an appropriate exception.\n"
				+ "Output from the " + COptionsPresetTaskFactory.TASK_NAME
				+ "() task can be passed as a value to this parameter."))
public class MSVCCCompileTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.msvc.ccompile";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {

			@SakerInput(value = { "", "Input" }, required = true)
			public Collection<CompilationInputPassTaskOption> inputOption;

			@SakerInput(value = "Architecture")
			public String architectureOption;

			@SakerInput(value = "Identifier")
			public CompilationIdentifierTaskOption identifierOption;

			@SakerInput(value = { "SDKs" })
			public Map<String, SDKDescriptionTaskOption> sdksOption;

			@SakerInput(value = { "CompilerOptions" })
			public Collection<MSVCCompilerOptions> compilerOptionsOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				List<CompilationInputPassTaskOption> inputpasses = new ArrayList<>();
				Collection<MSVCCompilerOptions> compileroptions = new ArrayList<>();
				Map<String, SDKDescriptionTaskOption> sdkoptions = new TreeMap<>(SDKSupportUtils.getSDKNameComparator());

				CompilationIdentifier optionidentifier = this.identifierOption == null ? null
						: this.identifierOption.clone().getIdentifier();

				String architecture;
				if (this.architectureOption == null) {
					try {
						architecture = taskcontext.getTaskUtilities()
								.getReportEnvironmentDependency(SystemArchitectureEnvironmentProperty.INSTANCE);
					} catch (PropertyComputationFailedException e) {
						taskcontext.abortExecution(
								new UnsupportedOperationException("Failed to determine system architecture. "
										+ "Specify Architecture parameter or run the build on a Windows machine.", e));
						return null;
					}
				} else {
					architecture = this.architectureOption;
				}

				for (CompilationInputPassTaskOption inputopt : inputOption) {
					if (inputopt == null) {
						continue;
					}
					CompilationInputPassTaskOption inputpass = inputopt.clone();
					inputpasses.add(inputpass);
				}
				if (!ObjectUtils.isNullOrEmpty(this.compilerOptionsOption)) {
					for (MSVCCompilerOptions options : this.compilerOptionsOption) {
						if (options == null) {
							continue;
						}
						compileroptions.add(options.clone());
					}
				}
				if (!ObjectUtils.isNullOrEmpty(this.sdksOption)) {
					for (Entry<String, SDKDescriptionTaskOption> entry : this.sdksOption.entrySet()) {
						SDKDescriptionTaskOption sdktaskopt = entry.getValue();
						if (sdktaskopt == null) {
							continue;
						}
						SDKDescriptionTaskOption prev = sdkoptions.putIfAbsent(entry.getKey(), sdktaskopt.clone());
						if (prev != null) {
							taskcontext.abortExecution(new IllegalArgumentException(
									"SDK with name " + entry.getKey() + " defined multiple times."));
							return null;
						}
					}
				}

				Map<IncludeDirectoryTaskOption, Collection<IncludeDirectoryOption>> calculatedincludediroptions = new HashMap<>();

				//ignore-case comparison of possible output names of the files
				//  as windows has case-insensitive file names, we need to support Main.cpp and main.cpp from different directories
				NavigableSet<String> outnames = new TreeSet<>(String::compareToIgnoreCase);
				Set<FileCompilationConfiguration> files = new LinkedHashSet<>();
				NavigableMap<String, SDKDescription> sdkdescriptions = new TreeMap<>(SDKSupportUtils.getSDKNameComparator());

				for (Entry<String, SDKDescriptionTaskOption> entry : sdkoptions.entrySet()) {
					SDKDescriptionTaskOption val = entry.getValue();
					SDKDescription[] desc = { null };
					if (val != null) {
						val.accept(new SDKDescriptionTaskOption.Visitor() {
							@Override
							public void visit(SDKDescription description) {
								desc[0] = description;
							}
						});
					}
					sdkdescriptions.putIfAbsent(entry.getKey(), desc[0]);
				}

				FileLocationFileNameVisitor filenamevisitor = new FileLocationFileNameVisitor();

				List<FileCompilationConfiguration> configbuf = new ArrayList<>();
				for (CompilationInputPassTaskOption inputpass : inputpasses) {
					CompilationIdentifier[] subid = { null };
					OptionCompilationInputPass[] suboptioninputpass = { null };
					inputpass.toCompilationInputPassOption(taskcontext).accept(new CompilationInputPassOptionVisitor() {
						@Override
						public void visit(FileCompilationInputPass input) {
							Collection<FileLocation> filelocations = input.toFileLocations(taskcontext);
							if (ObjectUtils.isNullOrEmpty(filelocations)) {
								return;
							}
							for (FileLocation filelocation : filelocations) {
								FileCompilationConfiguration nconfig = createConfigurationForFileLocation(filelocation,
										null, null);
								configbuf.add(nconfig);
							}
						}

						@Override
						public void visit(OptionCompilationInputPass input) {
							Collection<MultiFileLocationTaskOption> files = input.getFiles();
							if (ObjectUtils.isNullOrEmpty(files)) {
								return;
							}

							suboptioninputpass[0] = input;
							Map<String, String> macrodefinitions = input.getMacroDefinitions();
							Set<String> simpleparamoption = ImmutableUtils
									.makeImmutableNavigableSet(input.getSimpleParameters());
							String passlang = input.getLanguage();

							Collection<IncludeDirectoryTaskOption> indirtaskopts = input.getIncludeDirectories();
							Set<IncludeDirectoryOption> inputincludedirs = new LinkedHashSet<>();
							if (!ObjectUtils.isNullOrEmpty(indirtaskopts)) {
								for (IncludeDirectoryTaskOption indirtaskopt : indirtaskopts) {
									Collection<IncludeDirectoryOption> indiroptions = calculatedincludediroptions
											.computeIfAbsent(indirtaskopt, o -> o.toIncludeDirectories(taskcontext));
									ObjectUtils.addAll(inputincludedirs, indiroptions);
								}
							}

							for (MultiFileLocationTaskOption filesopt : files) {
								Collection<FileLocation> filelocations = TaskOptionUtils.toFileLocations(filesopt,
										taskcontext, TaskTags.TASK_INPUT_FILE);
								if (ObjectUtils.isNullOrEmpty(filelocations)) {
									continue;
								}
								CompilationIdentifierTaskOption passsubidopt = input.getSubIdentifier();
								CompilationIdentifier passsubid = CompilationIdentifierTaskOption
										.getIdentifier(passsubidopt);
								subid[0] = passsubid;
								for (FileLocation filelocation : filelocations) {
									FileCompilationConfiguration nconfig = createConfigurationForFileLocation(
											filelocation, passsubid, passlang);
									nconfig.setSimpleParameters(simpleparamoption);
									nconfig.setIncludeDirectories(inputincludedirs);
									nconfig.setMacroDefinitions(macrodefinitions);
									configbuf.add(nconfig);
								}
							}
						}

						private FileCompilationConfiguration createConfigurationForFileLocation(
								FileLocation filelocation, CompilationIdentifier passsubid, String optionlanguage) {
							filelocation.accept(filenamevisitor);
							String pathfilename = filenamevisitor.getFileName();
							if (pathfilename == null) {
								throw new IllegalArgumentException(
										"Input file doesn't have file name: " + filelocation);
							}
							String outfname = getOutFileName(pathfilename, outnames, passsubid);
							String language = getLanguageBasedOnFileName(optionlanguage, pathfilename);

							FileCompilationConfiguration nconfig = new FileCompilationConfiguration(filelocation,
									outfname);
							nconfig.setLanguage(language);
							return nconfig;
						}
					});

					CompilationIdentifier targetmergeidentifier = CompilationIdentifier.concat(optionidentifier,
							subid[0]);

					for (FileCompilationConfiguration config : configbuf) {
						MSVCCompilerOptionsVisitor optionsvisitor = new MSVCCompilerOptionsVisitor() {
							@Override
							public void visit(MSVCCompilerOptions options) {
								if (!CompilerUtils.canMergeIdentifiers(targetmergeidentifier,
										options.getIdentifier() == null ? null
												: options.getIdentifier().getIdentifier())) {
									return;
								}
								if (!CompilerUtils.canMergeLanguages(config.getLanguage(), options.getLanguage())) {
									return;
								}
								if (!MSVCCompilerOptions.canMergeArchitectures(architecture,
										options.getArchitecture())) {
									return;
								}
								Collection<IncludeDirectoryTaskOption> optincludedirs = options.getIncludeDirectories();
								mergeIncludeDirectoryTaskOptions(config, taskcontext, calculatedincludediroptions,
										optincludedirs);
								mergeSDKDescriptionOptions(taskcontext, sdkdescriptions, options.getSDKs());
								mergeMacroDefinitions(config, options.getMacroDefinitions());
								mergeSimpleParameters(config, options.getSimpleCompilerParameters());
							}

							@Override
							public void visit(COptionsPresetTaskOutput options) {
								for (PresetCOptions preset : options.getPresets()) {
									if (!CompilerUtils.canMergeIdentifiers(targetmergeidentifier,
											preset.getIdentifier())) {
										continue;
									}
									if (!CompilerUtils.canMergeLanguages(config.getLanguage(), preset.getLanguage())) {
										continue;
									}
									if (!MSVCCompilerOptions.canMergeArchitectures(architecture,
											preset.getArchitecture())) {
										continue;
									}

									mergeIncludeDirectories(config, preset.getIncludeDirectories());
									mergePresetSDKDescriptions(sdkdescriptions, preset);
									mergeMacroDefinitions(config, preset.getMacroDefinitions());
									mergeSimpleParameters(config, preset.getSimpleCompilerParameters());
								}
							}
						};
						if (suboptioninputpass[0] != null) {
							Collection<MSVCCompilerOptions> subcompileroptions = suboptioninputpass[0]
									.getCompilerOptions();
							if (!ObjectUtils.isNullOrEmpty(subcompileroptions)) {
								for (MSVCCompilerOptions options : subcompileroptions) {
									options.accept(optionsvisitor);
								}
							}
						}
						for (MSVCCompilerOptions options : compileroptions) {
							options.accept(optionsvisitor);
						}
					}

					files.addAll(configbuf);
					configbuf.clear();
				}

				CompilationIdentifier identifier = optionidentifier;
				if (identifier == null) {
					String wdfilename = taskcontext.getTaskWorkingDirectoryPath().getFileName();
					try {
						if (wdfilename != null) {
							identifier = CompilationIdentifier.valueOf(wdfilename);
						}
					} catch (IllegalArgumentException e) {
					}
				}
				if (identifier == null) {
					identifier = CompilationIdentifier.valueOf("default");
				}

				TaskIdentifier workertaskid = new MSVCCCompileWorkerTaskIdentifier(identifier, architecture);

				sdkdescriptions.putIfAbsent(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.DEFAULT_MSVC_SDK_DESCRIPTION);

				MSVCCCompileWorkerTaskFactory worker = new MSVCCCompileWorkerTaskFactory();
				worker.setFiles(files);
				worker.setSdkDescriptions(sdkdescriptions);
				taskcontext.startTask(workertaskid, worker, null);

				SimpleStructuredObjectTaskResult result = new SimpleStructuredObjectTaskResult(workertaskid);
				taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
				return result;
			}

		};
	}

	private static void mergeIncludeDirectoryTaskOptions(FileCompilationConfiguration config, TaskContext taskcontext,
			Map<IncludeDirectoryTaskOption, Collection<IncludeDirectoryOption>> calculatedincludediroptions,
			Collection<IncludeDirectoryTaskOption> includediroptions) {
		if (ObjectUtils.isNullOrEmpty(includediroptions)) {
			return;
		}
		LinkedHashSet<IncludeDirectoryOption> nincludedirs = ObjectUtils
				.newLinkedHashSet(config.getIncludeDirectories());
		for (IncludeDirectoryTaskOption idirtaskopt : includediroptions) {
			Collection<IncludeDirectoryOption> indiroptions = calculatedincludediroptions.computeIfAbsent(idirtaskopt,
					o -> o.toIncludeDirectories(taskcontext));
			ObjectUtils.addAll(nincludedirs, indiroptions);
		}
		config.setIncludeDirectories(nincludedirs);
	}

	private static void mergeSimpleParameters(FileCompilationConfiguration config, Collection<String> simpleparams) {
		if (ObjectUtils.isNullOrEmpty(simpleparams)) {
			return;
		}
		TreeSet<String> result = ObjectUtils.newTreeSet(config.getSimpleParameters());
		if (!result.addAll(simpleparams)) {
			//not changed
			return;
		}
		config.setSimpleParameters(result);
	}

	private static void mergeMacroDefinitions(FileCompilationConfiguration config, Map<String, String> macrodefs) {
		if (ObjectUtils.isNullOrEmpty(macrodefs)) {
			return;
		}
		Map<String, String> configmacros = config.getMacroDefinitions();
		Map<String, String> nmacros;
		if (configmacros == null) {
			nmacros = ImmutableUtils.makeImmutableLinkedHashMap(macrodefs);
		} else {
			nmacros = new LinkedHashMap<>(configmacros);
			for (Entry<String, String> entry : macrodefs.entrySet()) {
				nmacros.putIfAbsent(entry.getKey(), entry.getValue());
			}
		}
		config.setMacroDefinitions(nmacros);
	}

	private static void mergeIncludeDirectories(FileCompilationConfiguration config,
			Collection<IncludeDirectoryOption> includediroptions) {
		if (ObjectUtils.isNullOrEmpty(includediroptions)) {
			return;
		}
		config.setIncludeDirectories(
				ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getIncludeDirectories()), includediroptions));
	}

	public static void mergeSDKDescriptionOptions(TaskContext taskcontext,
			NavigableMap<String, SDKDescription> sdkdescriptions, Map<String, SDKDescriptionTaskOption> sdks) {
		if (ObjectUtils.isNullOrEmpty(sdks)) {
			return;
		}
		for (Entry<String, SDKDescriptionTaskOption> entry : sdks.entrySet()) {
			SDKDescriptionTaskOption val = entry.getValue();
			SDKDescription[] desc = { null };
			if (val != null) {
				val.accept(new SDKDescriptionTaskOption.Visitor() {
					@Override
					public void visit(SDKDescription description) {
						desc[0] = description;
					}
				});
			}
			sdkdescriptions.putIfAbsent(entry.getKey(), desc[0]);
		}
	}

	public static void mergePresetSDKDescriptions(NavigableMap<String, SDKDescription> sdkdescriptions,
			PresetCOptions preset) {
		if (preset == null) {
			return;
		}
		Map<String, SDKDescription> presetsdks = preset.getSDKs();
		if (ObjectUtils.isNullOrEmpty(presetsdks)) {
			return;
		}
		for (Entry<String, SDKDescription> sdkentry : presetsdks.entrySet()) {
			sdkdescriptions.putIfAbsent(sdkentry.getKey(), sdkentry.getValue());
		}
	}

	private static String getLanguageBasedOnFileName(String language, String filename) {
		if (language != null) {
			if (language.equalsIgnoreCase("c++")) {
				return "c++";
			}
			if (language.equalsIgnoreCase("c")) {
				return "c";
			}
			throw new IllegalArgumentException("Unknown language: " + language);
		}
		String extension = FileUtils.getExtension(filename);
		if (extension != null && (StringUtils.endsWithIgnoreCase(extension, "xx")
				|| StringUtils.endsWithIgnoreCase(extension, "pp"))) {
			return "c++";
		}
		return "c";
	}

	private static String getOutFileName(String fname, Set<String> presentfiles, CompilationIdentifier passsubid) {
		Objects.requireNonNull(fname, "file name");
		if (presentfiles.add(fname)) {
			return fname;
		}
		if (passsubid != null) {
			String subidfname = passsubid + "-" + fname;
			if (presentfiles.add(subidfname)) {
				return subidfname;
			}
		}
		int i = 1;
		int dotidx = fname.lastIndexOf('.');
		String base;
		String end;
		if (dotidx < 0) {
			//no extension
			base = fname;
			end = "";
		} else {
			base = fname.substring(0, dotidx);
			end = fname.substring(dotidx);
		}
		while (true) {
			String nfname = base + "_" + i + end;
			if (presentfiles.add(nfname)) {
				return nfname;
			}
			++i;
		}
	}

}
