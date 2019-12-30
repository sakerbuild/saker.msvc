package saker.msvc.impl.ccompile;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
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
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import saker.build.task.identifier.TaskIdentifier;
import saker.build.task.utils.FixedTaskDuplicationPredicate;
import saker.build.task.utils.TaskUtils;
import saker.build.thirdparty.saker.rmi.annot.transfer.RMIWrap;
import saker.build.thirdparty.saker.rmi.io.RMIObjectInput;
import saker.build.thirdparty.saker.rmi.io.RMIObjectOutput;
import saker.build.thirdparty.saker.rmi.io.wrap.RMIWrapper;
import saker.build.thirdparty.saker.util.ConcurrentPrependAccumulator;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.DataInputUnsyncByteArrayInputStream;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.compiler.utils.api.options.CompilationIdentifier;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.CompilerState.CompiledFileState;
import saker.msvc.impl.ccompile.option.FileIncludeDirectory;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.ccompile.option.IncludeDirectoryVisitor;
import saker.msvc.impl.util.EnvironmentSelectionTestExecutionProperty;
import saker.msvc.impl.util.InnerTaskMirrorHandler;
import saker.msvc.impl.util.SystemArchitectureEnvironmentProperty;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.proc.NativeProcess.IOProcessor;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.ResolvedSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKDescriptionVisitor;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.UserSDKDescription;
import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;
import testing.saker.msvc.TestFlag;

public class MSVCCCompileWorkerTaskFactory implements TaskFactory<Object>, Task<Object>, Externalizable {
	private static final long serialVersionUID = 1L;

