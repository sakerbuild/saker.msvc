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
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;

public class CompilerState implements Externalizable {
	private static final long serialVersionUID = 1L;

	public static class CompiledFileState implements Externalizable {
		private static final long serialVersionUID = 1L;

		private ContentDescriptor inputContents;
		private FileCompilationConfiguration compilationConfiguration;
		private SakerPath outputObjectPath;
		private ContentDescriptor outputObjectContents;
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

		public SakerPath getOutputObjectPath() {
			return outputObjectPath;
		}

		public ContentDescriptor getOutputObjectContents() {
			return outputObjectContents;
		}

		public FileCompilationConfiguration getCompilationConfiguration() {
			return compilationConfiguration;
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

		public void setDiagnostics(NavigableSet<CompilerDiagnostic> diagnostics) {
			this.diagnostics = diagnostics;
		}

		public void setInputContents(ContentDescriptor inputContents) {
			this.inputContents = inputContents;
		}

		public void setObjectOutputContents(SakerPath outputpath, ContentDescriptor outputContents) {
			this.outputObjectPath = outputpath;
			this.outputObjectContents = outputContents;
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
			out.writeObject(outputObjectPath);
			out.writeObject(outputObjectContents);
			SerialUtils.writeExternalCollection(out, diagnostics);
			SerialUtils.writeExternalCollection(out, includes);
			SerialUtils.writeExternalCollection(out, failedIncludes);
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
			inputContents = (ContentDescriptor) in.readObject();
			compilationConfiguration = (FileCompilationConfiguration) in.readObject();
			outputObjectPath = (SakerPath) in.readObject();
			outputObjectContents = (ContentDescriptor) in.readObject();
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

	public NavigableMap<SakerPath, ContentDescriptor> getOutputObjectFileContentDescriptors() {
		TreeMap<SakerPath, ContentDescriptor> result = new TreeMap<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath outpath = state.getOutputObjectPath();
			if (outpath == null) {
				continue;
			}
			result.put(outpath, state.getOutputObjectContents());
		}
		return result;
	}

	public NavigableSet<String> getAllOutputFileNames() {
		TreeSet<String> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath objpath = state.getOutputObjectPath();
			if (objpath != null) {
				result.add(objpath.getFileName());
			}
		}
		return result;
	}

	public NavigableSet<SakerPath> getOutputObjectFilePaths() {
		TreeSet<SakerPath> result = new TreeSet<>();
		for (CompiledFileState state : executionCompiledFiles.values()) {
			SakerPath outpath = state.getOutputObjectPath();
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
			if (state.getOutputObjectPath() == null) {
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
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
		environmentSelection = (EnvironmentSelectionResult) in.readObject();
	}

}
