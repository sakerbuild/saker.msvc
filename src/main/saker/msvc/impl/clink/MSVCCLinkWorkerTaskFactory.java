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
import java.io.File;
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
import java.util.TreeSet;

import saker.build.exception.FileMirroringUnavailableException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.EnvironmentProperty;
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
import saker.build.thirdparty.saker.util.io.ByteSink;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.compiler.utils.api.options.CompilationIdentifier;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.SDKBasedExecutionEnvironmentSelector;
import saker.msvc.impl.clink.option.FileLibraryPath;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.clink.option.LibraryPathVisitor;
import saker.msvc.impl.util.EnvironmentSelectionTestExecutionProperty;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.ResolvedSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKDescriptionVisitor;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.UserSDKDescription;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;

public class MSVCCLinkWorkerTaskFactory implements TaskFactory<Object>, Task<Object>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	private Set<FileLocation> inputs;
	private Set<LibraryPathOption> libraryPath;
	private NavigableMap<String, SDKDescription> sdkDescriptions;
	private NavigableSet<String> simpleParameters;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCLinkWorkerTaskFactory() {
	}

	public void setInputs(Set<FileLocation> inputs) {
		this.inputs = inputs;
	}

	public void setLibraryPath(Set<LibraryPathOption> libraryPath) {
		this.libraryPath = libraryPath;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkDescriptions) {
		ObjectUtils.requireComparator(sdkDescriptions, MSVCUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkDescriptions;
		if (!sdkDescriptions.containsKey(MSVCUtils.SDK_NAME_MSVC)) {
			throw new IllegalArgumentException("MSVC SDK unspecified for linking.");
		}
	}

	public void setSimpleParameters(Set<String> simpleParameters) {
		if (simpleParameters == null) {
			this.simpleParameters = Collections.emptyNavigableSet();
		} else {
			TreeSet<String> nset = new TreeSet<>(MSVCUtils.getLinkerParameterIgnoreCaseComparator());
			nset.addAll(simpleParameters);
			nset.remove("/nologo");
			this.simpleParameters = nset;
		}
	}

	@Override
	public Set<String> getCapabilities() {
		return WORKER_TASK_CAPABILITIES;
	}

	@Override
	public Object run(TaskContext taskcontext) throws Exception {
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
		taskcontext
				.setStandardOutDisplayIdentifier(MSVCCLinkTaskFactory.TASK_NAME + ":" + passidstr + "/" + architecture);

		SakerDirectory outdir = SakerPathFiles.requireBuildDirectory(taskcontext)
				.getDirectoryCreate(MSVCCLinkTaskFactory.TASK_NAME).getDirectoryCreate(passidstr)
				.getDirectoryCreate(architecture);

		SakerPath outdirpath = outdir.getSakerPath();

		SDKBasedExecutionEnvironmentSelector envselector = MSVCUtils.createEnvironmentSelectorForSDKs(sdkDescriptions);
		NavigableMap<String, SDKDescription> linkerinnertasksdkdescriptions = sdkDescriptions;
		if (envselector != null) {
			EnvironmentSelectionResult envselectionresult;
			try {
				envselectionresult = taskcontext.getTaskUtilities()
						.getReportExecutionDependency(new EnvironmentSelectionTestExecutionProperty(envselector));
			} catch (Exception e) {
				throw new TaskEnvironmentSelectionFailedException(
						"Failed to select a suitable build environment for linking.", e);
			}
			envselector = MSVCUtils.undefaultizeSDKEnvironmentSelector(envselector, envselectionresult);
			linkerinnertasksdkdescriptions = envselector.getDescriptions();
		}

		System.out.println("Linking " + inputs.size() + " files.");

		LinkerInnerTaskFactory innertaskfactory = new LinkerInnerTaskFactory(envselector, inputs, libraryPath,
				linkerinnertasksdkdescriptions, simpleParameters, architecture, outdirpath, passidstr);
		InnerTaskResults<LinkerInnerTaskFactoryResult> innertaskresults = taskcontext.startInnerTask(innertaskfactory,
				null);
		InnerTaskResultHolder<LinkerInnerTaskFactoryResult> nextres = innertaskresults.getNext();
		if (nextres == null) {
			throw new RuntimeException("Failed to start linker task.");
		}
		LinkerInnerTaskFactoryResult innertaskresult = nextres.getResult();

		MSVCLinkerWorkerTaskOutputImpl result = new MSVCLinkerWorkerTaskOutputImpl(passcompilationidentifier,
				architecture, innertaskresult.getOutputPath());
		return result;
	}

	@Override
	public Task<? extends Object> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, inputs);
		SerialUtils.writeExternalCollection(out, libraryPath);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
		libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in, MSVCUtils.getSDKNameComparator());
		simpleParameters = SerialUtils.readExternalSortedImmutableNavigableSet(in,
				MSVCUtils.getLinkerParameterIgnoreCaseComparator());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
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

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactoryResult() {
		}

		public LinkerInnerTaskFactoryResult(SakerPath outputPath) {
			this.outputPath = outputPath;
		}

		public SakerPath getOutputPath() {
			return outputPath;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(outputPath);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			outputPath = (SakerPath) in.readObject();
		}
	}

	private static class LinkerInnerTaskFactory
			implements TaskFactory<LinkerInnerTaskFactoryResult>, Task<LinkerInnerTaskFactoryResult>, Externalizable {
		private static final long serialVersionUID = 1L;

		private TaskExecutionEnvironmentSelector environmentSelector;
		private Set<FileLocation> inputs;
		private Set<LibraryPathOption> libraryPath;
		private NavigableMap<String, SDKDescription> sdkDescriptions;
		private NavigableSet<String> simpleParameters;
		private String architecture;
		private SakerPath outDirectoryPath;
		private String passIdentifier;

		/**
		 * For {@link Externalizable}.
		 */
		public LinkerInnerTaskFactory() {
		}

		public LinkerInnerTaskFactory(TaskExecutionEnvironmentSelector environmentSelector, Set<FileLocation> inputs,
				Set<LibraryPathOption> libraryPath, NavigableMap<String, SDKDescription> sdkDescriptions,
				NavigableSet<String> simpleParameters, String architecture, SakerPath outdirpath, String passid) {
			this.environmentSelector = environmentSelector;
			this.inputs = inputs;
			this.libraryPath = libraryPath;
			this.sdkDescriptions = sdkDescriptions;
			this.simpleParameters = simpleParameters;
			this.architecture = architecture;
			this.outDirectoryPath = outdirpath;
			this.passIdentifier = passid;
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

			NavigableMap<String, SDKReference> referencedsdks = new TreeMap<>(MSVCUtils.getSDKNameComparator());

			SakerEnvironment environment = taskcontext.getExecutionContext().getEnvironment();
			if (!ObjectUtils.isNullOrEmpty(this.libraryPath)) {
				for (LibraryPathOption libpathoption : this.libraryPath) {
					libpathoption.accept(new LibraryPathVisitor() {
						@Override
						public void visit(FileLibraryPath libpath) {
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
						public void visit(SDKPathReference libpath) {
							//XXX duplicated code with compiler worker
							String sdkname = libpath.getSDKName();
							if (ObjectUtils.isNullOrEmpty(sdkname)) {
								throw new NullPointerException("Library path returned empty sdk name: " + libpath);
							}
							SDKReference sdkref = getSDKReferenceForName(taskcontext, referencedsdks, sdkname);
							if (sdkref == null) {
								throw new IllegalArgumentException("SDK configuration not found for name: " + sdkname
										+ " required by library path: " + libpath);
							}
							try {
								SakerPath sdkdirpath = libpath.getPath(sdkref);
								if (sdkdirpath == null) {
									throw new IllegalArgumentException("No SDK library path found for: " + libpath
											+ " in SDK: " + sdkname + " as " + sdkref);
								}
								libpaths.add(LocalFileProvider.toRealPath(sdkdirpath));
							} catch (Exception e) {
								throw new IllegalArgumentException("Failed to retrieve SDK library path for: " + libpath
										+ " in SDK: " + sdkname + " as " + sdkref, e);
							}
						}
					});
				}
			}
			String hostarchitecture = environment
					.getEnvironmentPropertyCurrentValue(SystemArchitectureEnvironmentProperty.INSTANCE);

			SDKReference vcsdk = getSDKReferenceForName(taskcontext, referencedsdks, MSVCUtils.SDK_NAME_MSVC);

			SakerPath linkexepath = MSVCUtils.getVCSDKExecutablePath(vcsdk, hostarchitecture, architecture,
					MSVCUtils.VC_EXECUTABLE_NAME_LINK);
			if (linkexepath == null) {
				throw new IllegalArgumentException("SDK doesn't contain appropriate link.exe: " + vcsdk);
			}
			SakerPath workingdir = MSVCUtils.getVCSDKExecutableWorkingDirectoryPath(vcsdk, hostarchitecture,
					architecture, MSVCUtils.VC_EXECUTABLE_NAME_LINK);
			if (workingdir == null) {
				workingdir = linkexepath.getParent();
			}

			List<String> commands = new ArrayList<>();
			commands.add(linkexepath.toString());
			commands.add("/nologo");
			commands.add("/MACHINE:" + architecture);

			for (Path lpath : libpaths) {
				commands.add("/LIBPATH:" + lpath);
			}
			commands.addAll(simpleParameters);

			boolean librarylink = simpleParameters.contains("/dll");
			String extension;
			if (librarylink) {
				extension = ".dll";
			} else {
				extension = ".exe";
			}
			SakerPath outputexecpath = outDirectoryPath.resolve(passIdentifier + extension);
			Path outputmirrorpath = taskcontext.getExecutionContext().toMirrorPath(outputexecpath);
			commands.add("/OUT:" + outputmirrorpath);
			for (Path inputpath : inputfilepaths) {
				commands.add(inputpath.toString());
			}
			LocalFileProvider.getInstance().createDirectories(outputmirrorpath.getParent());

			ProcessBuilder pb = new ProcessBuilder(commands);
			pb.redirectErrorStream(true);
			pb.directory(new File(workingdir.toString()));
			MSVCUtils.removeEnvironmentVariablesFromProcess(pb);
			Process proc = MSVCUtils.startProcess(pb);

			StreamUtils.copyStream(proc.getInputStream(), ByteSink.toOutputStream(taskcontext.getStandardOut()));

			int procresult = proc.waitFor();
			if (procresult != 0) {
				throw new IOException("Failed to link: " + procresult + " (0x" + Integer.toHexString(procresult) + ")");
			}
			ProviderHolderPathKey outputpathkey = LocalFileProvider.getInstance().getPathKey(outputmirrorpath);
			taskcontext.invalidate(outputpathkey);

			SakerDirectory outdir = taskcontext.getTaskUtilities().resolveDirectoryAtPath(outDirectoryPath);

			SakerFile outputsakerfile = taskcontext.getTaskUtilities()
					.createProviderPathFile(outputexecpath.getFileName(), outputpathkey);
			outdir.add(outputsakerfile);
			outputsakerfile.synchronize();

			taskcontext.getTaskUtilities().reportInputFileDependency(LinkerFileTags.INPUT_FILE, inputdescriptors);
			taskcontext.reportOutputFileDependency(LinkerFileTags.OUTPUT_FILE, outputexecpath,
					outputsakerfile.getContentDescriptor());

			LinkerInnerTaskFactoryResult result = new LinkerInnerTaskFactoryResult(outputexecpath);
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
			out.writeObject(passIdentifier);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			environmentSelector = (TaskExecutionEnvironmentSelector) in.readObject();
			inputs = SerialUtils.readExternalImmutableLinkedHashSet(in);
			libraryPath = SerialUtils.readExternalImmutableLinkedHashSet(in);
			sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in, MSVCUtils.getSDKNameComparator());
			simpleParameters = SerialUtils.readExternalSortedImmutableNavigableSet(in,
					MSVCUtils.getLinkerParameterIgnoreCaseComparator());
			architecture = (String) in.readObject();
			outDirectoryPath = (SakerPath) in.readObject();
			passIdentifier = (String) in.readObject();
		}

		//XXX somewhat duplicated with compiler worker factory
		private SDKReference getSDKReferenceForName(TaskContext taskcontext,
				NavigableMap<String, SDKReference> referencedsdkcache, String sdkname) {
			SDKReference sdkref = referencedsdkcache.computeIfAbsent(sdkname, x -> {
				SDKDescription desc = sdkDescriptions.get(sdkname);
				if (desc == null) {
					return null;
				}
				SDKReference[] refresult = { null };
				desc.accept(new SDKDescriptionVisitor() {
					@Override
					public void visit(EnvironmentSDKDescription description) {
						EnvironmentProperty<? extends SDKReference> envproperty = SDKSupportUtils
								.getEnvironmentSDKDescriptionReferenceEnvironmentProperty(description);
						SDKReference envsdkref = taskcontext.getExecutionContext().getEnvironment()
								.getEnvironmentPropertyCurrentValue(envproperty);
						refresult[0] = envsdkref;
					}

					@Override
					public void visit(ResolvedSDKDescription description) {
						refresult[0] = description.getSDKReference();
					}

					@Override
					public void visit(UserSDKDescription description) {
						refresult[0] = UserSDKDescription.createSDKReference(description.getPaths(),
								description.getProperties());
					}
				});
				return refresult[0];
			});
			return sdkref;
		}
	}

}
