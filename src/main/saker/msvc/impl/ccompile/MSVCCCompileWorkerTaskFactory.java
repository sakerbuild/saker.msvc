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
package saker.msvc.impl.ccompile;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import saker.build.exception.FileMirroringUnavailableException;
import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;
import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.PathKey;
import saker.build.file.path.ProviderHolderPathKey;
import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.file.provider.RootFileProviderKey;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.SakerLog;
import saker.build.task.CommonTaskContentDescriptors;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.InnerTaskExecutionParameters;
import saker.build.task.InnerTaskResultHolder;
import saker.build.task.InnerTaskResults;
import saker.build.task.Task;
import saker.build.task.TaskContext;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.task.TaskExecutionUtilities;
import saker.build.task.TaskExecutionUtilities.MirroredFileContents;
import saker.build.task.TaskFactory;
import saker.build.task.TaskFileDeltas;
import saker.build.task.delta.DeltaType;
import saker.build.task.delta.FileChangeDelta;
import saker.build.task.exception.TaskEnvironmentSelectionFailedException;
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.FixedTaskDuplicationPredicate;
import saker.build.task.utils.TaskUtils;
import saker.build.thirdparty.saker.rmi.annot.transfer.RMISerialize;
import saker.build.thirdparty.saker.rmi.annot.transfer.RMIWrap;
import saker.build.thirdparty.saker.rmi.io.RMIObjectInput;
import saker.build.thirdparty.saker.rmi.io.RMIObjectOutput;
import saker.build.thirdparty.saker.rmi.io.wrap.RMIWrapper;
import saker.build.thirdparty.saker.util.ConcurrentPrependAccumulator;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.build.thirdparty.saker.util.function.LazySupplier;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.CompilerState.CompiledFileState;
import saker.msvc.impl.ccompile.CompilerState.PrecompiledHeaderState;
import saker.msvc.impl.ccompile.option.FileIncludePath;
import saker.msvc.impl.ccompile.option.IncludePathOption;
import saker.msvc.impl.ccompile.option.IncludePathVisitor;
import saker.msvc.impl.util.CollectingProcessIOConsumer;
import saker.msvc.impl.util.EnvironmentSelectionTestExecutionProperty;
import saker.msvc.impl.util.InnerTaskMirrorHandler;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.exc.SDKManagementException;
import saker.sdk.support.api.exc.SDKNotFoundException;
import saker.sdk.support.api.exc.SDKPathNotFoundException;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import testing.saker.msvc.TestFlag;

