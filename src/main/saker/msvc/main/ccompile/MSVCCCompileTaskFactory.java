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
import saker.build.trace.BuildTrace;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.api.CompilerUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.FileCompilationConfiguration;
import saker.msvc.impl.ccompile.FileCompilationProperties;
import saker.msvc.impl.ccompile.MSVCCCompileWorkerTaskFactory;
import saker.msvc.impl.ccompile.MSVCCCompileWorkerTaskIdentifier;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.impl.coptions.preset.PresetCOptions;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.ccompile.options.CompilationInputPassOption;
import saker.msvc.main.ccompile.options.CompilationInputPassTaskOption;
import saker.msvc.main.ccompile.options.FileCompilationInputPass;
import saker.msvc.main.ccompile.options.MSVCCompilerOptions;
import saker.msvc.main.ccompile.options.OptionCompilationInputPass;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.coptions.COptionsPresetTaskFactory;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.doc.TaskDocs.DocCCompilerWorkerTaskOutput;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.msvc.main.options.SimpleParameterTaskOption;
import saker.msvc.main.util.TaskTags;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKNameConflictException;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.FileLocation;
import saker.std.main.file.option.MultiFileLocationTaskOption;
import saker.std.main.file.utils.TaskOptionUtils;

@NestTaskInformation(returnType = @NestTypeUsage(DocCCompilerWorkerTaskOutput.class))
@NestInformation("Compiles C/C++ sources using the Microsoft Visual C++ toolchain.\n"
		+ "The task provides access to the MSVC compiler (cl.exe). It can be used to compile C/C++ source files which "
		+ "can be later linked using the " + MSVCCLinkTaskFactory.TASK_NAME + "() task.\n"
		+ "The task supports distributing its workload using build clusters.")