	private static final NavigableSet<String> WORKER_TASK_CAPABILITIES = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { CAPABILITY_INNER_TASKS_COMPUTATIONAL });

	public static final Set<String> ALWAYS_PRESENT_CL_PARAMETERS = ImmutableUtils
			.makeImmutableNavigableSet(new String[] { "/nologo", "/c", "/showIncludes" });

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
		ObjectUtils.requireComparator(sdkdescriptions, MSVCUtils.getSDKNameComparator());
		this.sdkDescriptions = sdkdescriptions;
		if (!sdkdescriptions.containsKey(MSVCUtils.SDK_NAME_MSVC)) {
			throw new IllegalArgumentException("MSVC SDK unspecified for compilation.");
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

		SDKBasedExecutionEnvironmentSelector envselector = MSVCUtils.createEnvironmentSelectorForSDKs(sdkDescriptions);
		NavigableMap<String, SDKDescription> compilerinnertasksdkdescriptions = sdkDescriptions;
		EnvironmentSelectionResult envselectionresult;
		if (envselector != null) {
			envselectionresult = taskcontext.getTaskUtilities()
					.getReportExecutionDependency(new EnvironmentSelectionTestExecutionProperty(envselector));
			envselector = MSVCUtils.undefaultizeSDKEnvironmentSelector(envselector, envselectionresult);
			compilerinnertasksdkdescriptions = envselector.getDescriptions();
		} else {
			envselectionresult = null;
		}

		CompilerState nstate = new CompilerState();
		nstate.setSdkDescriptions(sdkDescriptions);
		nstate.setEnvironmentSelection(envselectionresult);

		CompilerState prevoutput = taskcontext.getPreviousTaskOutput(CompilerState.class, CompilerState.class);
		if (prevoutput != null) {
			filterUnchangedPreviousFiles(taskcontext, compilationentries, stateexecutioncompiledfiles, prevoutput,
					nstate);
		}

		if (!compilationentries.isEmpty()) {
			System.out.println("Compiling " + compilationentries.size() + " source files.");
			ConcurrentPrependAccumulator<FileCompilationConfiguration> fileaccumulator = new ConcurrentPrependAccumulator<>(
					compilationentries);
			InnerTaskExecutionParameters innertaskparams = new InnerTaskExecutionParameters();
			innertaskparams.setClusterDuplicateFactor(compilationentries.size());
			innertaskparams.setDuplicationPredicate(new FixedTaskDuplicationPredicate(compilationentries.size()));

			SourceCompilerInnerTaskFactory innertask = new SourceCompilerInnerTaskFactory(fileaccumulator::take,
					outdirpath, architecture, compilerinnertasksdkdescriptions, envselector, outdir);
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
				taskcontext.getStandardOut().write(compilationresult.getProcessOutput());
				compilationentry.getFileLocation().accept(new FileLocationVisitor() {
					@Override
					public void visit(ExecutionFileLocation loc) {
						CompiledFileState compiledfilestate = new CompiledFileState(
								compilationresult.getInputContents(), compilationentry);
						compiledfilestate.setDiagnostics(compilationresult.getDiagnostics());
						compiledfilestate.setIncludes(compilationresult.getIncludes());
						compiledfilestate.setFailedIncludes(compilationresult.getFailedIncludes());
						if (compilationresult.isSuccessful()) {
							SakerPath outputpath = outdirpath.resolve(compilationresult.getOutputName());
							SakerFile outfile = taskcontext.getTaskUtilities().resolveFileAtPath(outputpath);
							if (outfile == null) {
								throw ObjectUtils.sneakyThrow(
										new FileNotFoundException("Output object file was not found: " + outdirpath));
							}
							ContentDescriptor outcontentdescriptor = outfile.getContentDescriptor();

							compiledfilestate.setOutputContents(outputpath, outcontentdescriptor);
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
			compilationconfig.getFileLocation().accept(new FileLocationVisitor() {

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
			Set<IncludeDirectoryOption> includedirs = compilationconfig.getIncludeDirectories();
			if (!ObjectUtils.isNullOrEmpty(includedirs)) {
				for (IncludeDirectoryOption includediroption : includedirs) {
					includediroption.accept(new IncludeDirectoryVisitor() {
						@Override
						public void visit(FileIncludeDirectory includedir) {
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
				nstate.getOutputContentDescriptors());
		taskcontext.setTaskOutput(CompilerState.class, nstate);

		//remove files which are not part of the output object files
		ObjectUtils.iterateOrderedIterables(outdir.getChildren().entrySet(), nstate.getOutputFileNames(),
				(entry, name) -> entry.getKey().compareTo(name), (entry, outf) -> {
					if (outf == null) {
						entry.getValue().remove();
					}
				});
		//use the nothing predicate to only delete the files which were removed
		outdir.synchronize(DirectoryVisitPredicate.nothing());

		if (!nstate.isAllCompilationSucceeded()) {
			taskcontext.abortExecution(new IOException("Compilation failed."));
			return null;
		}

		MSVCCompilerWorkerTaskOutputImpl result = new MSVCCompilerWorkerTaskOutputImpl(passidentifier, architecture,
				sdkDescriptions);
		result.setObjectFilePaths(nstate.getObjectFilePaths());
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
			SakerPath outpath = prevfilestate.getOutputPath();
			if (outpath != null) {
				if (relevantchanges.contains(outpath)) {
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

			compilationentry.getFileLocation().accept(new FileLocationVisitor() {
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
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in, MSVCUtils.getSDKNameComparator());
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

	private static class CompilerInnerTaskResult implements Externalizable {
		private static final long serialVersionUID = 1L;

		protected FileCompilationConfiguration compilationEntry;
		protected boolean successful;
		protected String outputName;
		protected ContentDescriptor inputContents;
		protected NavigableSet<CompilerDiagnostic> diagnostics;
		protected NavigableSet<SakerPath> includes;
		protected NavigableSet<SakerPath> failedIncludes;
		protected ByteArrayRegion processOutput;

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

		public String getOutputName() {
			return outputName;
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
			out.writeObject(compilationEntry);
			out.writeBoolean(successful);
			out.writeObject(outputName);
			out.writeObject(inputContents);
			SerialUtils.writeExternalCollection(out, diagnostics);
			SerialUtils.writeExternalCollection(out, includes);
			SerialUtils.writeExternalCollection(out, failedIncludes);
			out.writeObject(processOutput);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			compilationEntry = (FileCompilationConfiguration) in.readObject();
			successful = in.readBoolean();
			outputName = (String) in.readObject();
			inputContents = (ContentDescriptor) in.readObject();
			diagnostics = SerialUtils.readExternalImmutableNavigableSet(in);
			includes = SerialUtils.readExternalImmutableNavigableSet(in);
			failedIncludes = SerialUtils.readExternalImmutableNavigableSet(in);
			processOutput = (ByteArrayRegion) in.readObject();
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

	@RMIWrap(SourceCompilerRMIWrapper.class)
	private static class SourceCompilerInnerTaskFactory
			implements TaskFactory<CompilerInnerTaskResult>, Task<CompilerInnerTaskResult> {
		protected Supplier<FileCompilationConfiguration> fileLocationSuppier;
		protected SakerPath outputDirPath;
		protected String architecture;
		protected NavigableMap<String, SDKDescription> sdkDescriptions;
		protected TaskExecutionEnvironmentSelector environmentSelector;
		protected SakerDirectory outputDir;

		private transient InnerTaskMirrorHandler mirrorHandler = new InnerTaskMirrorHandler();
		private transient NavigableMap<String, SDKReference> referencedSDKCache = new ConcurrentSkipListMap<>(
				MSVCUtils.getSDKNameComparator());
		private transient NavigableMap<String, Object> sdkCacheLocks = new ConcurrentSkipListMap<>(
				MSVCUtils.getSDKNameComparator());

		/**
		 * For RMI transfer.
		 */
		public SourceCompilerInnerTaskFactory() {
		}

		public SourceCompilerInnerTaskFactory(Supplier<FileCompilationConfiguration> fileLocationSuppier,
				SakerPath outputDirPath, String architecture, NavigableMap<String, SDKDescription> sdkDescriptions,
				TaskExecutionEnvironmentSelector envselector, SakerDirectory outputDir) {
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

		@Override
		public CompilerInnerTaskResult run(TaskContext taskcontext) throws Exception {
			FileCompilationConfiguration compilationentry = fileLocationSuppier.get();
			if (compilationentry == null) {
				return null;
			}
			Path[] compilefilepath = { null };
			ContentDescriptor[] contents = { null };
			TaskExecutionUtilities taskutilities = taskcontext.getTaskUtilities();
			ExecutionContext executioncontext = taskcontext.getExecutionContext();
			SakerEnvironment environment = executioncontext.getEnvironment();
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
			List<Path> includedirpaths = new ArrayList<>();
			Set<IncludeDirectoryOption> includedirectories = compilationentry.getIncludeDirectories();

			if (!ObjectUtils.isNullOrEmpty(includedirectories)) {
				for (IncludeDirectoryOption includediroption : includedirectories) {
					includediroption.accept(new IncludeDirectoryVisitor() {
						@Override
						public void visit(FileIncludeDirectory includedir) {
							includedir.getFileLocation().accept(new FileLocationVisitor() {
								@Override
								public void visit(ExecutionFileLocation loc) {
									SakerPath path = loc.getPath();
									try {
										includedirpaths.add(mirrorHandler.mirrorDirectory(taskutilities, path));
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
								throw new NullPointerException(
										"Include directory returned empty sdk name: " + includedir);
							}
							SDKReference sdkref = getSDKReferenceForName(environment, sdkname);
							if (sdkref == null) {
								throw new IllegalArgumentException("SDK configuration not found for name: " + sdkname
										+ " required by include directory: " + includedir);
							}
							try {
								SakerPath sdkdirpath = includedir.getPath(sdkref);
								if (sdkdirpath == null) {
									throw new IllegalArgumentException("No SDK include directory found for: "
											+ includedir + " in SDK: " + sdkname + " as " + sdkref);
								}
								includedirpaths.add(LocalFileProvider.toRealPath(sdkdirpath));
							} catch (Exception e) {
								throw new IllegalArgumentException("Failed to retrieve SDK include directory for: "
										+ includedir + " in SDK: " + sdkname + " as " + sdkref, e);
							}
						}

					});

				}
			}
			FileCompilationConfiguration compilationconfiguration = compilationentry;
			String objectfilename = compilationconfiguration.getOutFileName() + ".obj";
			Path objoutpath = executioncontext.toMirrorPath(outputDirPath.resolve(objectfilename));
			//create the parent directory, else the process will throw
			LocalFileProvider.getInstance().createDirectories(objoutpath.getParent());

			String hostarchitecture = environment
					.getEnvironmentPropertyCurrentValue(SystemArchitectureEnvironmentProperty.INSTANCE);

			SDKReference vcsdk = getSDKReferenceForName(environment, MSVCUtils.SDK_NAME_MSVC);

			SakerPath clexepath = MSVCUtils.getVCSDKExecutablePath(vcsdk, hostarchitecture, architecture,
					MSVCUtils.VC_EXECUTABLE_NAME_CL);
			if (clexepath == null) {
				throw new IllegalArgumentException("SDK doesn't contain appropriate cl.exe: " + vcsdk);
			}
			SakerPath workingdir = MSVCUtils.getVCSDKExecutableWorkingDirectoryPath(vcsdk, hostarchitecture,
					architecture, MSVCUtils.VC_EXECUTABLE_NAME_CL);
			if (workingdir == null) {
				workingdir = clexepath.getParent();
			}

			List<String> commands = new ArrayList<>();
			commands.addAll(ALWAYS_PRESENT_CL_PARAMETERS);
			commands.addAll(compilationconfiguration.getSimpleParameters());
			commands.add(getLanguageCommandLineOption(compilationconfiguration.getLanguage()) + compilefilepath[0]);
			commands.add("/Fo" + objoutpath);
			for (Path incdir : includedirpaths) {
				commands.add("/I" + incdir);
			}
			Map<String, String> macrodefs = compilationentry.getMacroDefinitions();
			if (!ObjectUtils.isNullOrEmpty(macrodefs)) {
				for (Entry<String, String> entry : macrodefs.entrySet()) {
					String val = entry.getValue();
					commands.add("/D" + entry.getKey() + (ObjectUtils.isNullOrEmpty(val) ? "" : "=" + val));
				}
			}

			ByteArrayRegion procbytecontents;
			int procresult;
			try (UnsyncByteArrayOutputStream proccontents = new UnsyncByteArrayOutputStream()) {
				procresult = MSVCUtils.runClProcess(clexepath, commands, workingdir, new IOProcessor() {
					@Override
					public boolean standardInputBytesAvailable(ByteBuffer inbuffer) {
						proccontents.write(inbuffer);
						return true;
					}
				});
				procbytecontents = proccontents.toByteArrayRegion();
			}

			NavigableSet<CompilerDiagnostic> diagnostics = new TreeSet<>();

			NavigableSet<SakerPath> includebases = new TreeSet<>();
			for (Path incdir : includedirpaths) {
				includebases.add(SakerPath.valueOf(incdir));
			}
			includebases.add(SakerPath.valueOf(compilefilepath[0]).getParent());

			//TODO separately handle the std out and std err of the process so we don't copy the output
			UnsyncByteArrayOutputStream processout = new UnsyncByteArrayOutputStream();

			NavigableSet<SakerPath> includes = new TreeSet<>();
			NavigableSet<SakerPath> failedincludes = null;
			boolean hadline = false;
			//TODO print the lines in a locked way, so they're not interlaced
			try (DataInputUnsyncByteArrayInputStream reader = new DataInputUnsyncByteArrayInputStream(
					procbytecontents)) {
				for (String line; (line = reader.readLine()) != null;) {
					if (line.isEmpty()) {
						continue;
					}
					hadline = true;
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
							SakerLog.error().verbose().println("Failed to parse CL output path: " + e + " for " + file);
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
											SakerPath unmirrored = executioncontext.toUnmirrorPath(notfoundabspath);
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
						diagnostics
								.add(new CompilerDiagnostic(diagnosticpath, severity, lineindex, trimmedclerror, desc));
					} else if (line.startsWith("Note: including file:")) {
						//XXX should we choose a lower case locale?
						String includedfilepathstr = line.substring(21).trim().toLowerCase();// len of Note: ...
						try {
							Path reallocalpath = Paths.get(includedfilepathstr).toRealPath(LinkOption.NOFOLLOW_LINKS);
							SakerPath reallocalsakerpath = SakerPath.valueOf(reallocalpath);
							SakerPath unmirrored = executioncontext.toUnmirrorPath(reallocalpath);
							if (!SakerPathFiles.hasPathOrParent(includebases, reallocalsakerpath)) {
								StringBuilder sb = new StringBuilder();
								if (unmirrored == null) {
									sb.append("Included local file: ");
									sb.append(reallocalsakerpath);
								} else {
									sb.append("Included file: ");
									sb.append(unmirrored);
								}
								sb.append(" is not present in any of the include paths.");
								diagnostics.add(new CompilerDiagnostic(null, SakerLog.SEVERITY_WARNING, -1, null,
										sb.toString()));
							}
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
			CompilerInnerTaskResult result;
			if (procresult != 0) {
				if (!hadline) {
					//no output from the process, it probably failed to start
					SakerLog.error().verbose().println("Failed to start cl process: " + procresult + " (0x"
							+ Integer.toHexString(procresult) + ")");
				}
				result = CompilerInnerTaskResult.failed(compilationentry);
			} else {
				ProviderHolderPathKey objoutpathkey = LocalFileProvider.getInstance().getPathKey(objoutpath);
				taskutilities.addSynchronizeInvalidatedProviderPathFileToDirectory(outputDir, objoutpathkey,
						objectfilename);
//				taskcontext.invalidate(objoutpathkey);
//				SakerFile objsakerfile = taskutilities.createProviderPathFile(objectfilename, objoutpathkey);
//				outputDir.add(objsakerfile);
//				objsakerfile.synchronize();
				result = CompilerInnerTaskResult.successful(compilationentry);
			}

			result.outputName = objectfilename;
			result.inputContents = contents[0];
			if (!diagnostics.isEmpty()) {
				//dont assign if non empty, keep it null
				result.diagnostics = diagnostics;
			}
			result.includes = includes;
			result.failedIncludes = failedincludes;
			result.processOutput = processout.toByteArrayRegion();

			return result;
		}

		//XXX somewhat duplicated with linker worker factory
		private SDKReference getSDKReferenceForName(SakerEnvironment environment, String sdkname) {
			SDKReference sdkref = referencedSDKCache.get(sdkname);
			if (sdkref != null) {
				return sdkref;
			}
			synchronized (sdkCacheLocks.computeIfAbsent(sdkname, Functionals.objectComputer())) {
				sdkref = referencedSDKCache.get(sdkname);
				if (sdkref != null) {
					return sdkref;
				}
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
						SDKReference envsdkref = environment.getEnvironmentPropertyCurrentValue(envproperty);
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
				sdkref = refresult[0];
				referencedSDKCache.put(sdkname, sdkref);
			}
			return sdkref;
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
		for (CompilerDiagnostic d : diagnostics) {
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

}