public class MSVCCCompileWorkerTaskFactory implements TaskFactory<Object>, Task<Object>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	private static final String PRECOMPILED_HEADERS_SUBDIRECTORY_NAME = "pch";

	public static final Set<String> ALWAYS_PRESENT_CL_PARAMETERS = ImmutableUtils
			.makeImmutableNavigableSet(new String[] {
//					Suppress logo
					"/nologo",
//					Only compile
					"/c",
//					Write includes to standard error or standard output
//					based on experience, the includes may be written to the standard output
//					instead of standard error, although the docs says they would be written to std err
//					
//					https://docs.microsoft.com/en-us/cpp/build/reference/showincludes-list-include-files?view=vs-2019
//					    > The /showIncludes option emits to stderr, not stdout.
//					
//					However, this is false for some cases. This is verified as well by plainly invoking it from cmd.
//					Others also have this experience: https://github.com/fastbuild/fastbuild/issues/24
//					See also: https://developercommunity.visualstudio.com/content/problem/902524/clexe-with-showincludes-writes-to-stdout-instead-o.html
					"/showIncludes",
//					Prevents the compiler from searching for include files 
//					in directories specified in the PATH and INCLUDE environment variables.
					"/X",

			});

	private static final Pattern CL_OUTPUT_ERROR_PATTERN = Pattern
			.compile("(.+?)\\(([0-9]+)\\) ?: ?([a-zA-Z ]+)( C[0-9]+)?: (.+)");
	private static final int CL_OUTPUT_ERROR_GROUP_FILE = 1;
	private static final int CL_OUTPUT_ERROR_GROUP_LINENUM = 2;
	private static final int CL_OUTPUT_ERROR_GROUP_TYPE = 3;
	/**
	 * optional
	 */
	private static final int CL_OUTPUT_ERROR_GROUP_CLERROR = 4;
	private static final int CL_OUTPUT_ERROR_GROUP_DESC = 5;

	private Set<FileCompilationConfiguration> files;
	private NavigableMap<String, SDKDescription> sdkDescriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCCompileWorkerTaskFactory() {
	}

	public void setFiles(Set<FileCompilationConfiguration> files) {
		this.files = files;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkdescriptions) {
		ObjectUtils.requireComparator(sdkdescriptions, SDKSupportUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkdescriptions;
		if (!sdkdescriptions.containsKey(MSVCUtils.SDK_NAME_MSVC)) {
			throw new SDKNotFoundException("MSVC SDK unspecified for compilation.");
		}
	}

	private static void collectFileDeltaPaths(Collection<? extends FileChangeDelta> deltas,
			Collection<SakerPath> result) {
		if (ObjectUtils.isNullOrEmpty(deltas)) {
			return;
		}
		for (FileChangeDelta delta : deltas) {
			result.add(delta.getFilePath());
		}
	}

	private static class DeltaDetectedException extends RuntimeException {
		private static final long serialVersionUID = 1L;

		public static final DeltaDetectedException INSTANCE = new DeltaDetectedException();

		public DeltaDetectedException() {
			super(null, null, false, false);
		}
	}

	@Override
	public Object run(TaskContext taskcontext) throws Exception {
		TaskIdentifier taskid = taskcontext.getTaskId();
		if (!(taskid instanceof MSVCCCompileWorkerTaskIdentifier)) {
			taskcontext.abortExecution(
					new IllegalStateException("Invalid task identifier for: " + this.getClass().getName()));
			return null;
		}
		MSVCCCompileWorkerTaskIdentifier workertaskid = (MSVCCCompileWorkerTaskIdentifier) taskid;
		CompilationIdentifier passidentifier = workertaskid.getPassIdentifier();
		String passid = passidentifier.toString();
		String architecture = workertaskid.getArchitecture();
		taskcontext
				.setStandardOutDisplayIdentifier(MSVCCCompileTaskFactory.TASK_NAME + ":" + passid + "/" + architecture);

		SakerDirectory outdir = SakerPathFiles.requireBuildDirectory(taskcontext)
				.getDirectoryCreate(MSVCCCompileTaskFactory.TASK_NAME).getDirectoryCreate(passid)
				.getDirectoryCreate(architecture);

		SakerPath outdirpath = outdir.getSakerPath();

		List<FileCompilationConfiguration> compilationentries = new ArrayList<>(this.files);

		NavigableMap<String, CompiledFileState> stateexecutioncompiledfiles = new TreeMap<>();

		TaskExecutionEnvironmentSelector envselector = SDKSupportUtils
				.getSDKBasedClusterExecutionEnvironmentSelector(sdkDescriptions.values());
		NavigableMap<String, SDKDescription> compilerinnertasksdkdescriptions = sdkDescriptions;
		EnvironmentSelectionResult envselectionresult;
		if (envselector != null) {
			try {
				envselectionresult = taskcontext.getTaskUtilities()
						.getReportExecutionDependency(new EnvironmentSelectionTestExecutionProperty(envselector));
			} catch (Exception e) {
				throw new TaskEnvironmentSelectionFailedException(
						"Failed to select a suitable build environment for compilation.", e);
			}
			compilerinnertasksdkdescriptions = SDKSupportUtils.pinSDKSelection(envselectionresult, sdkDescriptions);
			envselector = SDKSupportUtils
					.getSDKBasedClusterExecutionEnvironmentSelector(compilerinnertasksdkdescriptions.values());
		} else {
			envselectionresult = null;
		}

		CompilerState prevoutput = taskcontext.getPreviousTaskOutput(CompilerState.class, CompilerState.class);
		Map<RootFileProviderKey, NavigableMap<SakerPath, PrecompiledHeaderState>> nprecompiledheaders = new ConcurrentHashMap<>();

		CompilerState nstate = new CompilerState();
		nstate.setSdkDescriptions(sdkDescriptions);
		nstate.setEnvironmentSelection(envselectionresult);
		nstate.setPrecompiledHeaders(nprecompiledheaders);

		if (prevoutput != null) {
			for (Entry<RootFileProviderKey, NavigableMap<SakerPath, PrecompiledHeaderState>> entry : prevoutput
					.getPrecompiledHeaders().entrySet()) {
				nprecompiledheaders.put(entry.getKey(), new ConcurrentSkipListMap<>(entry.getValue()));
			}
			filterUnchangedPreviousFiles(taskcontext, compilationentries, stateexecutioncompiledfiles, prevoutput,
					nstate);
		}

		if (!compilationentries.isEmpty()) {
			int sccount = compilationentries.size();
			System.out.println("Compiling " + sccount + " source file" + (sccount == 1 ? "" : "s") + ".");
			ConcurrentPrependAccumulator<FileCompilationConfiguration> fileaccumulator = new ConcurrentPrependAccumulator<>(
					compilationentries);
			InnerTaskExecutionParameters innertaskparams = new InnerTaskExecutionParameters();
			innertaskparams.setClusterDuplicateFactor(compilationentries.size());
			innertaskparams.setDuplicationPredicate(new FixedTaskDuplicationPredicate(compilationentries.size()));

			//TODO print the process output and the diagnostics in a locked way
			WorkerTaskCoordinator coordinator = new WorkerTaskCoordinator() {
				@Override
				public void headerPrecompiled(CompilerInnerTaskResult result, PathKey outputpathkey,
						ContentDescriptor outputcontents) {
					CompilationDependencyInfo depinfo = result.getDependencyInfo();
					try {
						taskcontext.getStandardOut().write(depinfo.getProcessOutput());
					} catch (NullPointerException | IOException e) {
						taskcontext.getTaskUtilities().reportIgnoredException(e);
					}
					SakerPath[] sourcefilepath = { null };
					result.compilationEntry.getProperties().fileLocation.accept(new FileLocationVisitor() {
						@Override
						public void visit(ExecutionFileLocation loc) {
							sourcefilepath[0] = loc.getPath();
						}
						//TODO local file location
					});
					printDiagnostics(taskcontext, sourcefilepath[0], depinfo.getDiagnostics());
					nprecompiledheaders
							.computeIfAbsent(outputpathkey.getFileProviderKey(),
									Functionals.concurrentSkipListMapComputer())
							.put(outputpathkey.getPath(),
									new PrecompiledHeaderState(depinfo.getInputContents(), outputcontents,
											result.getCompilationEntry().getProperties(), depinfo.getIncludes(),
											depinfo.getDiagnostics()));
				}

				@Override
				public NavigableMap<SakerPath, PrecompiledHeaderState> getPrecompiledHeaderStates(
						RootFileProviderKey fpk) {
					return nprecompiledheaders.get(fpk);
				}
			};
			SourceCompilerInnerTaskFactory innertask = new SourceCompilerInnerTaskFactory(coordinator,
					fileaccumulator::take, outdirpath, architecture, compilerinnertasksdkdescriptions, envselector,
					outdir);
			InnerTaskResults<CompilerInnerTaskResult> innertaskresults = taskcontext.startInnerTask(innertask,
					innertaskparams);
			InnerTaskResultHolder<CompilerInnerTaskResult> resultholder;

			while ((resultholder = innertaskresults.getNext()) != null) {
				CompilerInnerTaskResult compilationresult = resultholder.getResult();
				if (compilationresult == null) {
					//may be if the inner task doesn't receive a compilation entry as there are no more
					//and returns prematurely
					continue;
				}
				FileCompilationConfiguration compilationentry = compilationresult.getCompilationEntry();
				CompilationDependencyInfo depinfo = compilationresult.getDependencyInfo();
				taskcontext.getStandardOut().write(depinfo.getProcessOutput());
				compilationentry.getProperties().getFileLocation().accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						CompiledFileState compiledfilestate = new CompiledFileState(depinfo.getInputContents(),
								compilationentry);
						compiledfilestate.setDiagnostics(depinfo.getDiagnostics());
						compiledfilestate.setIncludes(depinfo.getIncludes());
						compiledfilestate.setFailedIncludes(depinfo.getFailedIncludes());
						if (compilationresult.isSuccessful()) {
							String outputobjectfilename = compilationresult.getOutputObjectName();
							if (outputobjectfilename != null) {
								SakerPath outputpath = outdirpath.resolve(outputobjectfilename);
								SakerFile outfile = taskcontext.getTaskUtilities().resolveFileAtPath(outputpath);
								if (outfile == null) {
									throw ObjectUtils.sneakyThrow(new FileNotFoundException(
											"Output object file was not found: " + outdirpath));
								}
								ContentDescriptor outcontentdescriptor = outfile.getContentDescriptor();

								compiledfilestate.setObjectOutputContents(outputpath, outcontentdescriptor);
							}
						}
						printDiagnostics(taskcontext, loc.getPath(), compiledfilestate);
						stateexecutioncompiledfiles.put(compilationentry.getOutFileName(), compiledfilestate);
					}

					@Override
					public void visit(LocalFileLocation loc) {
						// TODO handle local input file result
						FileLocationVisitor.super.visit(loc);
					}
				});
			}
		}

		nstate.setExecutionCompiledFiles(stateexecutioncompiledfiles);

		NavigableSet<SakerPath> allreferencedincludes = nstate.getAllReferencedIncludes();
		NavigableSet<SakerPath> allfailedincludes = nstate.getAllReferencedFailedIncludes();
		NavigableMap<SakerPath, ContentDescriptor> includecontentdescriptors = new TreeMap<>();
		NavigableSet<String> includedfilenames = new TreeSet<>();
		for (SakerPath includepath : allreferencedincludes) {
			includedfilenames.add(includepath.getFileName());
			//XXX use a more efficient resolveFileAtPath algorithm
			SakerFile includefile = taskcontext.getTaskUtilities().resolveFileAtPath(includepath);
			if (includefile == null) {
				SakerLog.error().verbose().println("Included file no longer found: " + includepath);
				//report an IS_FILE dependency nonetheless, as that will trigger the reinvocation of the
				//compilation the next time.
				//this scenario should not happen at all generally.
				includecontentdescriptors.put(includepath, CommonTaskContentDescriptors.IS_FILE);
				continue;
			}
			includecontentdescriptors.put(includepath, includefile.getContentDescriptor());
		}
		for (SakerPath includepath : allfailedincludes) {
			ContentDescriptor prev = includecontentdescriptors.putIfAbsent(includepath,
					CommonTaskContentDescriptors.IS_NOT_FILE);
			if (prev != null) {
				SakerLog.error().verbose().println("Header referencing concurrency error. Referenced header: "
						+ includepath
						+ " had transient presence for compiled sources. It is recommended to clean the project.");
			}
		}

		NavigableSet<SakerPath> compiledfileparentdirectorypaths = new TreeSet<>();
		NavigableMap<SakerPath, ContentDescriptor> inputexecutionfilecontents = new TreeMap<>();
		for (CompiledFileState filestate : stateexecutioncompiledfiles.values()) {
			FileCompilationConfiguration compilationconfig = filestate.getCompilationConfiguration();
			compilationconfig.getProperties().getFileLocation().accept(new FileLocationVisitor() {

				@Override
				public void visit(ExecutionFileLocation loc) {
					compiledfileparentdirectorypaths.add(loc.getPath().getParent());
					inputexecutionfilecontents.put(loc.getPath(), filestate.getInputContents());
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO report dependencies for local input files
					FileLocationVisitor.super.visit(loc);
				}
			});

		}
		for (SakerPath includedirpath : compiledfileparentdirectorypaths) {
			reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(taskcontext, includecontentdescriptors,
					includedfilenames, includedirpath);
		}
		for (CompiledFileState filestate : stateexecutioncompiledfiles.values()) {
			FileCompilationConfiguration compilationconfig = filestate.getCompilationConfiguration();
			Collection<IncludePathOption> includedirs = compilationconfig.getProperties().getIncludeDirectories();
			if (!ObjectUtils.isNullOrEmpty(includedirs)) {
				for (IncludePathOption includediroption : includedirs) {
					includediroption.accept(new IncludePathVisitor() {
						@Override
						public void visit(FileIncludePath includedir) {
							includedir.getFileLocation().accept(new FileLocationVisitor() {
								@Override
								public void visit(ExecutionFileLocation loc) {
									SakerPath includedirexecutionpath = loc.getPath();
									if (compiledfileparentdirectorypaths.contains(includedirexecutionpath)) {
										//already reported
										return;
									}
									reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(taskcontext,
											includecontentdescriptors, includedfilenames, includedirexecutionpath);
								}

								@Override
								public void visit(LocalFileLocation loc) {
									// TODO report dependencies for local include directories
									FileLocationVisitor.super.visit(loc);
								}
							});
						}

						@Override
						public void visit(SDKPathReference includedir) {
							//ignore dependency wise
						}
					});
				}
			}
		}
		taskcontext.getTaskUtilities().reportInputFileDependency(CompilationFileTags.INCLUDE_FILE,
				includecontentdescriptors);

		taskcontext.getTaskUtilities().reportInputFileDependency(CompilationFileTags.SOURCE,
				inputexecutionfilecontents);
		taskcontext.getTaskUtilities().reportOutputFileDependency(CompilationFileTags.OBJECT_FILE,
				nstate.getOutputObjectFileContentDescriptors());
		taskcontext.setTaskOutput(CompilerState.class, nstate);

		//remove files which are not part of the output object files
		ObjectUtils.iterateOrderedIterables(outdir.getChildren().entrySet(), nstate.getAllOutputFileNames(),
				(entry, name) -> entry.getKey().compareTo(name), (entry, outf) -> {
					if (outf == null) {
						entry.getValue().remove();
					}
				});
		//use the nothing predicate to only delete the files which were removed
		outdir.synchronize(new DirectoryVisitPredicate() {
			@Override
			public DirectoryVisitPredicate directoryVisitor(String arg0, SakerDirectory arg1) {
				return null;
			}

			@Override
			public boolean visitFile(String name, SakerFile file) {
				return false;
			}

			@Override
			public boolean visitDirectory(String name, SakerDirectory directory) {
				return false;
			}

			@Override
			public NavigableSet<String> getSynchronizeFilesToKeep() {
				//don't remove the pch subdir
				return ImmutableUtils.singletonNavigableSet(PRECOMPILED_HEADERS_SUBDIRECTORY_NAME);
			}
		});

		if (!nstate.isAllCompilationSucceeded()) {
			taskcontext.abortExecution(new IOException("Compilation failed."));
			return null;
		}

		MSVCCompilerWorkerTaskOutputImpl result = new MSVCCompilerWorkerTaskOutputImpl(passidentifier, architecture,
				sdkDescriptions);
		result.setObjectFilePaths(nstate.getOutputObjectFilePaths());
		return result;
	}

	private static void reportAdditionDontCareDependenciesForFileNamesIncludeDirectory(TaskContext taskcontext,
			NavigableMap<SakerPath, ContentDescriptor> includecontentdescriptors,
			NavigableSet<String> includedfilenames, SakerPath includedirexecutionpath) {
		if (ObjectUtils.isNullOrEmpty(includedfilenames)) {
			//don't need to report anything
			return;
		}
		FileNameRecursiveFileCollectionStrategy collectionstrategy = new FileNameRecursiveFileCollectionStrategy(
				includedirexecutionpath, includedfilenames);
		NavigableMap<SakerPath, SakerFile> foundsimilarfiles = taskcontext.getTaskUtilities()
				.collectFilesReportAdditionDependency(CompilationFileTags.INCLUDE_FILE, collectionstrategy);
		for (SakerPath similarfilepath : foundsimilarfiles.keySet()) {
			includecontentdescriptors.putIfAbsent(similarfilepath, CommonTaskContentDescriptors.DONT_CARE);
		}
	}

	private static void filterUnchangedPreviousFiles(TaskContext taskcontext,
			List<FileCompilationConfiguration> compilationentries,
			NavigableMap<String, CompiledFileState> stateexecutioncompiledfiles, CompilerState prevoutput,
			CompilerState nstate) {
		//XXX sorted iteration for equals?
		if (!Objects.equals(nstate.getSdkDescriptions(), prevoutput.getSdkDescriptions())) {
			//different toolchains used, recompile all
			return;
		}
		if (!Objects.equals(nstate.getEnvironmentSelection(), prevoutput.getEnvironmentSelection())) {
			//different environments are used to compile the sources.
			//recompile all
			return;
		}

		TaskUtils.collectFilesForTags(taskcontext.getFileDeltas(),
				ImmutableUtils.asUnmodifiableArrayList(CompilationFileTags.SOURCE));
		TaskFileDeltas inputfilechanges = taskcontext.getFileDeltas(DeltaType.INPUT_FILE_CHANGE);
		TaskFileDeltas outputfilechanges = taskcontext.getFileDeltas(DeltaType.OUTPUT_FILE_CHANGE);

		TaskFileDeltas inputfileadditions = taskcontext.getFileDeltas(DeltaType.INPUT_FILE_ADDITION);

		NavigableSet<SakerPath> relevantchanges = new TreeSet<>();
		collectFileDeltaPaths(inputfilechanges.getFileDeltasWithTag(CompilationFileTags.SOURCE), relevantchanges);
		collectFileDeltaPaths(outputfilechanges.getFileDeltasWithTag(CompilationFileTags.OBJECT_FILE), relevantchanges);

		NavigableSet<SakerPath> includechanges = new TreeSet<>();
		collectFileDeltaPaths(inputfilechanges.getFileDeltasWithTag(CompilationFileTags.INCLUDE_FILE), includechanges);

		//compare using ignore-case, as if an include file was not found, we should trigger the recompilation
		//if a file with different casing is added
		NavigableSet<String> includeadditionfilenames = new TreeSet<>(String::compareToIgnoreCase);
		for (FileChangeDelta adddelta : inputfileadditions.getFileDeltas()) {
			includeadditionfilenames.add(adddelta.getFilePath().getFileName());
		}

		NavigableMap<String, CompiledFileState> prevcompiledfiles = new TreeMap<>(
				prevoutput.getExecutionCompiledFiles());
		for (Iterator<FileCompilationConfiguration> it = compilationentries.iterator(); it.hasNext();) {
			FileCompilationConfiguration compilationentry = it.next();
			String outfilename = compilationentry.getOutFileName();
			CompiledFileState prevfilestate = prevcompiledfiles.remove(outfilename);
			if (prevfilestate == null) {
				//wasn't compiled previously, compile now
				continue;
			}
			if (!Objects.equals(prevfilestate.getCompilationConfiguration(), compilationentry)) {
				//the configuration for the compiled file changed
				continue;
			}
			SakerPath outobjpath = prevfilestate.getOutputObjectPath();
			if (outobjpath != null) {
				if (relevantchanges.contains(outobjpath)) {
					continue;
				}
			}

			if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames, prevfilestate.getIncludes())) {
				continue;
			}
			if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames,
					prevfilestate.getFailedIncludes())) {
				continue;
			}

			compilationentry.getProperties().getFileLocation().accept(new FileLocationVisitor() {
				@Override
				public void visit(ExecutionFileLocation loc) {
					SakerPath sourcefilepath = loc.getPath();
					if (relevantchanges.contains(sourcefilepath)) {
						return;
					}

					//no changes found, remove the file from the to-compile collection

					printDiagnostics(taskcontext, sourcefilepath, prevfilestate);
					stateexecutioncompiledfiles.put(outfilename, prevfilestate);

					it.remove();
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO check changes in local input file
					FileLocationVisitor.super.visit(loc);
				}
			});
		}

		//check any deltas for the precompiled headers
		for (NavigableMap<SakerPath, PrecompiledHeaderState> pchs : nstate.getPrecompiledHeaders().values()) {
			for (Iterator<PrecompiledHeaderState> it = pchs.values().iterator(); it.hasNext();) {
				PrecompiledHeaderState pchstate = it.next();
				NavigableSet<SakerPath> pchincludes = pchstate.getIncludes();
				if (isAnyIncludeRelatedChange(includechanges, includeadditionfilenames, pchincludes)) {
					it.remove();
					continue;
				}
			}
		}
	}

	private static boolean isAnyIncludeRelatedChange(NavigableSet<SakerPath> includechanges,
			NavigableSet<String> includeadditionfilenames, NavigableSet<SakerPath> prevstateincludes) {
		if (ObjectUtils.isNullOrEmpty(prevstateincludes)) {
			return false;
		}
		if (!includechanges.isEmpty()) {
			try {
				NavigableSet<SakerPath> prevstateincludessubset = prevstateincludes.subSet(includechanges.first(), true,
						includechanges.last(), true);
				ObjectUtils.iterateOrderedIterables(prevstateincludessubset, includechanges, (l, r) -> {
					if (l != null && r != null) {
						throw DeltaDetectedException.INSTANCE;
					}
				});
			} catch (DeltaDetectedException e) {
				//changes found in the included files
				return true;
			}
		}
		if (!includeadditionfilenames.isEmpty()) {
			for (SakerPath include : prevstateincludes) {
				if (includeadditionfilenames.contains(include.getFileName())) {
					//possible include resolution change
					return true;
				}
			}
		}
		return false;
	}

	protected static SakerPath getPrecompiledHeaderOutputDirectoryPath(SakerPath outputdirpath) {
		return outputdirpath.resolve(PRECOMPILED_HEADERS_SUBDIRECTORY_NAME);
	}

	@Override
	public Set<String> getCapabilities() {
		return WORKER_TASK_CAPABILITIES;
	}

	@Override
	public Task<? extends Object> createTask(ExecutionContext executioncontext) {
		return this;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, files);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		files = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((files == null) ? 0 : files.hashCode());
		result = prime * result + ((sdkDescriptions == null) ? 0 : sdkDescriptions.hashCode());
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
		MSVCCCompileWorkerTaskFactory other = (MSVCCCompileWorkerTaskFactory) obj;
		if (files == null) {
			if (other.files != null)
				return false;
		} else if (!files.equals(other.files))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		return true;
	}

	private static class PrecompiledHeaderDependencyInfo {
		protected NavigableSet<SakerPath> includes;

		public PrecompiledHeaderDependencyInfo() {
			this.includes = new TreeSet<>();
		}

		public PrecompiledHeaderDependencyInfo(NavigableSet<SakerPath> includes) {
			this.includes = includes;
		}

		public PrecompiledHeaderDependencyInfo(CompilationDependencyInfo depinfo) {
			this.includes = depinfo.includes;
		}
	}

	private static class CompilationDependencyInfo implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected ContentDescriptor inputContents;
		protected NavigableSet<CompilerDiagnostic> diagnostics = new TreeSet<>();
		protected NavigableSet<SakerPath> includes = new TreeSet<>();
		protected NavigableSet<SakerPath> failedIncludes = new TreeSet<>();
		//XXX this should not be here but only for compiled source files. no need for pch
		protected ByteArrayRegion processOutput = ByteArrayRegion.EMPTY;

		/**
		 * For {@link Externalizable}.
		 */
		public CompilationDependencyInfo() {
		}

		public CompilationDependencyInfo(ContentDescriptor inputContents) {
			this.inputContents = inputContents;
		}

		public ContentDescriptor getInputContents() {
			return inputContents;
		}

		public NavigableSet<CompilerDiagnostic> getDiagnostics() {
			return diagnostics;
		}

		public NavigableSet<SakerPath> getIncludes() {
			return includes;
		}

		public NavigableSet<SakerPath> getFailedIncludes() {
			return failedIncludes;
		}

		public ByteArrayRegion getProcessOutput() {
			return processOutput;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(inputContents);
			SerialUtils.writeExternalCollection(out, diagnostics);
			SerialUtils.writeExternalCollection(out, includes);
			SerialUtils.writeExternalCollection(out, failedIncludes);
			out.writeObject(processOutput);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			inputContents = (ContentDescriptor) in.readObject();
			diagnostics = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			includes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			failedIncludes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			processOutput = (ByteArrayRegion) in.readObject();
		}
	}

	public static class CompilerInnerTaskResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected FileCompilationConfiguration compilationEntry;
		protected boolean successful;
		protected String outputObjectName;

		protected CompilationDependencyInfo dependencyInfo;

		/**
		 * For {@link Externalizable}.
		 */
		public CompilerInnerTaskResult() {
		}

		public CompilerInnerTaskResult(FileCompilationConfiguration compilationEntry) {
			this.compilationEntry = compilationEntry;
		}

		public static CompilerInnerTaskResult successful(FileCompilationConfiguration compilationEntry) {
			CompilerInnerTaskResult result = new CompilerInnerTaskResult(compilationEntry);
			result.successful = true;
			return result;
		}

		public static CompilerInnerTaskResult failed(FileCompilationConfiguration compilationEntry) {
			CompilerInnerTaskResult result = new CompilerInnerTaskResult(compilationEntry);
			result.successful = false;
			return result;
		}

		public boolean isSuccessful() {
			return successful;
		}

		public FileCompilationConfiguration getCompilationEntry() {
			return compilationEntry;
		}

		public String getOutputObjectName() {
			return outputObjectName;
		}

		public CompilationDependencyInfo getDependencyInfo() {
			return dependencyInfo;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(compilationEntry);
			out.writeBoolean(successful);
			out.writeObject(outputObjectName);
			out.writeObject(dependencyInfo);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			compilationEntry = (FileCompilationConfiguration) in.readObject();
			successful = in.readBoolean();
			outputObjectName = (String) in.readObject();
			dependencyInfo = (CompilationDependencyInfo) in.readObject();
		}
	}

	protected final static class SourceCompilerRMIWrapper implements RMIWrapper {
		private SourceCompilerInnerTaskFactory task;

		public SourceCompilerRMIWrapper() {
		}

		public SourceCompilerRMIWrapper(SourceCompilerInnerTaskFactory task) {
			this.task = task;
		}

		@Override
		public void writeWrapped(RMIObjectOutput out) throws IOException {
			out.writeRemoteObject(task.coordinator);
			out.writeRemoteObject(task.fileLocationSuppier);
			out.writeObject(task.outputDirPath);
			out.writeObject(task.architecture);
			out.writeSerializedObject(task.sdkDescriptions);
			out.writeSerializedObject(task.environmentSelector);
			out.writeRemoteObject(task.outputDir);
		}

		@SuppressWarnings("unchecked")
		@Override
		public void readWrapped(RMIObjectInput in) throws IOException, ClassNotFoundException {
			task = new SourceCompilerInnerTaskFactory();
			task.coordinator = (WorkerTaskCoordinator) in.readObject();
			task.fileLocationSuppier = (Supplier<FileCompilationConfiguration>) in.readObject();
			task.outputDirPath = (SakerPath) in.readObject();
			task.architecture = (String) in.readObject();
			task.sdkDescriptions = (NavigableMap<String, SDKDescription>) in.readObject();
			task.environmentSelector = (TaskExecutionEnvironmentSelector) in.readObject();
			task.outputDir = (SakerDirectory) in.readObject();
		}

		@Override
		public Object resolveWrapped() {
			return task;
		}

		@Override
		public Object getWrappedObject() {
			throw new UnsupportedOperationException();
		}
	}

	public interface WorkerTaskCoordinator {
		public void headerPrecompiled(@RMISerialize CompilerInnerTaskResult result, PathKey outputpathkey,
				@RMISerialize ContentDescriptor outputcontents);

		@RMISerialize
		public NavigableMap<SakerPath, PrecompiledHeaderState> getPrecompiledHeaderStates(
				@RMISerialize RootFileProviderKey fpk);
	}

	@RMIWrap(SourceCompilerRMIWrapper.class)
	private static class SourceCompilerInnerTaskFactory
			implements TaskFactory<CompilerInnerTaskResult>, Task<CompilerInnerTaskResult> {
		protected WorkerTaskCoordinator coordinator;
		protected Supplier<FileCompilationConfiguration> fileLocationSuppier;
		protected SakerPath outputDirPath;
		protected String architecture;
		protected NavigableMap<String, SDKDescription> sdkDescriptions;
		protected TaskExecutionEnvironmentSelector environmentSelector;
		protected SakerDirectory outputDir;

		private transient InnerTaskMirrorHandler mirrorHandler = new InnerTaskMirrorHandler();
		private transient NavigableMap<String, Supplier<SDKReference>> referencedSDKCache = new ConcurrentSkipListMap<>(
				SDKSupportUtils.getSDKNameComparator());
		private transient NavigableMap<String, Object> sdkCacheLocks = new ConcurrentSkipListMap<>(
				SDKSupportUtils.getSDKNameComparator());

		private transient ConcurrentHashMap<FileCompilationConfiguration, Object> precompiledHeaderCreationLocks = new ConcurrentHashMap<>();
		private transient ConcurrentHashMap<FileCompilationConfiguration, Optional<PrecompiledHeaderDependencyInfo>> precompiledHeaderCreationResults = new ConcurrentHashMap<>();

		private transient final Supplier<NavigableMap<SakerPath, PrecompiledHeaderState>> precompiledHeaderStatesLazySupplier = LazySupplier
				.of(() -> {
					return coordinator.getPrecompiledHeaderStates(LocalFileProvider.getProviderKeyStatic());
				});

		/**
		 * For RMI transfer.
		 */
		public SourceCompilerInnerTaskFactory() {
		}

		public SourceCompilerInnerTaskFactory(WorkerTaskCoordinator coordinator,
				Supplier<FileCompilationConfiguration> fileLocationSuppier, SakerPath outputDirPath,
				String architecture, NavigableMap<String, SDKDescription> sdkDescriptions,
				TaskExecutionEnvironmentSelector envselector, SakerDirectory outputDir) {
			this.coordinator = coordinator;
			this.fileLocationSuppier = fileLocationSuppier;
			this.outputDirPath = outputDirPath;
			this.architecture = architecture;
			this.sdkDescriptions = sdkDescriptions;
			this.environmentSelector = envselector;
			this.outputDir = outputDir;
		}

		@Override
		public Task<? extends CompilerInnerTaskResult> createTask(ExecutionContext executioncontext) {
			return this;
		}

		@Override
		public int getRequestedComputationTokenCount() {
			return 1;
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

		private Path getCompileFilePath(FileCompilationProperties compilationentry, SakerEnvironment environment,
				TaskExecutionUtilities taskutilities, ContentDescriptor[] contents) {
			Path[] compilefilepath = { null };
			compilationentry.getFileLocation().accept(new FileLocationVisitor() {
				@Override
				public void visit(ExecutionFileLocation loc) {
					SakerPath path = loc.getPath();
					if (TestFlag.ENABLED) {
						TestFlag.metric().compiling(path, environment);
					}
					try {
						MirroredFileContents mirrorres = mirrorHandler.mirrorFile(taskutilities, path);
						compilefilepath[0] = mirrorres.getPath();
						contents[0] = mirrorres.getContents();
					} catch (FileMirroringUnavailableException | IOException e) {
						throw ObjectUtils.sneakyThrow(e);
					}
				}

				@Override
				public void visit(LocalFileLocation loc) {
					// TODO handle local input file
					FileLocationVisitor.super.visit(loc);
				}
			});
			return compilefilepath[0];
		}

		private static boolean isPrecompiledHeaderUpToDate(TaskContext taskcontext, PrecompiledHeaderState prevstate,
				ContentDescriptor currentcontents, Path outputpath, FileCompilationProperties entrypch) {
			if (prevstate == null) {
				return false;
			}
			if (!prevstate.getInputContents().equals(currentcontents)) {
				//the input header changed
				return false;
			}
			//the output was changed
			if (!prevstate.getOutputContents().equals(taskcontext.getExecutionContext()
					.getContentDescriptor(LocalFileProvider.getInstance().getPathKey(outputpath)))) {
				return false;
			}
			if (!entrypch.equals(prevstate.getCompilationProperties())) {
				return false;
			}
			return true;
		}

		@Override
		public CompilerInnerTaskResult run(TaskContext taskcontext) throws Exception {
			FileCompilationConfiguration compilationentry = fileLocationSuppier.get();
			if (compilationentry == null) {
				return null;
			}
			FileCompilationProperties compilationentryproperties = compilationentry.getProperties();
			SakerPath outputdirpath = outputDirPath;
			ContentDescriptor[] contents = { null };
			TaskExecutionUtilities taskutilities = taskcontext.getTaskUtilities();
			ExecutionContext executioncontext = taskcontext.getExecutionContext();
			SakerEnvironment environment = executioncontext.getEnvironment();
			Path compilefilepath = getCompileFilePath(compilationentryproperties, environment, taskutilities, contents);

			List<Path> includedirpaths = getIncludePaths(taskutilities, environment,
					compilationentryproperties.getIncludeDirectories(), true);
			List<Path> forceincludepaths = getIncludePaths(taskutilities, environment,
					compilationentryproperties.getForceInclude(), false);
			boolean forceincludepch = compilationentry.isPrecompiledHeaderForceInclude();

			FileCompilationConfiguration compilationconfiguration = compilationentry;
			String outputfilenamebase = compilationconfiguration.getOutFileName();
			String outputobjectfilename = outputfilenamebase + ".obj";

			Path objoutpath = executioncontext.toMirrorPath(outputdirpath.resolve(outputobjectfilename));

			//create the parent directory, else the process will throw
			LocalFileProvider localfp = LocalFileProvider.getInstance();
			localfp.createDirectories(objoutpath.getParent());

			String hostarchitecture = environment
					.getEnvironmentPropertyCurrentValue(SystemArchitectureEnvironmentProperty.INSTANCE);

			SDKReference vcsdk = getSDKReferenceForName(environment, MSVCUtils.SDK_NAME_MSVC);

			SakerPath clexepath = MSVCUtils.getVCSDKExecutablePath(vcsdk, hostarchitecture, this.architecture,
					MSVCUtils.VC_EXECUTABLE_NAME_CL);
			if (clexepath == null) {
				throw new SDKPathNotFoundException("SDK doesn't contain appropriate cl.exe: " + vcsdk);
			}
			SakerPath workingdir = MSVCUtils.getVCSDKExecutableWorkingDirectoryPath(vcsdk, hostarchitecture,
					this.architecture, MSVCUtils.VC_EXECUTABLE_NAME_CL);
			if (workingdir == null) {
				workingdir = clexepath.getParent();
			}

			String pchoutfilename = compilationentry.getPrecompiledHeaderOutFileName();
			Path pchoutpath = null;
			String pchname = null;
			PrecompiledHeaderDependencyInfo pchdepinfo = null;
			if (pchoutfilename != null) {
				FileCompilationProperties pchproperties = compilationentryproperties
						.withFileLocation(compilationconfiguration.getPrecompiledHeaderFileLocation());
				SakerPath pchoutdir = getPrecompiledHeaderOutputDirectoryPath(outputdirpath);
				pchname = MSVCUtils.getFileName(pchproperties.getFileLocation());
				pchoutpath = executioncontext.toMirrorPath(pchoutdir.resolve(pchoutfilename + ".pch"));

				localfp.createDirectories(pchoutpath.getParent());

				Path pchobjpath = executioncontext.toMirrorPath(pchoutdir.resolve(pchoutfilename + ".obj"));
				FileCompilationConfiguration entrypch = new FileCompilationConfiguration(pchoutfilename, pchproperties);
				Optional<PrecompiledHeaderDependencyInfo> headerres = precompiledHeaderCreationResults.get(entrypch);
				if (headerres == null) {
					synchronized (precompiledHeaderCreationLocks.computeIfAbsent(entrypch,
							Functionals.objectComputer())) {
						headerres = precompiledHeaderCreationResults.get(entrypch);
						if (headerres == null) {
							ContentDescriptor[] pchcontents = { null };
							Path pchcompilefilepath = getCompileFilePath(pchproperties, environment, taskutilities,
									pchcontents);

							NavigableMap<SakerPath, PrecompiledHeaderState> precompiledheaderstates = precompiledHeaderStatesLazySupplier
									.get();
							SakerPath pchcompilesakerfilepath = SakerPath.valueOf(pchcompilefilepath);
							PrecompiledHeaderState prevheaderstate = ObjectUtils.getMapValue(precompiledheaderstates,
									pchcompilesakerfilepath);
							if (isPrecompiledHeaderUpToDate(taskcontext, prevheaderstate, pchcontents[0],
									pchcompilefilepath, pchproperties)) {
								headerres = Optional
										.of(new PrecompiledHeaderDependencyInfo(prevheaderstate.getIncludes()));
							} else {
								List<String> commands = new ArrayList<>();
								commands.add(clexepath.toString());
								commands.addAll(ALWAYS_PRESENT_CL_PARAMETERS);
								commands.addAll(pchproperties.getSimpleParameters());
								commands.add(
										getLanguageCommandLineOption(pchproperties.getLanguage()) + pchcompilefilepath);
								commands.add("/Fo" + pchobjpath);
								addIncludeDirectoryCommands(commands, includedirpaths);
								addForceIncludeCommands(commands, forceincludepaths);
								addMacroDefinitionCommands(commands, pchproperties.getMacroDefinitions());

								commands.add("/Yc");
								commands.add("/Fp" + pchoutpath);

								CollectingProcessIOConsumer stdoutcollector = new CollectingProcessIOConsumer();
								int procresult = MSVCUtils.runMSVCProcess(commands, workingdir, stdoutcollector, null,
										true);
								CompilationDependencyInfo depinfo = new CompilationDependencyInfo(pchcontents[0]);
								pchproperties.getFileLocation().accept(new FileLocationVisitor() {
									//add the compiled header file as an include dependency, so it is added to the source files
									@Override
									public void visit(ExecutionFileLocation loc) {
										depinfo.includes.add(loc.getPath());
									}
									//TODO support local
								});
								analyzeCLOutput(taskcontext, includedirpaths, stdoutcollector.getOutputBytes(), depinfo,
										procresult);
								CompilerInnerTaskResult headerprecompileresult;
								if (procresult == 0) {
									headerprecompileresult = CompilerInnerTaskResult.successful(entrypch);

									headerres = Optional.of(new PrecompiledHeaderDependencyInfo(depinfo));
								} else {
									headerprecompileresult = CompilerInnerTaskResult.failed(entrypch);

									headerres = Optional.empty();
								}
								headerprecompileresult.dependencyInfo = depinfo;
								coordinator.headerPrecompiled(headerprecompileresult,
										LocalFileProvider.getPathKeyStatic(pchcompilesakerfilepath),
										taskcontext.getExecutionContext()
												.getContentDescriptor(localfp.getPathKey(pchcompilesakerfilepath)));
							}
							precompiledHeaderCreationResults.put(entrypch, headerres);
							//clear the force include paths as they are part of the precompiled header
							//and they shouldn't be included in the source files
							forceincludepaths = Collections.emptyList();
						}
					}
				}
				if (!headerres.isPresent()) {
					//TODO reify exception
					throw new IOException("Failed to compile required precompiled header. (" + pchname + ")");
				}
				pchdepinfo = headerres.get();
			}

			List<String> commands = new ArrayList<>();
			commands.add(clexepath.toString());
			commands.addAll(ALWAYS_PRESENT_CL_PARAMETERS);
			commands.addAll(compilationentryproperties.getSimpleParameters());
			commands.add(getLanguageCommandLineOption(compilationentryproperties.getLanguage()) + compilefilepath);
			commands.add("/Fo" + objoutpath);
			addIncludeDirectoryCommands(commands, includedirpaths);
			addForceIncludeCommands(commands, forceincludepaths);
			addMacroDefinitionCommands(commands, compilationentryproperties.getMacroDefinitions());
			if (pchoutpath != null) {
				commands.add("/Fp" + pchoutpath);
				commands.add("/Yu" + pchname);
				if (forceincludepch) {
					commands.add("/FI" + pchname);
				}
			}

			//merge std error as the /showIncludes option doesn't work properly
			CollectingProcessIOConsumer stdoutcollector = new CollectingProcessIOConsumer();
			int procresult = MSVCUtils.runMSVCProcess(commands, workingdir, stdoutcollector, null, true);
			CompilationDependencyInfo depinfo = new CompilationDependencyInfo(contents[0]);

			analyzeCLOutput(taskcontext, includedirpaths, stdoutcollector.getOutputBytes(), depinfo, procresult);

			if (pchdepinfo != null) {
				//no need to add failed includes, as if the pch compilation fails, the source file doesn't get compiled
				depinfo.includes.addAll(pchdepinfo.includes);
			}

			CompilerInnerTaskResult result;
			if (procresult != 0) {
				if (depinfo.processOutput.isEmpty()) {
					//failed to start or something
					CompilerDiagnostic errordiag = new CompilerDiagnostic(null, SakerLog.SEVERITY_ERROR, -1, null,
							"cl exited with error code: " + procresult + " (0x" + Integer.toHexString(procresult)
									+ ")");
					depinfo.diagnostics.add(errordiag);
					printDiagnostic(taskcontext, null, errordiag);
				}
				result = CompilerInnerTaskResult.failed(compilationentry);
			} else {
				ProviderHolderPathKey objoutpathkey = localfp.getPathKey(objoutpath);
				taskutilities.addSynchronizeInvalidatedProviderPathFileToDirectory(outputDir, objoutpathkey,
						outputobjectfilename);

//				taskcontext.invalidate(objoutpathkey);
//				SakerFile objsakerfile = taskutilities.createProviderPathFile(objectfilename, objoutpathkey);
//				outputDir.add(objsakerfile);
//				objsakerfile.synchronize();
				result = CompilerInnerTaskResult.successful(compilationentry);
			}

			result.outputObjectName = outputobjectfilename;
			result.dependencyInfo = depinfo;

			return result;
		}

		private List<Path> getIncludePaths(TaskExecutionUtilities taskutilities, SakerEnvironment environment,
				Collection<IncludePathOption> includeoptions, boolean directories) {
			if (ObjectUtils.isNullOrEmpty(includeoptions)) {
				return Collections.emptyList();
			}
			List<Path> includepaths = new ArrayList<>();
			if (!ObjectUtils.isNullOrEmpty(includeoptions)) {
				for (IncludePathOption incopt : includeoptions) {
					Path incpath = getIncludePath(taskutilities, environment, incopt, directories);
					includepaths.add(incpath);
				}
			}
			return includepaths;
		}

		private Path getIncludePath(TaskExecutionUtilities taskutilities, SakerEnvironment environment,
				IncludePathOption includediroption, boolean directories) {
			Path[] includepath = { null };
			includediroption.accept(new IncludePathVisitor() {
				@Override
				public void visit(FileIncludePath includedir) {
					includedir.getFileLocation().accept(new FileLocationVisitor() {
						@Override
						public void visit(ExecutionFileLocation loc) {
							SakerPath path = loc.getPath();
							try {
								if (directories) {
									includepath[0] = mirrorHandler.mirrorDirectory(taskutilities, path);
								} else {
									//XXX handle mirrored force include contents?
									includepath[0] = mirrorHandler.mirrorFile(taskutilities, path).getPath();
								}
							} catch (FileMirroringUnavailableException | IOException e) {
								throw ObjectUtils.sneakyThrow(e);
							}
						}

						@Override
						public void visit(LocalFileLocation loc) {
							// TODO handle local include directory
							FileLocationVisitor.super.visit(loc);
						}
					});
				}

				@Override
				public void visit(SDKPathReference includedir) {
					//XXX duplicated code with linker worker
					String sdkname = includedir.getSDKName();
					if (ObjectUtils.isNullOrEmpty(sdkname)) {
						throw new SDKPathNotFoundException("Include directory returned empty sdk name: " + includedir);
					}
					SDKReference sdkref = getSDKReferenceForName(environment, sdkname);
					if (sdkref == null) {
						throw new SDKPathNotFoundException("SDK configuration not found for name: " + sdkname
								+ " required by include directory: " + includedir);
					}
					try {
						SakerPath sdkdirpath = includedir.getPath(sdkref);
						if (sdkdirpath == null) {
							throw new SDKPathNotFoundException("No SDK include directory found for: " + includedir
									+ " in SDK: " + sdkname + " as " + sdkref);
						}
						includepath[0] = LocalFileProvider.toRealPath(sdkdirpath);
					} catch (Exception e) {
						throw new SDKPathNotFoundException("Failed to retrieve SDK include directory for: " + includedir
								+ " in SDK: " + sdkname + " as " + sdkref, e);
					}
				}
			});
			return includepath[0];
		}

		private static void addIncludeDirectoryCommands(List<String> commands, List<Path> includedirpaths) {
			if (ObjectUtils.isNullOrEmpty(includedirpaths)) {
				return;
			}
			for (Path incdir : includedirpaths) {
				commands.add("/I" + incdir);
			}
		}

		private static void addForceIncludeCommands(List<String> commands, List<Path> forceincludepaths) {
			if (ObjectUtils.isNullOrEmpty(forceincludepaths)) {
				return;
			}
			for (Path fipath : forceincludepaths) {
				commands.add("/FI" + fipath);
			}
		}

		private static void analyzeCLOutput(TaskContext taskcontext, List<Path> includedirpaths,
				ByteArrayRegion stdoutbytecontents, CompilationDependencyInfo depinfo, int procresult) {
			NavigableSet<SakerPath> includes = depinfo.includes;
			NavigableSet<SakerPath> failedincludes = depinfo.failedIncludes;
			NavigableSet<CompilerDiagnostic> diagnostics = depinfo.diagnostics;
			ExecutionContext executioncontext = taskcontext.getExecutionContext();
			if (stdoutbytecontents.isEmpty()) {
				if (procresult != 0) {
					CompilerDiagnostic errordiag = new CompilerDiagnostic(null, SakerLog.SEVERITY_ERROR, -1, null,
							"cl exited with error code: " + procresult + " (0x" + Integer.toHexString(procresult)
									+ ")");
					depinfo.diagnostics.add(errordiag);
					printDiagnostic(taskcontext, null, errordiag);
				}
			} else {
				try (UnsyncByteArrayOutputStream processout = new UnsyncByteArrayOutputStream()) {
					try (DataInputUnsyncByteArrayInputStream reader = new DataInputUnsyncByteArrayInputStream(
							stdoutbytecontents)) {
						for (String line; (line = reader.readLine()) != null;) {
							if (line.isEmpty()) {
								continue;
							}
							Matcher errmatcher = CL_OUTPUT_ERROR_PATTERN.matcher(line);
							if (errmatcher.matches()) {
								String file = errmatcher.group(CL_OUTPUT_ERROR_GROUP_FILE);
								String linenum = errmatcher.group(CL_OUTPUT_ERROR_GROUP_LINENUM);
								String type = errmatcher.group(CL_OUTPUT_ERROR_GROUP_TYPE);
								String clerror = errmatcher.group(CL_OUTPUT_ERROR_GROUP_CLERROR);
								String desc = errmatcher.group(CL_OUTPUT_ERROR_GROUP_DESC);
								int severity;
								switch (type.toLowerCase(Locale.ENGLISH)) {
									case "fatal error":
									case "error": {
										severity = SakerLog.SEVERITY_ERROR;
										break;
									}
									case "warning": {
										severity = SakerLog.SEVERITY_WARNING;
										break;
									}
									default: {
										severity = SakerLog.SEVERITY_INFO;
										break;
									}
								}
								SakerPath diagnosticpath = null;
								int lineindex = -1;
								try {
									Path diagpath = Paths.get(file);
									diagnosticpath = executioncontext.toUnmirrorPath(diagpath);
								} catch (Exception e) {
									SakerLog.error().verbose()
											.println("Failed to parse CL output path: " + e + " for " + file);
								}
								if (diagnosticpath != null) {
									//only set line index if the path is known
									try {
										lineindex = Integer.parseInt(linenum) - 1;
									} catch (NumberFormatException e) {
										//ignore
									}
								}
								String trimmedclerror = clerror == null ? null : clerror.trim();
								if (desc != null && "C1083".equalsIgnoreCase(trimmedclerror)) {
									//C1083: Cannot open include file: 'the/path/to/the/file': No such file or directory
									int idx1 = desc.indexOf('\'');
									if (idx1 >= 0) {
										int idx2 = desc.lastIndexOf('\'');
										if (idx2 > idx1) {
											String notfoundpathstr = desc.substring(idx1 + 1, idx2);
											Path notfoundpath = Paths.get(notfoundpathstr);
											if (notfoundpath.isAbsolute()) {
												SakerPath unmirrored = executioncontext.toUnmirrorPath(notfoundpath);
												if (unmirrored != null) {
													if (failedincludes == null) {
														failedincludes = new TreeSet<>();
													}
													failedincludes.add(unmirrored);
												} else {
													//TODO handle local missing include
												}
											} else {
												for (Path includedirpath : includedirpaths) {
													Path notfoundabspath = includedirpath.resolve(notfoundpath);
													SakerPath unmirrored = executioncontext
															.toUnmirrorPath(notfoundabspath);
													if (unmirrored != null) {
														if (failedincludes == null) {
															failedincludes = new TreeSet<>();
														}
														failedincludes.add(unmirrored);
													} else {
														//TODO handle local missing include
													}
												}
											}
										}
									}
								}
								diagnostics.add(new CompilerDiagnostic(diagnosticpath, severity, lineindex,
										trimmedclerror, desc));
							} else if (line.startsWith("Note: including file:")) {
								//XXX should we choose a lower case locale?
								// 21: len of Note: ...
								String includedfilepathstr = line.substring(21).trim().toLowerCase();
								try {
									Path reallocalpath = Paths.get(includedfilepathstr)
											.toRealPath(LinkOption.NOFOLLOW_LINKS);
									SakerPath unmirrored = executioncontext.toUnmirrorPath(reallocalpath);
									if (unmirrored != null) {
										includes.add(unmirrored);
									} else {
										//TODO handle non mirrored included path
									}
								} catch (IOException | InvalidPathException e) {
									SakerLog.error().verbose().println("Failed to determine included file path for: "
											+ includedfilepathstr + " (" + e + ")");
									continue;
								}
							} else {
								processout.write(ByteArrayRegion.wrap((line + "\n").getBytes(StandardCharsets.UTF_8)));
							}
						}
					}
					depinfo.processOutput = processout.toByteArrayRegion();
				}
			}
		}

		private static void addMacroDefinitionCommands(List<String> commands, Map<String, String> macrodefs) {
			if (!ObjectUtils.isNullOrEmpty(macrodefs)) {
				for (Entry<String, String> entry : macrodefs.entrySet()) {
					String val = entry.getValue();
					commands.add("/D" + entry.getKey() + (ObjectUtils.isNullOrEmpty(val) ? "" : "=" + val));
				}
			}
		}

		//XXX somewhat duplicated with linker worker factory
		private SDKReference getSDKReferenceForName(SakerEnvironment environment, String sdkname) {
			Supplier<SDKReference> sdkref = referencedSDKCache.get(sdkname);
			if (sdkref != null) {
				return sdkref.get();
			}
			synchronized (sdkCacheLocks.computeIfAbsent(sdkname, Functionals.objectComputer())) {
				sdkref = referencedSDKCache.get(sdkname);
				if (sdkref != null) {
					return sdkref.get();
				}
				SDKDescription desc = sdkDescriptions.get(sdkname);
				if (desc == null) {
					sdkref = () -> {
						throw new SDKNotFoundException("SDK not found for name: " + sdkname);
					};
				} else {
					try {
						SDKReference refresult = SDKSupportUtils.resolveSDKReference(environment, desc);
						sdkref = Functionals.valSupplier(refresult);
					} catch (Exception e) {
						sdkref = () -> {
							throw new SDKManagementException("Failed to resolve SDK: " + sdkname + " as " + desc, e);
						};
					}
				}
				referencedSDKCache.put(sdkname, sdkref);
			}
			return sdkref.get();
		}
	}

	private static String getLanguageCommandLineOption(String language) {
		if ("c++".equalsIgnoreCase(language)) {
			return "/Tp";
		}
		if (language == null || "c".equalsIgnoreCase(language)) {
			return "/Tc";
		}
		throw new IllegalArgumentException("Unknown language: " + language);
	}

	private static void printDiagnostics(TaskContext taskcontext, SakerPath sourcefilepath, CompiledFileState state) {
		NavigableSet<CompilerDiagnostic> diagnostics = state.getDiagnostics();
		if (ObjectUtils.isNullOrEmpty(diagnostics)) {
			return;
		}
		printDiagnostics(taskcontext, sourcefilepath, diagnostics);
	}

	private static void printDiagnostics(TaskContext taskcontext, SakerPath sourcefilepath,
			NavigableSet<CompilerDiagnostic> diagnostics) {
		for (CompilerDiagnostic d : diagnostics) {
			printDiagnostic(taskcontext, sourcefilepath, d);
		}
	}

	private static void printDiagnostic(TaskContext taskcontext, SakerPath sourcefilepath, CompilerDiagnostic d) {
		String clerror = d.getErrorCode();
		String desc = d.getDescription();

		SakerLog log = SakerLog.severity(d.getSeverity());
		log.out(taskcontext);
		SakerPath path = d.getPath();
		if (path != null) {
			log.path(path);
			log.line(d.getLineIndex());
		} else {
			log.path(sourcefilepath);
		}

		log.println((clerror == null ? "" : clerror + ": ") + desc);
	}

}
