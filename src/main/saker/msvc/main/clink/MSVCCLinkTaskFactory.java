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
package saker.msvc.main.clink;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

import saker.build.exception.PropertyComputationFailedException;
import saker.build.file.path.SakerPath;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.SakerLog;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.SimpleStructuredObjectTaskResult;
import saker.build.task.utils.annot.SakerInput;
import saker.build.task.utils.dependencies.EqualityTaskOutputChangeDetector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.trace.BuildTrace;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.api.CompilerUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.api.ccompile.MSVCCompilerWorkerTaskOutput;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.clink.MSVCCLinkWorkerTaskFactory;
import saker.msvc.impl.clink.MSVCCLinkWorkerTaskIdentifier;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.impl.coptions.preset.PresetCOptions;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.impl.util.option.FileCompilationPathOptionImpl;
import saker.msvc.impl.util.option.SDKPathReferenceCompilationPathOption;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.ccompile.options.MSVCCompilerOptions;
import saker.msvc.main.clink.options.CompilerOutputLinkerInputPass;
import saker.msvc.main.clink.options.FileLinkerInputPass;
import saker.msvc.main.clink.options.LinkerInputPassOption;
import saker.msvc.main.clink.options.LinkerInputPassTaskOption;
import saker.msvc.main.clink.options.MSVCLinkerOptions;
import saker.msvc.main.coptions.COptionsPresetTaskFactory;
import saker.msvc.main.doc.TaskDocs;
import saker.msvc.main.doc.TaskDocs.ArchitectureType;
import saker.msvc.main.doc.TaskDocs.DocCLinkerWorkerTaskOutput;
import saker.msvc.main.doc.TaskDocs.DocSimpleLinkerParameterTaskOption;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.msvc.main.options.SimpleParameterTaskOption;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKNameConflictException;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;

@NestInformation("Links input files using the Microsoft Visual C++ toolchain.\n"
		+ "The task provides access to the MSVC linker (link.exe). "
		+ "It can be used to link appropriate input files into a final binary.\n"
		+ "Generally it is used to link the output object files from the " + MSVCCCompileTaskFactory.TASK_NAME
		+ "() task, however, other type of files which are valid inputs to the backend linker are accepted as well.\n"
		+ "The task supports distributing its workload using build clusters.")
@NestTaskInformation(returnType = @NestTypeUsage(DocCLinkerWorkerTaskOutput.class))

@NestParameterInformation(value = "Input",
		aliases = { "" },
		required = true,
		type = @NestTypeUsage(value = Collection.class, elementTypes = LinkerInputPassTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_INPUT))
@NestParameterInformation(value = "Architecture",
		type = @NestTypeUsage(ArchitectureType.class),
		info = @NestInformation("Specifies the linking target Architecture.\n"
				+ "The link operation will target the specified architecture. If no architecture is "
				+ "specified, the task will attempt to infer it from the input objects. It will check if any of the input "
				+ "is an output of the " + MSVCCCompileTaskFactory.TASK_NAME
				+ "() task, and if so the target architecture will be set "
				+ "to the architecture that was used during compilation.\n"
				+ "If the target architecture is incompatible with the input files, then the linker backed (link.exe) will "
				+ "most likely emit an error or warning to signal that.\n"
				+ "The used Architecture will be taken into account as a qualifier when merging LinkerOptions."))

@NestParameterInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation("The Identifier of the link operation.\n"
				+ "Each link task has an identifier that uniquely identifies it during a build execution. "
				+ "The identifier is used to determine the output directory of the link operation. It is also used "
				+ "to merge the appropriate options specified in LinkerOptions parameter.\n"
				+ "An identifier constists of dash separated parts of character sequences of a-z, A-Z, 0-9, _, ., (), [], @.\n"
				+ "An option specification in the LinkerOptions can be merged if "
				+ "the linker identifier contains all parts of the option Identifier.\n"
				+ "If not specified, the identifier is determined based on the current working directory, "
				+ "or assigned to \"default\", however, it won't be subject to option merging."))
