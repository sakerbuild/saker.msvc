package saker.msvc.impl.ccompile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;

import saker.build.file.content.ContentDescriptor;
import saker.build.file.path.SakerPath;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.TransformingSortedMap;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.msvc.impl.MSVCUtils;
import saker.sdk.support.api.SDKDescription;

public class CompilerState implements Externalizable {
	private static final long serialVersionUID = 1L;

	public static class CompiledFileState implements Externalizable {
		private static final long serialVersionUID = 1L;

		private ContentDescriptor inputContents;
		private FileCompilationConfiguration compilationConfiguration;
		private SakerPath outputPath;
		private ContentDescriptor outputContents;
		private NavigableSet<CompilerDiagnostic> diagnostics;
		/**
		 * Absolute execution paths of referenced include files.
		 */
		private NavigableSet<SakerPath> includes;
		/**
		 * Absolute execution paths based on the include failure error messages and include directories
		 */
		private NavigableSet<SakerPath> failedIncludes;

		/**
		 * For {@link Externalizable}.
		 */
		public CompiledFileState() {
		}

		public CompiledFileState(ContentDescriptor inputContents,
				FileCompilationConfiguration compilationConfiguration) {
			this.inputContents = inputContents;
			this.compilationConfiguration = compilationConfiguration;
		}

		/**
		 * <code>null</code> if compilation failed.
		 */
		public SakerPath getOutputPath() {
			return outputPath;
		}

		public FileCompilationConfiguration getCompilationConfiguration() {
			return compilationConfiguration;
		}

		public ContentDescriptor getInputContents() {
			return inputContents;
		}

		public ContentDescriptor getOutputContents() {
			return outputContents;
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

		public void setDiagnostics(NavigableSet<CompilerDiagnostic> diagnostics) {
			this.diagnostics = diagnostics;
		}

		public void setInputContents(ContentDescriptor inputContents) {
			this.inputContents = inputContents;
		}

		public void setOutputContents(SakerPath outputpath, ContentDescriptor outputContents) {
			this.outputPath = outputpath;
			this.outputContents = outputContents;
		}

		public void setIncludes(NavigableSet<SakerPath> includes) {
			this.includes = includes;
		}

		public void setFailedIncludes(NavigableSet<SakerPath> failedIncludes) {
			this.failedIncludes = failedIncludes;
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
			out.writeObject(inputContents);
			out.writeObject(compilationConfiguration);
			out.writeObject(outputPath);
			out.writeObject(outputContents);
			SerialUtils.writeExternalCollection(out, diagnostics);
			SerialUtils.writeExternalCollection(out, includes);
			SerialUtils.writeExternalCollection(out, failedIncludes);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			inputContents = (ContentDescriptor) in.readObject();
			compilationConfiguration = (FileCompilationConfiguration) in.readObject();
			outputPath = (SakerPath) in.readObject();
			outputContents = (ContentDescriptor) in.readObject();
			diagnostics = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			includes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
			failedIncludes = SerialUtils.readExternalSortedImmutableNavigableSet(in);
		}

	}

	//maps out file names to states
	private NavigableMap<String, CompiledFileState> executionCompiledFiles = Collections.emptyNavigableMap();
	private NavigableMap<String, SDKDescription> sdkDescriptions;
	private EnvironmentSelectionResult environmentSelection;

	/**
	 * For {@link Externalizable}.
	 */
	public CompilerState() {
	}

	public void setExecutionCompiledFiles(NavigableMap<String, CompiledFileState> executionCompiledFiles) {
		this.executionCompiledFiles = executionCompiledFiles;
	}

	public void setSdkDescriptions(NavigableMap<String, SDKDescription> sdkDescriptions) {
		this.sdkDescriptions = sdkDescriptions;
	}

	public NavigableMap<String, CompiledFileState> getExecutionCompiledFiles() {
		return executionCompiledFiles;
	}

	public NavigableMap<String, SDKDescription> getSdkDescriptions() {
		return sdkDescriptions;
	}

	public EnvironmentSelectionResult getEnvironmentSelection() {
		return environmentSelection;
	}

	public void setEnvironmentSelection(EnvironmentSelectionResult envselectionresult) {
		this.environmentSelection = envselectionresult;
	}

	public NavigableMap<String, ContentDescriptor> getInputContentDescriptors() {
		return ImmutableUtils.makeImmutableNavigableMap(
				new TransformingSortedMap<String, CompiledFileState, String, ContentDescriptor>(
						executionCompiledFiles) {
					@Override
					protected Entry<String, ContentDescriptor> transformEntry(String key, CompiledFileState value) {
						return ImmutableUtils.makeImmutableMapEntry(key, value.getInputContents());
					}
				});
	}

	public NavigableMap<SakerPath, ContentDescriptor> getOutputContentDescriptors() {
		TreeMap<SakerPath, ContentDescriptor> result = new TreeMap<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath outpath = state.getOutputPath();
			if (outpath == null) {
				continue;
			}
			result.put(outpath, state.getOutputContents());
		}
		return result;
	}

	public NavigableSet<String> getOutputFileNames() {
		TreeSet<String> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath outpath = state.getOutputPath();
			if (outpath == null) {
				continue;
			}
			result.add(outpath.getFileName());
		}
		return result;
	}

//	public NavigableSet<SakerPath> getCompiledFileParentDirectoryPaths() {
//		TreeSet<SakerPath> result = new TreeSet<>();
//		for (SakerPath compiledfilepath : executionCompiledFiles.keySet()) {
//			result.add(compiledfilepath.getParent());
//		}
//		return result;
//	}

	public NavigableSet<SakerPath> getObjectFilePaths() {
		TreeSet<SakerPath> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath outpath = state.getOutputPath();
			if (outpath == null) {
				continue;
			}
			result.add(outpath);
		}
		return result;
	}

	public NavigableSet<SakerPath> getAllReferencedIncludes() {
		TreeSet<SakerPath> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			ObjectUtils.addAll(result, state.getIncludes());
		}
		return result;
	}

	public NavigableSet<SakerPath> getAllReferencedFailedIncludes() {
		TreeSet<SakerPath> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			ObjectUtils.addAll(result, state.getFailedIncludes());
		}
		return result;
	}

	public boolean isAllCompilationSucceeded() {
		for (CompiledFileState state : executionCompiledFiles.values()) {
			if (state.getOutputPath() == null) {
				return false;
			}
		}
		return true;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalMap(out, executionCompiledFiles);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
		out.writeObject(environmentSelection);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		executionCompiledFiles = SerialUtils.readExternalSortedImmutableNavigableMap(in);
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in, MSVCUtils.getSDKNameComparator());
		environmentSelection = (EnvironmentSelectionResult) in.readObject();
	}

}
