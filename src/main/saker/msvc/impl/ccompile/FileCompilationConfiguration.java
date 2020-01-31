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
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.std.api.file.location.FileLocation;

public class FileCompilationConfiguration implements Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation fileLocation;
	private String outFileName;
	private String language;
	private Set<IncludeDirectoryOption> includeDirectories;
	private Map<String, String> macroDefinitions;
	private NavigableSet<String> simpleParameters = Collections.emptyNavigableSet();
	private boolean createPrecompiledHeader;

	/**
	 * For {@link Externalizable}.
	 */
	public FileCompilationConfiguration() {
	}

	public FileCompilationConfiguration(FileLocation fileLocation, String outFileName) {
		this.fileLocation = fileLocation;
		this.outFileName = outFileName;
	}

	public void setIncludeDirectories(Set<IncludeDirectoryOption> includeDirectories) {
		if (ObjectUtils.isNullOrEmpty(includeDirectories)) {
			this.includeDirectories = null;
		} else {
			this.includeDirectories = includeDirectories;
		}
	}

	public void setSimpleParameters(Collection<String> simpleParameters) {
		if (simpleParameters == null) {
			this.simpleParameters = Collections.emptyNavigableSet();
		} else {
			TreeSet<String> nparams = new TreeSet<>(simpleParameters);
			nparams.removeAll(MSVCCCompileWorkerTaskFactory.ALWAYS_PRESENT_CL_PARAMETERS);
			this.simpleParameters = nparams;
		}
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public FileLocation getFileLocation() {
		return fileLocation;
	}

	public String getOutFileName() {
		return outFileName;
	}

	public String getLanguage() {
		return language;
	}

	public Set<IncludeDirectoryOption> getIncludeDirectories() {
		return includeDirectories;
	}

	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	public NavigableSet<String> getSimpleParameters() {
		return simpleParameters;
	}

	public void setMacroDefinitions(Map<String, String> macroDefinitions) {
		if (ObjectUtils.isNullOrEmpty(macroDefinitions)) {
			this.macroDefinitions = null;
		} else {
			this.macroDefinitions = macroDefinitions;
		}
	}

	public boolean isCreatePrecompiledHeader() {
		return createPrecompiledHeader;
	}

	public void setCreatePrecompiledHeader(boolean createPrecompiledHeader) {
		this.createPrecompiledHeader = createPrecompiledHeader;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
		out.writeObject(outFileName);
		out.writeObject(language);
		out.writeBoolean(createPrecompiledHeader);
		SerialUtils.writeExternalCollection(out, includeDirectories);
		SerialUtils.writeExternalMap(out, macroDefinitions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
		outFileName = (String) in.readObject();
		language = (String) in.readObject();
		createPrecompiledHeader = in.readBoolean();
		includeDirectories = SerialUtils.readExternalImmutableLinkedHashSet(in);
		macroDefinitions = SerialUtils.readExternalImmutableLinkedHashMap(in);
		simpleParameters = SerialUtils.readExternalSortedImmutableNavigableSet(in);
	}

	@Override
	public int hashCode() {
		//keep hashcode simple
		return ((fileLocation == null) ? 0 : fileLocation.hashCode());
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileCompilationConfiguration other = (FileCompilationConfiguration) obj;
		if (createPrecompiledHeader != other.createPrecompiledHeader)
			return false;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		if (includeDirectories == null) {
			if (other.includeDirectories != null)
				return false;
		} else if (!includeDirectories.equals(other.includeDirectories))
			return false;
		if (language == null) {
			if (other.language != null)
				return false;
		} else if (!language.equals(other.language))
			return false;
		if (macroDefinitions == null) {
			if (other.macroDefinitions != null)
				return false;
		} else if (!macroDefinitions.equals(other.macroDefinitions))
			return false;
		if (outFileName == null) {
			if (other.outFileName != null)
				return false;
		} else if (!outFileName.equals(other.outFileName))
			return false;
		if (simpleParameters == null) {
			if (other.simpleParameters != null)
				return false;
		} else if (!simpleParameters.equals(other.simpleParameters))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileCompilationConfiguration[" + (outFileName != null ? "outFileName=" + outFileName + ", " : "")
				+ (language != null ? "language=" + language : "") + "]";
	}

}