@NestParameterInformation(value = "LibraryPath",
		type = @NestTypeUsage(value = Collection.class, elementTypes = CompilationPathTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_LIBRARY_PATH))
@NestParameterInformation(value = "SDKs",
		type = @NestTypeUsage(value = Map.class,
				elementTypes = { saker.sdk.support.main.TaskDocs.DocSdkNameOption.class,
						SDKDescriptionTaskOption.class }),
		info = @NestInformation(TaskDocs.OPTION_SDKS))
@NestParameterInformation(value = "SimpleParameters",
		type = @NestTypeUsage(value = Collection.class, elementTypes = DocSimpleLinkerParameterTaskOption.class),
		info = @NestInformation(TaskDocs.LINK_SIMPLE_PARAMETERS))
@NestParameterInformation(value = "GenerateWinmd",
		type = @NestTypeUsage(boolean.class),
		info = @NestInformation(TaskDocs.LINK_GENERATE_WINMD))
@NestParameterInformation(value = "BinaryName",
		type = @NestTypeUsage(String.class),
		info = @NestInformation(TaskDocs.LINK_BINARY_NAME))

@NestParameterInformation(value = "LinkerOptions",
		type = @NestTypeUsage(value = Collection.class, elementTypes = MSVCLinkerOptions.class),
		info = @NestInformation("Specifies one or more option specifications that are merged with the inputs when applicable.\n"
				+ "The parameter can be used to indirectly specify various linker arguments independent of the actual inputs. "
				+ "This is generally useful when common options need to be specified to multiple link operation inputs.\n"
				+ "When linker arguments are determined, each option specification will be merged into the used argumnets if applicable. "
				+ "An option is considered to be applicable to merging if all of the Identifier parts are contained in the link task Identifier, "
				+ "and the Architecture arguments can be matched.\n"
				+ "In case of unresolveable merge conflicts, the task will throw an appropriate exception.\n"
				+ "Output from the " + COptionsPresetTaskFactory.TASK_NAME
				+ "() task can be passed as a value to this parameter."))