@NestParameterInformation(value = "Input",
		aliases = { "" },
		required = true,
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationInputPassTaskOption.class),
		info = @NestInformation("Specifies one or more inputs for the compilation.\n"
				+ "The inputs may be either simple paths, wildcards, file locations, file collections or complex configuration specifying the "
				+ "input source files passed to the backend compiler.\n"
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
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_FRONTEND);
				}

				List<CompilationInputPassTaskOption> inputpasses = new ArrayList<>();
				Collection<MSVCCompilerOptions> compileroptions = new ArrayList<>();
				Map<String, SDKDescriptionTaskOption> sdkoptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());

				CompilationIdentifier optionidentifier = this.identifierOption == null ? null
						: this.identifierOption.clone().getIdentifier();

				String architecture;
				if (ObjectUtils.isNullOrEmpty(this.architectureOption)) {
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
							taskcontext.abortExecution(new SDKNameConflictException(
									"SDK with name " + entry.getKey() + " defined multiple times."));
							return null;
						}
					}
				}

				Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculateddiroptions = new HashMap<>();
				Map<FileCompilationProperties, String> precompiledheaderoutnamesconfigurations = new HashMap<>();

				//ignore-case comparison of possible output names of the files
				//  as windows has case-insensitive file names, we need to support Main.cpp and main.cpp from different directories
				NavigableSet<String> outnames = new TreeSet<>(String::compareToIgnoreCase);
				Set<FileCompilationConfiguration> files = new LinkedHashSet<>();
				NavigableMap<String, SDKDescription> sdkdescriptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());

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

				List<ConfigSetupHolder> configbuf = new ArrayList<>();
				for (CompilationInputPassTaskOption inputpass : inputpasses) {
					CompilationIdentifier[] subid = { null };
					OptionCompilationInputPass[] suboptioninputpass = { null };
					inputpass.toCompilationInputPassOption(taskcontext)
							.accept(new CompilationInputPassOption.Visitor() {
								@Override
								public void visit(FileCompilationInputPass input) {
									Collection<FileLocation> filelocations = input.toFileLocations(taskcontext);
									if (ObjectUtils.isNullOrEmpty(filelocations)) {
										return;
									}
									for (FileLocation filelocation : filelocations) {
										FileCompilationConfiguration nconfig = createConfigurationForProperties(
												new FileCompilationProperties(filelocation));
										configbuf.add(new ConfigSetupHolder(nconfig));
									}
								}

								@Override
								public void visit(OptionCompilationInputPass input) {
									Collection<MultiFileLocationTaskOption> files = input.getFiles();
									if (ObjectUtils.isNullOrEmpty(files)) {
										return;
									}

									Map<String, String> macrodefinitions = input.getMacroDefinitions();
									List<SimpleParameterOption> simpleparamoption = new ArrayList<>();
									MSVCCLinkTaskFactory.addSimpleParameters(simpleparamoption,
											input.getSimpleParameters());
									String passlang = input.getLanguage();
									FileLocation pchfilelocation = TaskOptionUtils
											.toFileLocation(input.getPrecompiledHeader(), taskcontext);

									Set<CompilationPathOption> inputincludedirs = toIncludePathOptions(taskcontext,
											calculateddiroptions, input.getIncludeDirectories());
									Set<CompilationPathOption> inputforceincludes = toIncludePathOptions(taskcontext,
											calculateddiroptions, input.getForceInclude());
									Set<CompilationPathOption> inputforceusings = toIncludePathOptions(taskcontext,
											calculateddiroptions, input.getForceUsing());
									Boolean forceincludepch = input.getForceIncludePrecompiledHeader();

									CompilationIdentifierTaskOption passsubidopt = input.getSubIdentifier();
									CompilationIdentifier passsubid = CompilationIdentifierTaskOption
											.getIdentifier(passsubidopt);
									subid[0] = passsubid;
									suboptioninputpass[0] = input;

									for (MultiFileLocationTaskOption filesopt : files) {
										Collection<FileLocation> filelocations = TaskOptionUtils
												.toFileLocations(filesopt, taskcontext, TaskTags.TASK_INPUT_FILE);
										if (ObjectUtils.isNullOrEmpty(filelocations)) {
											continue;
										}
										for (FileLocation filelocation : filelocations) {
											FileCompilationProperties nproperties = new FileCompilationProperties(
													filelocation);
											nproperties.setSimpleParameters(simpleparamoption);
											nproperties.setIncludeDirectories(inputincludedirs);
											nproperties.setMacroDefinitions(macrodefinitions);
											nproperties.setForceInclude(inputforceincludes);
											nproperties.setForceUsing(inputforceusings);

											FileCompilationConfiguration nconfig = createConfigurationForProperties(
													nproperties, passsubid, passlang);
											if (Boolean.TRUE.equals(forceincludepch)) {
												nconfig.setPrecompiledHeaderForceInclude(true);
											}
											configbuf.add(new ConfigSetupHolder(nconfig, pchfilelocation));
										}
									}
								}

								private FileCompilationConfiguration createConfigurationForProperties(
										FileCompilationProperties properties, CompilationIdentifier passsubid,
										String optionlanguage) {
									FileLocation filelocation = properties.getFileLocation();
									String pathfilename = MSVCUtils.getFileName(filelocation);
									if (pathfilename == null) {
										throw new IllegalArgumentException(
												"Input file doesn't have file name: " + filelocation);
									}
									String outfname = getOutFileName(pathfilename, outnames, passsubid);
									String language = getLanguageBasedOnFileName(optionlanguage, pathfilename);

									FileCompilationConfiguration nconfig = new FileCompilationConfiguration(outfname,
											properties);
									properties.setLanguage(language);
									return nconfig;
								}

								private FileCompilationConfiguration createConfigurationForProperties(
										FileCompilationProperties properties) {
									return createConfigurationForProperties(properties, null, null);
								}
							});

					CompilationIdentifier targetmergeidentifier = CompilationIdentifier.concat(optionidentifier,
							subid[0]);

					for (ConfigSetupHolder configholder : configbuf) {
						FileCompilationConfiguration config = configholder.config;
						FileCompilationProperties configproperties = config.getProperties();
						String configlanguage = configproperties.getLanguage();
						MSVCCompilerOptions.Visitor optionsvisitor = new MSVCCompilerOptions.Visitor() {
							@Override
							public void visit(MSVCCompilerOptions options) {
								if (!CompilerUtils.canMergeIdentifiers(targetmergeidentifier,
										options.getIdentifier() == null ? null
												: options.getIdentifier().getIdentifier())) {
									return;
								}
								Collection<String> optionslang = options.getLanguage();
								if (!canMergeLanguages(configlanguage, optionslang)) {
									return;
								}
								if (!MSVCCompilerOptions.canMergeArchitectures(architecture,
										options.getArchitecture())) {
									return;
								}
								mergeCompilerOptions(options, configproperties, taskcontext, sdkdescriptions);
							}

							@Override
							public void visit(COptionsPresetTaskOutput options) {
								for (PresetCOptions preset : options.getPresets()) {
									if (!CompilerUtils.canMergeIdentifiers(targetmergeidentifier,
											preset.getIdentifier())) {
										continue;
									}
									String optionslang = preset.getLanguage();
									if (!CompilerUtils.canMergeLanguages(configlanguage, optionslang)) {
										continue;
									}
									if (!MSVCCompilerOptions.canMergeArchitectures(architecture,
											preset.getArchitecture())) {
										continue;
									}

									mergePreset(preset, configproperties, sdkdescriptions);
								}
							}

							private void mergeCompilerOptions(MSVCCompilerOptions options,
									FileCompilationProperties compilationproperties, TaskContext taskcontext,
									NavigableMap<String, SDKDescription> sdkdescriptions) {
								mergeIncludeDirectories(compilationproperties, toIncludePathOptions(taskcontext,
										calculateddiroptions, options.getIncludeDirectories()));
								mergeForceIncludes(compilationproperties, toIncludePathOptions(taskcontext,
										calculateddiroptions, options.getForceInclude()));
								mergeForceUsings(compilationproperties, toIncludePathOptions(taskcontext,
										calculateddiroptions, options.getForceUsing()));
								mergeSDKDescriptionOptions(taskcontext, sdkdescriptions, options.getSDKs());
								mergeMacroDefinitions(compilationproperties, options.getMacroDefinitions());
								mergeSimpleParameterOptions(compilationproperties,
										options.getSimpleCompilerParameters());

								mergePrecompiledHeader(configholder,
										TaskOptionUtils.toFileLocation(options.getPrecompiledHeader(), taskcontext));
								if (Boolean.TRUE.equals(options.getForceIncludePrecompiledHeader())) {
									config.setPrecompiledHeaderForceInclude(true);
								}
							}

							private void mergePreset(PresetCOptions preset,
									FileCompilationProperties compilationproperties,
									NavigableMap<String, SDKDescription> sdkdescriptions) {
								mergeIncludeDirectories(compilationproperties, preset.getIncludeDirectories());
								mergeForceIncludes(compilationproperties, preset.getForceInclude());
								mergeForceUsings(compilationproperties, preset.getForceUsing());
								mergePresetSDKDescriptions(sdkdescriptions, preset);
								mergeMacroDefinitions(compilationproperties, preset.getMacroDefinitions());
								mergeSimpleParameters(compilationproperties, preset.getSimpleCompilerParameters());

								mergePrecompiledHeader(configholder, preset.getPrecompiledHeader());
								if (Boolean.TRUE.equals(preset.getForceIncludePrecompiledHeader())) {
									config.setPrecompiledHeaderForceInclude(true);
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

					//resolve precompiled headers
					for (ConfigSetupHolder configholder : configbuf) {
						if (configholder.precompiledHeader != null) {
							FileCompilationProperties pchprops = new FileCompilationProperties(
									configholder.precompiledHeader);
							pchprops.copyFrom(configholder.config.getProperties());

							String pchoutfilename = precompiledheaderoutnamesconfigurations.get(pchprops);
							if (pchoutfilename == null) {
								String pchfilename = MSVCUtils.getFileName(configholder.precompiledHeader);

								pchoutfilename = getOutFileName(pchfilename, outnames, null);
								precompiledheaderoutnamesconfigurations.put(pchprops, pchoutfilename);
							}
							configholder.config.setPrecompiledHeader(configholder.precompiledHeader, pchoutfilename);
						}

						files.add(configholder.config);
					}
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

				sdkdescriptions.putIfAbsent(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.DEFAULT_MSVC_SDK_DESCRIPTION);

				TaskIdentifier workertaskid = new MSVCCCompileWorkerTaskIdentifier(identifier, architecture);

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

	private static Set<CompilationPathOption> toIncludePathOptions(TaskContext taskcontext,
			Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedincludediroptions,
			Collection<CompilationPathTaskOption> indirtaskopts) {
		Set<CompilationPathOption> inputincludedirs = new LinkedHashSet<>();
		collectIncludeDirectoryOptions(taskcontext, calculatedincludediroptions, indirtaskopts, inputincludedirs);
		return inputincludedirs;
	}

	private static void collectIncludeDirectoryOptions(TaskContext taskcontext,
			Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedincludediroptions,
			Collection<CompilationPathTaskOption> indirtaskopts, Set<CompilationPathOption> inputincludedirs) {
		if (ObjectUtils.isNullOrEmpty(indirtaskopts)) {
			return;
		}
		for (CompilationPathTaskOption indirtaskopt : indirtaskopts) {
			Collection<CompilationPathOption> indiroptions = calculatedincludediroptions.computeIfAbsent(indirtaskopt,
					o -> o.toCompilationPaths(taskcontext));
			ObjectUtils.addAll(inputincludedirs, indiroptions);
		}
	}

	private static void mergeSimpleParameterOptions(FileCompilationProperties config,
			Collection<SimpleParameterTaskOption> simpleparams) {
		if (ObjectUtils.isNullOrEmpty(simpleparams)) {
			return;
		}
		List<SimpleParameterOption> result = ObjectUtils.newArrayList(config.getSimpleParameters());
		MSVCCLinkTaskFactory.addSimpleParameters(result, simpleparams);
		config.setSimpleParameters(result);
	}

	private static void mergeSimpleParameters(FileCompilationProperties config,
			Collection<SimpleParameterOption> simpleparams) {
		if (ObjectUtils.isNullOrEmpty(simpleparams)) {
			return;
		}
		List<SimpleParameterOption> result = ObjectUtils.newArrayList(config.getSimpleParameters());
		result.addAll(simpleparams);
		config.setSimpleParameters(result);
	}

	private static void mergeMacroDefinitions(FileCompilationProperties config, Map<String, String> macrodefs) {
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

	private static void mergeIncludeDirectories(FileCompilationProperties config,
			Collection<CompilationPathOption> includediroptions) {
		if (ObjectUtils.isNullOrEmpty(includediroptions)) {
			return;
		}
		config.setIncludeDirectories(
				ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getIncludeDirectories()), includediroptions));
	}

	private static void mergeForceIncludes(FileCompilationProperties config,
			Collection<CompilationPathOption> includeoptions) {
		if (ObjectUtils.isNullOrEmpty(includeoptions)) {
			return;
		}
		config.setForceInclude(
				ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getForceInclude()), includeoptions));
	}

	private static void mergeForceUsings(FileCompilationProperties config,
			Collection<CompilationPathOption> usingoptions) {
		if (ObjectUtils.isNullOrEmpty(usingoptions)) {
			return;
		}
		config.setForceUsing(ObjectUtils.addAll(ObjectUtils.newLinkedHashSet(config.getForceUsing()), usingoptions));
	}

	private static boolean canMergeLanguages(String targetlang, Collection<String> optionslang) {
		if (ObjectUtils.isNullOrEmpty(optionslang)) {
			return true;
		}
		for (String lang : optionslang) {
			if (CompilerUtils.canMergeLanguages(targetlang, lang)) {
				return true;
			}
		}
		return false;
	}

	private static void mergePrecompiledHeader(ConfigSetupHolder configholder, FileLocation pch) {
		if (pch == null) {
			return;
		}
		FileLocation presentpch = configholder.precompiledHeader;
		if (presentpch != null && !presentpch.equals(pch)) {
			throw new IllegalArgumentException(
					"Option merge conflict for precompiled header: " + pch + " and " + presentpch);
		}
		configholder.precompiledHeader = pch;
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

	private static class ConfigSetupHolder {
		public FileCompilationConfiguration config;
		public FileLocation precompiledHeader;

		public ConfigSetupHolder(FileCompilationConfiguration config) {
			this.config = config;
		}

		public ConfigSetupHolder(FileCompilationConfiguration config, FileLocation precompiledHeader) {
			this.config = config;
			this.precompiledHeader = precompiledHeader;
		}

	}

}
