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

public class FileCompilationProperties implements Externalizable {
	private static final long serialVersionUID = 1L;

	protected FileLocation fileLocation;
	protected String language;
	protected Set<IncludeDirectoryOption> includeDirectories;
	protected Map<String, String> macroDefinitions;
	protected NavigableSet<String> simpleParameters = Collections.emptyNavigableSet();

	/**
	 * For {@link Externalizable}.
	 */
	public FileCompilationProperties() {
	}

	public FileCompilationProperties(FileLocation fileLocation) {
		this.fileLocation = fileLocation;
	}

	public void copyFrom(FileCompilationProperties config) {
		this.language = config.language;
		this.includeDirectories = config.includeDirectories;
		this.macroDefinitions = config.macroDefinitions;
		this.simpleParameters = config.simpleParameters;
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

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
		out.writeObject(language);
		SerialUtils.writeExternalCollection(out, includeDirectories);
		SerialUtils.writeExternalMap(out, macroDefinitions);
		SerialUtils.writeExternalCollection(out, simpleParameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = (FileLocation) in.readObject();
		language = (String) in.readObject();
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
		FileCompilationProperties other = (FileCompilationProperties) obj;
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
		if (simpleParameters == null) {
			if (other.simpleParameters != null)
				return false;
		} else if (!simpleParameters.equals(other.simpleParameters))
			return false;
		return true;
	}

}