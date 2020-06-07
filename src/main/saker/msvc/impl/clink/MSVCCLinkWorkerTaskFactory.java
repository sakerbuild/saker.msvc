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
package saker.msvc.impl.clink;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;

import saker.build.exception.FileMirroringUnavailableException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.InnerTaskResultHolder;
import saker.build.task.InnerTaskResults;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.task.TaskFactory;
import saker.build.task.exception.TaskEnvironmentSelectionFailedException;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.trace.BuildTrace;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.api.clink.MSVCLinkerWorkerTaskOutput;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.FileCompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.msvc.impl.util.ByteSinkProcessIOConsumer;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKManagementException;
import saker.sdk.support.api.exc.SDKNotFoundException;
import saker.sdk.support.api.exc.SDKPathNotFoundException;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import saker.std.api.util.SakerStandardUtils;

public class MSVCCLinkWorkerTaskFactory
		implements TaskFactory<MSVCLinkerWorkerTaskOutput>, Task<MSVCLinkerWorkerTaskOutput>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	public static final Set<String> ALWAYS_PRESENT_LINK_PARAMETERS = ImmutableUtils
			.makeImmutableNavigableSet(new String[] {
//					Suppress logo
					"/nologo",

			});

	private Set<FileLocation> inputs;
	private Set<CompilationPathOption> libraryPath;
	private NavigableMap<String, SDKDescription> sdkDescriptions;
	private List<SimpleParameterOption> simpleParameters;
	private Boolean generateWinmd;

	private String binaryName;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCLinkWorkerTaskFactory() {
	}

	public void setInputs(Set<FileLocation> inputs) {
		this.inputs = inputs;
	}

	public void setLibraryPath(Set<CompilationPathOption> libraryPath) {
		this.libraryPath = libraryPath;
	}

	public void setGenerateWinmd(Boolean generateWinmd) {
		this.generateWinmd = generateWinmd;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkDescriptions) {
		ObjectUtils.requireComparator(sdkDescriptions, SDKSupportUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkDescriptions;
		if (sdkDescriptions.get(MSVCUtils.SDK_NAME_MSVC) == null) {
			throw new SDKNotFoundException(MSVCUtils.SDK_NAME_MSVC + " SDK is not present for compilation.");
		}
	}

	public void setSimpleParameters(List<SimpleParameterOption> simpleParameters) {
		if (simpleParameters == null) {
			this.simpleParameters = Collections.emptyList();
		} else {
			this.simpleParameters = ImmutableUtils.makeImmutableList(simpleParameters);
		}
	}

	public void setBinaryName(String binaryName) {
		this.binaryName = binaryName;
	}

	@Override
	public Set<String> getCapabilities() {
		return WORKER_TASK_CAPABILITIES;
	}

	@Override
	public MSVCLinkerWorkerTaskOutput run(TaskContext taskcontext) throws Exception {
		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_WORKER);
		}

		TaskIdentifier taskid = taskcontext.getTaskId();
		if (!(taskid instanceof MSVCCLinkWorkerTaskIdentifier)) {
			taskcontext.abortExecution(
					new IllegalStateException("Invalid task identifier for: " + this.getClass().getName()));
			return null;
		}
		MSVCCLinkWorkerTaskIdentifier workertaskid = (MSVCCLinkWorkerTaskIdentifier) taskid;
		CompilationIdentifier passcompilationidentifier = workertaskid.getPassIdentifier();
		String passidstr = passcompilationidentifier.toString();
		String architecture = workertaskid.getArchitecture();
		String displayid = MSVCCLinkTaskFactory.TASK_NAME + ":" + passidstr + "/" + architecture;
		taskcontext.setStandardOutDisplayIdentifier(displayid);

		if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
			BuildTrace.setDisplayInformation("msvc.clink:" + passidstr + "/" + architecture, displayid);
		}

		SakerDirectory outdir = SakerPathFiles.requireBuildDirectory(taskcontext)
				.getDirectoryCreate(MSVCCLinkTaskFactory.TASK_NAME).getDirectoryCreate(passidstr)
				.getDirectoryCreate(architecture);

		SakerPath outdirpath = outdir.getSakerPath();

		TaskExecutionEnvironmentSelector envselector = SDKSupportUtils
				.getSDKBasedClusterExecutionEnvironmentSelector(sdkDescriptions.values());
		NavigableMap<String, SDKDescription> linkerinnertasksdkdescriptions;
		if (envselector != null) {
			EnvironmentSelectionResult envselectionresult;
			try {
				envselectionresult = taskcontext.getTaskUtilities().getReportExecutionDependency(
						SakerStandardUtils.createEnvironmentSelectionTestExecutionProperty(envselector));
			} catch (Exception e) {
				throw new TaskEnvironmentSelectionFailedException(
						"Failed to select a suitable build environment for linking.", e);
			}
			linkerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(envselectionresult, sdkDescriptions);
			envselector = SDKSupportUtils
					.getSDKBasedClusterExecutionEnvironmentSelector(linkerinnertasksdkdescriptions.values());
		} else {
			NavigableMap<String, SDKReference> resolvedsdks = SDKSupportUtils.resolveSDKReferences(taskcontext,
					sdkDescriptions);
			linkerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(sdkDescriptions, resolvedsdks);
		}

		int inputsize = inputs.size();
		System.out.println("Linking " + inputsize + " file" + (inputsize == 1 ? "" : "s") + ".");

		String binaryname = this.binaryName;
		if (ObjectUtils.isNullOrEmpty(binaryname)) {
			binaryname = passidstr;
		}

		LinkerInnerTaskFactory innertaskfactory = new LinkerInnerTaskFactory(envselector, inputs, libraryPath,
				linkerinnertasksdkdescriptions, simpleParameters, architecture, outdirpath, generateWinmd, binaryname);
		InnerTaskResults<LinkerInnerTaskFactoryResult> innertaskresults = taskcontext.startInnerTask(innertaskfactory,
				null);
		InnerTaskResultHolder<LinkerInnerTaskFactoryResult> nextres = innertaskresults.getNext();
		if (nextres == null) {
			throw new RuntimeException("Failed to start linker task.");
		}
		LinkerInnerTaskFactoryResult innertaskresult = nextres.getResult();

		MSVCLinkerWorkerTaskOutputImpl result = new MSVCLinkerWorkerTaskOutputImpl(passcompilationidentifier,
				architecture, innertaskresult.getOutputPath(), innertaskresult.getOutputWinmdPath(),
				linkerinnertasksdkdescriptions);
		return result;
	}

	@Override
	public Task<? extends MSVCLinkerWorkerTaskOutput> createTask(ExecutionContext executioncontext) {
		return this;
	}

	private static boolean containsIgnoreCase(Iterable<String> commands, String cmd) {
		for (String c : commands) {
			if (c.equalsIgnoreCase(cmd)) {
				return true;
			}
		}
		return false;
	}

	private static void addAlwaysPresentParameters(List<String> commands) {
		for (String p : ALWAYS_PRESENT_LINK_PARAMETERS) {
			if (!containsIgnoreCase(commands, p)) {
				//the simple parameters might've added it already
				commands.add(p);
			}
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, inputs);
		SerialUtils.writeExternalCollection(out, libraryPath);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
		out.writeObject(generateWinmd);
		out.writeObject(binaryName);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
		libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
		simpleParameters = SerialUtils.readExternalImmutableList(in);
		generateWinmd = SerialUtils.readExternalObject(in);
		binaryName = SerialUtils.readExternalObject(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((binaryName == null) ? 0 : binaryName.hashCode());
		result = prime * result + ((generateWinmd == null) ? 0 : generateWinmd.hashCode());
		result = prime * result + ((inputs == null) ? 0 : inputs.hashCode());
		result = prime * result + ((libraryPath == null) ? 0 : libraryPath.hashCode());
		result = prime * result + ((sdkDescriptions == null) ? 0 : sdkDescriptions.hashCode());
		result = prime * result + ((simpleParameters == null) ? 0 : simpleParameters.hashCode());
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
		MSVCCLinkWorkerTaskFactory other = (MSVCCLinkWorkerTaskFactory) obj;
		if (binaryName == null) {
			if (other.binaryName != null)
				return false;
		} else if (!binaryName.equals(other.binaryName))
			return false;
		if (generateWinmd == null) {
			if (other.generateWinmd != null)
				return false;
		} else if (!generateWinmd.equals(other.generateWinmd))
			return false;
		if (inputs == null) {
			if (other.inputs != null)
				return false;
		} else if (!inputs.equals(other.inputs))
			return false;
		if (libraryPath == null) {
			if (other.libraryPath != null)
				return false;
		} else if (!libraryPath.equals(other.libraryPath))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		if (simpleParameters == null) {
			if (other.simpleParameters != null)
				return false;
		} else if (!simpleParameters.equals(other.simpleParameters))
			return false;
		return true;
	}

	private static class LinkerInnerTaskFactoryResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		private SakerPath outputPath;
		private SakerPath outputWinmdPath;

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactoryResult() {
		}

		public LinkerInnerTaskFactoryResult(SakerPath outputPath, SakerPath outputwinmdpath) {
			this.outputPath = outputPath;
			this.outputWinmdPath = outputwinmdpath;
		}

		public SakerPath getOutputPath() {
			return outputPath;
		}

		public SakerPath getOutputWinmdPath() {
			return outputWinmdPath;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(outputPath);
			out.writeObject(outputWinmdPath);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			outputPath = SerialUtils.readExternalObject(in);
			outputWinmdPath = SerialUtils.readExternalObject(in);
		}
	}

	private static class LinkerInnerTaskFactory
			implements TaskFactory<LinkerInnerTaskFactoryResult>, Task<LinkerInnerTaskFactoryResult>, Externalizable {
		private static final long serialVersionUID = 1L;

		private TaskExecutionEnvironmentSelector environmentSelector;
		private Set<FileLocation> inputs;
		private Set<CompilationPathOption> libraryPath;
		private NavigableMap<String, SDKDescription> sdkDescriptions;
		private List<SimpleParameterOption> simpleParameters;
		private String architecture;
		private SakerPath outDirectoryPath;
		private Boolean generateWinmd;
		private String binaryName;

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactory() {
		}

		public LinkerInnerTaskFactory(TaskExecutionEnvironmentSelector environmentSelector, Set<FileLocation> inputs,
				Set<CompilationPathOption> libraryPath, NavigableMap<String, SDKDescription> sdkDescriptions,
				List<SimpleParameterOption> simpleParameters, String architecture, SakerPath outdirpath,
				Boolean generateWinmd, String binaryName) {
			this.environmentSelector = environmentSelector;
			this.inputs = inputs;
			this.libraryPath = libraryPath;
			this.sdkDescriptions = sdkDescriptions;
			this.simpleParameters = simpleParameters;
			this.architecture = architecture;
			this.outDirectoryPath = outdirpath;
			this.generateWinmd = generateWinmd;
			this.binaryName = binaryName;
		}

		@Override
		public LinkerInnerTaskFactoryResult run(TaskContext taskcontext) throws Exception {
			NavigableMap<SakerPath, ContentDescriptor> inputdescriptors = new TreeMap<>();

			Collection<Path> inputfilepaths = new LinkedHashSet<>();
			Collection<Path> libpaths = new LinkedHashSet<>();
			for (FileLocation inputfilelocation : inputs) {
				inputfilelocation.accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						SakerPath path = loc.getPath();
						SakerFile inputfile = taskcontext.getTaskUtilities().resolveFileAtPath(path);
						if (inputfile == null) {
							throw ObjectUtils
									.sneakyThrow(new FileNotFoundException("Linker input file not found: " + path));
						}
						inputdescriptors.put(path, inputfile.getContentDescriptor());
						try {
							inputfilepaths.add(taskcontext.mirror(inputfile));
						} catch (FileMirroringUnavailableException | NullPointerException | IOException e) {
							throw ObjectUtils.sneakyThrow(e);
						}
					}
				});
			}

			NavigableMap<String, SDKReference> sdks = SDKSupportUtils
					.resolveSDKReferences(taskcontext.getExecutionContext().getEnvironment(), sdkDescriptions);

			SakerEnvironment environment = taskcontext.getExecutionContext().getEnvironment();
			if (!ObjectUtils.isNullOrEmpty(this.libraryPath)) {
				for (CompilationPathOption libpathoption : this.libraryPath) {
					libpathoption.accept(new CompilationPathOption.Visitor() {
						@Override
						public void visit(FileCompilationPathOption libpath) {
							libpath.getFileLocation().accept(new FileLocationVisitor() {
								@Override
								public void visit(ExecutionFileLocation loc) {
									SakerPath path = loc.getPath();
									SakerDirectory dir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(path);
									if (dir == null) {
										throw ObjectUtils.sneakyThrow(
												new FileNotFoundException("Library path directory not found: " + path));
									}

									NavigableMap<SakerPath, ContentDescriptor> dircontents = SakerPathFiles
											.toFileContentMap(dir.getFilesRecursiveByPath(path,
													DirectoryVisitPredicate.subFiles()));
									inputdescriptors.putAll(dircontents);

									try {
										libpaths.add(taskcontext.mirror(dir));
									} catch (FileMirroringUnavailableException | IOException e) {
										throw ObjectUtils.sneakyThrow(e);
									}
								}

								@Override
								public void visit(LocalFileLocation loc) {
									// TODO handle local file location input libpath
									FileLocationVisitor.super.visit(loc);
								}
							});
						}

						@Override
						public void visit(SDKPathCollectionReference libpath) {
							//XXX duplicated code with compiler worker
							try {
								Collection<SakerPath> paths = libpath.getValue(sdks);
								if (paths == null) {
									throw new SDKPathNotFoundException("No SDK library path found for: " + libpath);
								}
								for (SakerPath sdkdirpath : paths) {
									libpaths.add(LocalFileProvider.toRealPath(sdkdirpath));
								}
							} catch (SDKManagementException e) {
								throw e;
							} catch (Exception e) {
								throw new SDKPathNotFoundException(
										"Failed to retrieve library path from SDKs: " + libpath);
							}
						}
					});
				}
			}
			String hostarchitecture = environment
					.getEnvironmentPropertyCurrentValue(SystemArchitectureEnvironmentProperty.INSTANCE);

			SDKReference vcsdk = sdks.get(MSVCUtils.SDK_NAME_MSVC);
			if (vcsdk == null) {
				throw new SDKNotFoundException(MSVCUtils.SDK_NAME_MSVC);
			}

			SakerPath linkexepath = MSVCUtils.getVCSDKExecutablePath(vcsdk, hostarchitecture, architecture,
					MSVCUtils.VC_EXECUTABLE_NAME_LINK);
			if (linkexepath == null) {
				throw new SDKPathNotFoundException("SDK doesn't contain appropriate link.exe: " + vcsdk);
			}
			SakerPath workingdir = MSVCUtils.getVCSDKExecutableWorkingDirectoryPath(vcsdk, hostarchitecture,
					architecture, MSVCUtils.VC_EXECUTABLE_NAME_LINK);
			if (workingdir == null) {
				workingdir = linkexepath.getParent();
			}

			List<String> commands = new ArrayList<>();
			commands.add(linkexepath.toString());
			MSVCUtils.evaluateSimpleParameters(commands, simpleParameters, sdks);
			addAlwaysPresentParameters(commands);
			commands.add("/MACHINE:" + architecture);

			for (Path lpath : libpaths) {
				commands.add("/LIBPATH:" + lpath);
			}

			boolean librarylink = containsIgnoreCase(commands, "/dll");
			String extension;
			if (librarylink) {
				extension = ".dll";
			} else {
				extension = ".exe";
			}
			SakerPath outputexecpath = outDirectoryPath.resolve(this.binaryName + extension);
			SakerPath outputwinmdpath;
			Path outputwinmdmirrorpath;
			if (generateWinmd != null) {
				if (generateWinmd) {
					outputwinmdpath = outDirectoryPath.resolve(this.binaryName + ".winmd");
					outputwinmdmirrorpath = taskcontext.getExecutionContext().toMirrorPath(outputwinmdpath);
					commands.add("/WINMD");
					commands.add("/WINMDFILE:" + outputwinmdmirrorpath);
				} else {
					outputwinmdpath = null;
					outputwinmdmirrorpath = null;
					commands.add("/WINMD:NO");
				}
			} else {
				outputwinmdpath = null;
				outputwinmdmirrorpath = null;
			}

			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.setDisplayInformation(outputexecpath.getFileName(), null);
			}

			Path outputmirrorpath = taskcontext.getExecutionContext().toMirrorPath(outputexecpath);
			commands.add("/OUT:" + outputmirrorpath);
			for (Path inputpath : inputfilepaths) {
				commands.add(inputpath.toString());
			}
			LocalFileProvider localfp = LocalFileProvider.getInstance();
			localfp.createDirectories(outputmirrorpath.getParent());

			int procresult = MSVCUtils.runMSVCProcess(commands, workingdir,
					new ByteSinkProcessIOConsumer(taskcontext.getStandardOut()), null, true);
			if (procresult != 0) {
				throw new IOException("Failed to link: " + procresult + " (0x" + Integer.toHexString(procresult) + ")");
			}
			ProviderHolderPathKey outputpathkey = localfp.getPathKey(outputmirrorpath);

			taskcontext.invalidate(outputpathkey);

			SakerDirectory outdir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(outDirectoryPath);

			SakerFile outputsakerfile = taskcontext.getTaskUtilities()
					.createProviderPathFile(outputexecpath.getFileName(), outputpathkey);
			outdir.add(outputsakerfile);
			outputsakerfile.synchronize();
			if (outputwinmdmirrorpath != null) {
				ProviderHolderPathKey outputwinmdpathkey = localfp.getPathKey(outputwinmdmirrorpath);
				taskcontext.invalidate(outputwinmdpathkey);

				SakerFile outputwinmdsakerfile = taskcontext.getTaskUtilities()
						.createProviderPathFile(outputwinmdpath.getFileName(), outputwinmdpathkey);
				outdir.add(outputwinmdsakerfile);
				outputwinmdsakerfile.synchronize();
				taskcontext.getTaskUtilities().reportOutputFileDependency(LinkerFileTags.OUTPUT_FILE,
						outputwinmdsakerfile);
			}

			taskcontext.getTaskUtilities().reportInputFileDependency(LinkerFileTags.INPUT_FILE, inputdescriptors);
			taskcontext.getTaskUtilities().reportOutputFileDependency(LinkerFileTags.OUTPUT_FILE, outputsakerfile);

			LinkerInnerTaskFactoryResult result = new LinkerInnerTaskFactoryResult(outputexecpath, outputwinmdpath);

			if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
				BuildTrace.reportOutputArtifact(outputexecpath, BuildTrace.ARTIFACT_EMBED_DEFAULT);
			}

			return result;
		}

		@Override
		public Task<? extends LinkerInnerTaskFactoryResult> createTask(ExecutionContext executioncontext) {
			return this;
		}

		@Override
		public Set<String> getCapabilities() {
			if (environmentSelector != null) {
				return Collections.singleton(CAPABILITY_REMOTE_DISPATCHABLE);
			}
			return TaskFactory.super.getCapabilities();
		}

		@Override
		public TaskExecutionEnvironmentSelector getExecutionEnvironmentSelector() {
			if (environmentSelector != null) {
				return environmentSelector;
			}
			return TaskFactory.super.getExecutionEnvironmentSelector();
		}

		@Override
		public int getRequestedComputationTokenCount() {
			return 1;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(environmentSelector);
			SerialUtils.writeExternalCollection(out, inputs);
			SerialUtils.writeExternalCollection(out, libraryPath);
			SerialUtils.writeExternalMap(out, sdkDescriptions);
			SerialUtils.writeExternalCollection(out, simpleParameters);
			out.writeObject(architecture);
			out.writeObject(outDirectoryPath);
			out.writeObject(binaryName);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			environmentSelector = (TaskExecutionEnvironmentSelector) in.readObject();
			inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
			libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
			sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
					SDKSupportUtils.getSDKNameComparator());
			simpleParameters = SerialUtils.readExternalImmutableList(in);
			architecture = SerialUtils.readExternalObject(in);
			outDirectoryPath = SerialUtils.readExternalObject(in);
			binaryName = SerialUtils.readExternalObject(in);
		}

	}

}
