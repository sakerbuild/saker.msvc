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
package saker.msvc.impl.coptions.preset;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;

public final class SimplePresetCOptions implements PresetCOptions, Externalizable, Cloneable {
	private static final long serialVersionUID = 1L;

	private transient String presetIdentifier;

	private CompilationIdentifier identifier;
	private String language;
	private String architecture;

	private Set<LibraryPathOption> libraryPaths;
	private Set<IncludeDirectoryOption> includeDirectories;
	private NavigableMap<String, SDKDescription> sdks;
	private Map<String, String> macroDefinitions;
	private Set<String> linkSimpleParameters;
	private Set<String> compileSimpleParameters;

	/**
	 * For {@link Externalizable}.
	 */
	public SimplePresetCOptions() {
	}

	@Override
	public SimplePresetCOptions clone() {
		try {
			return (SimplePresetCOptions) super.clone();
		} catch (CloneNotSupportedException e) {
			throw new AssertionError(e);
		}
	}

	@Override
	public String getPresetIdentifier() {
		return presetIdentifier;
	}

	@Override
	public CompilationIdentifier getIdentifier() {
		return identifier;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public String getArchitecture() {
		return architecture;
	}

	@Override
	public Set<LibraryPathOption> getLibraryPath() {
		return libraryPaths;
	}

	@Override
	public Set<IncludeDirectoryOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public Map<String, SDKDescription> getSDKs() {
		return sdks;
	}

	@Override
	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	@Override
	public Set<String> getSimpleLinkerParameters() {
		return linkSimpleParameters;
	}

	@Override
	public Set<String> getSimpleCompilerParameters() {
		return compileSimpleParameters;
	}

	public void setPresetIdentifier(String presetIdentifier) {
		this.presetIdentifier = presetIdentifier;
	}

	public void setIdentifier(CompilationIdentifier identifier) {
		this.identifier = identifier;
	}

	public void setLanguage(String language) {
		this.language = language;
	}

	public void setArchitecture(String architecture) {
		this.architecture = architecture;
	}

	public void setLibraryPaths(Set<LibraryPathOption> libraryPaths) {
		this.libraryPaths = libraryPaths;
	}

	public void setIncludeDirectories(Set<IncludeDirectoryOption> includeDirectories) {
		this.includeDirectories = includeDirectories;
	}

	public void setSdks(NavigableMap<String, SDKDescription> sdks) {
		ObjectUtils.requireComparator(sdks, SDKSupportUtils.getSDKNameComparator());
		this.sdks = sdks;
	}

	public void setMacroDefinitions(Map<String, String> macroDefinitions) {
		this.macroDefinitions = macroDefinitions;
	}

	public void setLinkSimpleParameters(Set<String> simpleParameters) {
		this.linkSimpleParameters = simpleParameters;
	}

	public void setCompileSimpleParameters(Set<String> compileSimpleParameters) {
		this.compileSimpleParameters = compileSimpleParameters;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(presetIdentifier);
		out.writeObject(identifier);
		out.writeObject(language);
		out.writeObject(architecture);
		SerialUtils.writeExternalCollection(out, libraryPaths);
		SerialUtils.writeExternalCollection(out, includeDirectories);
		SerialUtils.writeExternalMap(out, sdks);
		SerialUtils.writeExternalMap(out, macroDefinitions);
		SerialUtils.writeExternalCollection(out, linkSimpleParameters);
		SerialUtils.writeExternalCollection(out, compileSimpleParameters);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		presetIdentifier = (String) in.readObject();
		identifier = (CompilationIdentifier) in.readObject();
		language = (String) in.readObject();
		architecture = (String) in.readObject();
		libraryPaths = SerialUtils.readExternalImmutableLinkedHashSet(in);
		includeDirectories = SerialUtils.readExternalImmutableLinkedHashSet(in);
		sdks = SerialUtils.readExternalSortedImmutableNavigableMap(in, SDKSupportUtils.getSDKNameComparator());
		macroDefinitions = SerialUtils.readExternalImmutableLinkedHashMap(in);
		linkSimpleParameters = SerialUtils.readExternalImmutableNavigableSet(in);
		compileSimpleParameters = SerialUtils.readExternalImmutableNavigableSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((architecture == null) ? 0 : architecture.hashCode());
		result = prime * result + ((compileSimpleParameters == null) ? 0 : compileSimpleParameters.hashCode());
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result + ((includeDirectories == null) ? 0 : includeDirectories.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((libraryPaths == null) ? 0 : libraryPaths.hashCode());
		result = prime * result + ((linkSimpleParameters == null) ? 0 : linkSimpleParameters.hashCode());
		result = prime * result + ((macroDefinitions == null) ? 0 : macroDefinitions.hashCode());
		result = prime * result + ((sdks == null) ? 0 : sdks.hashCode());
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
		SimplePresetCOptions other = (SimplePresetCOptions) obj;
		if (architecture == null) {
			if (other.architecture != null)
				return false;
		} else if (!architecture.equals(other.architecture))
			return false;
		if (compileSimpleParameters == null) {
			if (other.compileSimpleParameters != null)
				return false;
		} else if (!compileSimpleParameters.equals(other.compileSimpleParameters))
			return false;
		if (identifier == null) {
			if (other.identifier != null)
				return false;
		} else if (!identifier.equals(other.identifier))
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
		if (libraryPaths == null) {
			if (other.libraryPaths != null)
				return false;
		} else if (!libraryPaths.equals(other.libraryPaths))
			return false;
		if (linkSimpleParameters == null) {
			if (other.linkSimpleParameters != null)
				return false;
		} else if (!linkSimpleParameters.equals(other.linkSimpleParameters))
			return false;
		if (macroDefinitions == null) {
			if (other.macroDefinitions != null)
				return false;
		} else if (!macroDefinitions.equals(other.macroDefinitions))
			return false;
		if (sdks == null) {
			if (other.sdks != null)
				return false;
		} else if (!sdks.equals(other.sdks))
			return false;
		return true;
	}

}