public class MSVCCLinkTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	//XXX examine the undocumented /Brepro flag
	//XXX examine /PDBALTPATH:pdb_file_name option

	public static final String TASK_NAME = "saker.msvc.clink";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {
			@SakerInput(value = { "", "Input" }, required = true)
			public Collection<LinkerInputPassTaskOption> inputOption;

			@SakerInput(value = "Architecture")
			public String architectureOption;

			@SakerInput(value = "Identifier")
			public CompilationIdentifierTaskOption identifierOption;

			@SakerInput(value = "LibraryPath")
			public Collection<CompilationPathTaskOption> libraryPathOption;

			@SakerInput(value = { "SDKs" })
			public Map<String, SDKDescriptionTaskOption> sdksOption;

			@SakerInput(value = "SimpleParameters")
			public Collection<SimpleParameterTaskOption> simpleParametersOption;

			@SakerInput(value = { "LinkerOptions" })
			public Collection<MSVCLinkerOptions> linkerOptionsOption;

			@SakerInput(value = { "GenerateWinmd" })
			public Boolean generateWinmdOption;

			@SakerInput(value = { "BinaryName" })
			public String binaryNameOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_FRONTEND);
				}

				Collection<LinkerInputPassTaskOption> inputtaskoptions = new ArrayList<>();
				Collection<MSVCLinkerOptions> linkeroptions = new ArrayList<>();
				Collection<CompilationPathTaskOption> libpathoptions = new ArrayList<>();
				Map<String, SDKDescriptionTaskOption> sdkoptions = new TreeMap<>(
						SDKSupportUtils.getSDKNameComparator());
				List<SimpleParameterOption> simpleparams = new ArrayList<>();
				Collection<SimpleParameterTaskOption> simpleparamsoption = this.simpleParametersOption;
				addSimpleParameters(simpleparams, simpleparamsoption);
				CompilationIdentifierTaskOption identifieropt = ObjectUtils.clone(this.identifierOption,
						CompilationIdentifierTaskOption::clone);

				CompilationIdentifier optionidentifier = CompilationIdentifierTaskOption.getIdentifier(identifieropt);

				if (!ObjectUtils.isNullOrEmpty(this.inputOption)) {
					for (LinkerInputPassTaskOption inputtaskoption : this.inputOption) {
						if (inputtaskoption == null) {
							continue;
						}
						inputtaskoptions.add(inputtaskoption.clone());
					}
				}
				if (ObjectUtils.isNullOrEmpty(inputtaskoptions)) {
					taskcontext.abortExecution(new IllegalArgumentException("No inputs specified for linking."));
					return null;
				}

				if (!ObjectUtils.isNullOrEmpty(this.linkerOptionsOption)) {
					for (MSVCLinkerOptions linkeropt : this.linkerOptionsOption) {
						if (linkeropt == null) {
							continue;
						}
						linkeroptions.add(linkeropt.clone());
					}
				}
				if (!ObjectUtils.isNullOrEmpty(this.libraryPathOption)) {
					for (CompilationPathTaskOption libpathtaskopt : this.libraryPathOption) {
						if (libpathtaskopt == null) {
							continue;
						}
						libpathoptions.add(libpathtaskopt.clone());
					}
				}
				if (!ObjectUtils.isNullOrEmpty(this.sdksOption)) {
					for (Entry<String, SDKDescriptionTaskOption> entry : this.sdksOption.entrySet()) {
						SDKDescriptionTaskOption sdktaskopt = entry.getValue();
						String sdkname = entry.getKey();
						if (sdkoptions.containsKey(sdkname)) {
							taskcontext.abortExecution(new SDKNameConflictException(
									"SDK with name " + sdkname + " defined multiple times."));
							return null;
						}
						//allow null SDK descriptions to disable inferring
						sdkoptions.put(sdkname, ObjectUtils.clone(sdktaskopt, SDKDescriptionTaskOption::clone));
					}
				}

				Collection<LinkerInputPassOption> inputoptions = new ArrayList<>();
				for (LinkerInputPassTaskOption intaskopt : inputtaskoptions) {
					inputoptions.add(intaskopt.toLinkerInputPassOption(taskcontext));
				}

				String architecture;
				if (this.architectureOption == null) {
					String inferred = inferArchitecture(inputoptions);
					if (inferred != null) {
						architecture = inferred;
					} else {
						try {
							architecture = taskcontext.getTaskUtilities()
									.getReportEnvironmentDependency(SystemArchitectureEnvironmentProperty.INSTANCE);
						} catch (PropertyComputationFailedException e) {
							taskcontext.abortExecution(new UnsupportedOperationException(
									"Failed to determine system architecture. "
											+ "Specify Architecture parameter or run the build on a Windows machine.",
									e));
							return null;
						}
					}
				} else {
					architecture = this.architectureOption;
				}

				Boolean[] genwinmd = { generateWinmdOption };
				String[] binaryname = { binaryNameOption };

				Set<CompilationPathOption> inputfiles = new LinkedHashSet<>();

				Set<CompilationPathOption> librarypath = new LinkedHashSet<>();
				Map<CompilationPathTaskOption, Collection<CompilationPathOption>> calculatedlibpathoptions = new HashMap<>();
				NavigableMap<String, SDKDescription> nullablesdkdescriptions = new TreeMap<>(
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
					nullablesdkdescriptions.put(entry.getKey(), desc[0]);
				}

				for (LinkerInputPassTaskOption inputoption : inputtaskoptions) {
					addLinkerInputs(inputoption, taskcontext, inputfiles, architecture);
				}

				for (CompilationPathTaskOption libpathopt : libpathoptions) {
					Collection<CompilationPathOption> libpaths = calculatedlibpathoptions.computeIfAbsent(libpathopt,
							o -> o.toCompilationPaths(taskcontext));
					ObjectUtils.addAll(librarypath, libpaths);
				}

				for (MSVCLinkerOptions options : linkeroptions) {
					options.accept(new MSVCLinkerOptions.Visitor() {
						@Override
						public void visit(MSVCLinkerOptions options) {
							if (!MSVCCompilerOptions.canMergeArchitectures(architecture, options.getArchitecture())) {
								return;
							}
							if (!CompilerUtils.canMergeIdentifiers(optionidentifier,
									options.getIdentifier() == null ? null : options.getIdentifier().getIdentifier())) {
								return;
							}
							Collection<CompilationPathTaskOption> optlibrarypath = options.getLibraryPath();
							if (!ObjectUtils.isNullOrEmpty(optlibrarypath)) {
								for (CompilationPathTaskOption libpathtaskoption : optlibrarypath) {
									Collection<CompilationPathOption> libpaths = calculatedlibpathoptions
											.computeIfAbsent(libpathtaskoption, o -> o.toCompilationPaths(taskcontext));
									ObjectUtils.addAll(librarypath, libpaths);
								}
							}
							MSVCCCompileTaskFactory.mergeSDKDescriptionOptions(taskcontext, nullablesdkdescriptions,
									options.getSDKs());
							addSimpleParameters(simpleparams, options.getSimpleLinkerParameters());
							Collection<LinkerInputPassTaskOption> optinput = options.getLinkerInput();
							if (!ObjectUtils.isNullOrEmpty(optinput)) {
								for (LinkerInputPassTaskOption opttaskin : optinput) {
									addLinkerInputs(opttaskin, taskcontext, inputfiles, architecture);
								}
							}
							if (Boolean.TRUE.equals(options.getGenerateWinmd())) {
								genwinmd[0] = Boolean.TRUE;
							}
							String optbinname = options.getBinaryName();
							if (!ObjectUtils.isNullOrEmpty(optbinname)) {
								if (binaryname[0] == null) {
									binaryname[0] = optbinname;
								}
							}
						}

						@Override
						public void visit(COptionsPresetTaskOutput options) {
							for (PresetCOptions preset : options.getPresets()) {
								if (!CompilerUtils.canMergeIdentifiers(optionidentifier, preset.getIdentifier())) {
									continue;
								}
								if (!MSVCCompilerOptions.canMergeArchitectures(architecture,
										preset.getArchitecture())) {
									continue;
								}

								ObjectUtils.addAll(librarypath, preset.getLibraryPath());
								MSVCCCompileTaskFactory.mergePresetSDKDescriptions(nullablesdkdescriptions, preset);
								ObjectUtils.addAll(simpleparams, preset.getSimpleLinkerParameters());
								if (Boolean.TRUE.equals(preset.getGenerateWinmd())) {
									genwinmd[0] = Boolean.TRUE;
								}
								ObjectUtils.addAll(inputfiles, preset.getLinkerInput());
							}
						}
					});
				}

				//XXX try to infer the MSVC SDK from input compilation task outputs?
				nullablesdkdescriptions.putIfAbsent(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.DEFAULT_MSVC_SDK_DESCRIPTION);
				nullablesdkdescriptions.values().removeIf(sdk -> sdk == null);
				NavigableMap<String, SDKDescription> sdkdescriptions = ImmutableUtils
						.makeImmutableNavigableMap(nullablesdkdescriptions);

				final CompilationIdentifier identifier;
				if (optionidentifier == null) {
					CompilationIdentifier inferred = inferCompilationIdentifier(inputoptions);
					if (inferred == null) {
						String wdfilename = taskcontext.getTaskWorkingDirectoryPath().getFileName();
						try {
							if (wdfilename != null) {
								inferred = CompilationIdentifier.valueOf(wdfilename);
							}
						} catch (IllegalArgumentException e) {
						}
						if (inferred == null) {
							inferred = CompilationIdentifier.valueOf("default");
						}
					}
					identifier = inferred;
				} else {
					identifier = optionidentifier;
				}

				TaskIdentifier workertaskid = new MSVCCLinkWorkerTaskIdentifier(identifier, architecture);

				MSVCCLinkWorkerTaskFactory worker = new MSVCCLinkWorkerTaskFactory();
				worker.setInputs(inputfiles);
				worker.setSimpleParameters(simpleparams);
				worker.setLibraryPath(librarypath);
				worker.setSdkDescriptions(sdkdescriptions);
				worker.setGenerateWinmd(genwinmd[0]);
				worker.setBinaryName(binaryname[0]);
				taskcontext.startTask(workertaskid, worker, null);

				SimpleStructuredObjectTaskResult result = new SimpleStructuredObjectTaskResult(workertaskid);
				taskcontext.reportSelfTaskOutputChangeDetector(new EqualityTaskOutputChangeDetector(result));
				return result;
			}

		};
	}

	private static String inferArchitecture(Collection<LinkerInputPassOption> inputoptions) {
		String[] result = { null };
		for (LinkerInputPassOption option : inputoptions) {
			option.accept(new LinkerInputPassOption.Visitor() {
				@Override
				public void visit(CompilerOutputLinkerInputPass input) {
					result[0] = input.getCompilerOutput().getArchitecture();
				}

				@Override
				public void visit(FileLinkerInputPass input) {
				}

				@Override
				public void visit(SDKPathCollectionReference input) {
				}
			});
			if (result[0] != null) {
				return result[0];
			}
		}
		return null;
	}

	private static CompilationIdentifier inferCompilationIdentifier(Collection<LinkerInputPassOption> inputoptions) {
		Set<String> parts = new LinkedHashSet<>();
		for (LinkerInputPassOption option : inputoptions) {
			option.accept(new LinkerInputPassOption.Visitor() {
				@Override
				public void visit(CompilerOutputLinkerInputPass input) {
					CompilationIdentifier outputid = input.getCompilerOutput().getIdentifier();
					parts.addAll(outputid.getParts());
				}

				@Override
				public void visit(FileLinkerInputPass input) {
				}

				@Override
				public void visit(SDKPathCollectionReference input) {
				}
			});
		}
		if (parts.isEmpty()) {
			return null;
		}
		return CompilationIdentifier.valueOf(StringUtils.toStringJoin("-", parts));
	}

	private static void addLinkerInputs(LinkerInputPassTaskOption inputoption, TaskContext taskcontext,
			Set<CompilationPathOption> inputfiles, String architecture) {
		inputoption.toLinkerInputPassOption(taskcontext).accept(new LinkerInputPassOption.Visitor() {
			@Override
			public void visit(FileLinkerInputPass input) {
				Collection<FileLocation> filelocations = input.toFileLocations(taskcontext);
				if (!ObjectUtils.isNullOrEmpty(filelocations)) {
					for (FileLocation fl : filelocations) {
						inputfiles.add(new FileCompilationPathOptionImpl(fl));
					}
				}
			}

			@Override
			public void visit(CompilerOutputLinkerInputPass input) {
				MSVCCompilerWorkerTaskOutput compileroutput = input.getCompilerOutput();
				String outputarchitecture = compileroutput.getArchitecture();
				if (outputarchitecture != null) {
					if (!outputarchitecture.equalsIgnoreCase(architecture)) {
						SakerLog.warning().taskScriptPosition(taskcontext).println(
								"Linker input architecture mismatch: " + outputarchitecture + " for " + architecture);
					}
				}

				Collection<SakerPath> filepaths = compileroutput.getObjectFilePaths();
				if (filepaths == null) {
					throw new IllegalArgumentException("null object file paths for compiler putput.");
				}
				for (SakerPath objfilepath : filepaths) {
					inputfiles.add(new FileCompilationPathOptionImpl(ExecutionFileLocation.create(objfilepath)));
				}
			}

			@Override
			public void visit(SDKPathCollectionReference input) {
				inputfiles.add(new SDKPathReferenceCompilationPathOption(input));
			}
		});
	}

	public static void addSimpleParameters(List<SimpleParameterOption> simpleparams,
			Collection<SimpleParameterTaskOption> simpleparamsoption) {
		if (simpleparamsoption == null) {
			return;
		}
		for (SimpleParameterTaskOption sp : simpleparamsoption) {
			if (sp == null) {
				continue;
			}
			simpleparams.add(sp.getParameter());
		}
	}

}
