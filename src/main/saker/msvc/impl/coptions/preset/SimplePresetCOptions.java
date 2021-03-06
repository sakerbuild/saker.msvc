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
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;
import saker.std.api.file.location.FileLocation;

public final class SimplePresetCOptions implements PresetCOptions, Externalizable, Cloneable {
	private static final long serialVersionUID = 1L;

	private transient String presetIdentifier;

	private CompilationIdentifier identifier;
	private String language;
	private String architecture;

	private List<CompilationPathOption> libraryPaths;
	private List<CompilationPathOption> includeDirectories;
	private NavigableMap<String, SDKDescription> sdks;
	private Map<String, String> macroDefinitions;
	private List<SimpleParameterOption> linkSimpleParameters;
	private Boolean generateWinmd;
	private List<SimpleParameterOption> compileSimpleParameters;
	private FileLocation precompiledHeader;
	private List<CompilationPathOption> forceInclude;
	private Boolean forceIncludePrecompiledHeader;
	private List<CompilationPathOption> forceUsing;
	private List<CompilationPathOption> linkerInput;

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
	public List<CompilationPathOption> getLibraryPath() {
		return libraryPaths;
	}

	@Override
	public List<CompilationPathOption> getIncludeDirectories() {
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
	public List<SimpleParameterOption> getSimpleLinkerParameters() {
		return linkSimpleParameters;
	}

	@Override
	public Boolean getGenerateWinmd() {
		return generateWinmd;
	}

	@Override
	public List<SimpleParameterOption> getSimpleCompilerParameters() {
		return compileSimpleParameters;
	}

	@Override
	public FileLocation getPrecompiledHeader() {
		return precompiledHeader;
	}

	@Override
	public List<CompilationPathOption> getForceInclude() {
		return forceInclude;
	}

	@Override
	public Boolean getForceIncludePrecompiledHeader() {
		return forceIncludePrecompiledHeader;
	}

	@Override
	public List<CompilationPathOption> getForceUsing() {
		return forceUsing;
	}

	@Override
	public List<CompilationPathOption> getLinkerInput() {
		return linkerInput;
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

	public void setLibraryPaths(List<CompilationPathOption> libraryPaths) {
		this.libraryPaths = libraryPaths;
	}

	public void setIncludeDirectories(List<CompilationPathOption> includeDirectories) {
		this.includeDirectories = includeDirectories;
	}

	public void setSDKs(NavigableMap<String, SDKDescription> sdks) {
		ObjectUtils.requireComparator(sdks, SDKSupportUtils.getSDKNameComparator());
		this.sdks = sdks;
	}

	public void setMacroDefinitions(Map<String, String> macroDefinitions) {
		this.macroDefinitions = macroDefinitions;
	}

	public void setLinkSimpleParameters(List<SimpleParameterOption> simpleParameters) {
		this.linkSimpleParameters = simpleParameters;
	}

	public void setGenerateWinmd(Boolean generateWinmd) {
		this.generateWinmd = generateWinmd;
	}

	public void setCompileSimpleParameters(List<SimpleParameterOption> compileSimpleParameters) {
		this.compileSimpleParameters = compileSimpleParameters;
	}

	public void setPrecompiledHeader(FileLocation precompiledHeader) {
		this.precompiledHeader = precompiledHeader;
	}

	public void setForceInclude(List<CompilationPathOption> forceInclude) {
		this.forceInclude = forceInclude;
	}

	public void setForceIncludePrecompiledHeader(Boolean forceIncludePrecompiledHeader) {
		this.forceIncludePrecompiledHeader = forceIncludePrecompiledHeader;
	}

	public void setForceUsing(List<CompilationPathOption> forceUsing) {
		this.forceUsing = forceUsing;
	}

	public void setLinkerInput(List<CompilationPathOption> linkerInput) {
		this.linkerInput = linkerInput;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(presetIdentifier);
		out.writeObject(identifier);
		out.writeObject(language);
		out.writeObject(architecture);
		out.writeObject(precompiledHeader);
		out.writeObject(forceIncludePrecompiledHeader);
		out.writeObject(generateWinmd);
		SerialUtils.writeExternalCollection(out, libraryPaths);
		SerialUtils.writeExternalCollection(out, includeDirectories);
		SerialUtils.writeExternalMap(out, sdks);
		SerialUtils.writeExternalMap(out, macroDefinitions);
		SerialUtils.writeExternalCollection(out, linkSimpleParameters);
		SerialUtils.writeExternalCollection(out, compileSimpleParameters);
		SerialUtils.writeExternalCollection(out, forceInclude);
		SerialUtils.writeExternalCollection(out, forceUsing);
		SerialUtils.writeExternalCollection(out, linkerInput);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		presetIdentifier = (String) in.readObject();
		identifier = (CompilationIdentifier) in.readObject();
		language = (String) in.readObject();
		architecture = (String) in.readObject();
		precompiledHeader = (FileLocation) in.readObject();
		forceIncludePrecompiledHeader = (Boolean) in.readObject();
		generateWinmd = (Boolean) in.readObject();
		libraryPaths = SerialUtils.readExternalImmutableList(in);
		includeDirectories = SerialUtils.readExternalImmutableList(in);
		sdks = SerialUtils.readExternalSortedImmutableNavigableMap(in, SDKSupportUtils.getSDKNameComparator());
		macroDefinitions = SerialUtils.readExternalImmutableLinkedHashMap(in);
		linkSimpleParameters = SerialUtils.readExternalImmutableList(in);
		compileSimpleParameters = SerialUtils.readExternalImmutableList(in);
		forceInclude = SerialUtils.readExternalImmutableList(in);
		forceUsing = SerialUtils.readExternalImmutableList(in);
		linkerInput = SerialUtils.readExternalImmutableList(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((architecture == null) ? 0 : architecture.hashCode());
		result = prime * result + ((compileSimpleParameters == null) ? 0 : compileSimpleParameters.hashCode());
		result = prime * result + ((forceInclude == null) ? 0 : forceInclude.hashCode());
		result = prime * result
				+ ((forceIncludePrecompiledHeader == null) ? 0 : forceIncludePrecompiledHeader.hashCode());
		result = prime * result + ((forceUsing == null) ? 0 : forceUsing.hashCode());
		result = prime * result + ((generateWinmd == null) ? 0 : generateWinmd.hashCode());
		result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
		result = prime * result + ((includeDirectories == null) ? 0 : includeDirectories.hashCode());
		result = prime * result + ((language == null) ? 0 : language.hashCode());
		result = prime * result + ((libraryPaths == null) ? 0 : libraryPaths.hashCode());
		result = prime * result + ((linkSimpleParameters == null) ? 0 : linkSimpleParameters.hashCode());
		result = prime * result + ((linkerInput == null) ? 0 : linkerInput.hashCode());
		result = prime * result + ((macroDefinitions == null) ? 0 : macroDefinitions.hashCode());
		result = prime * result + ((precompiledHeader == null) ? 0 : precompiledHeader.hashCode());
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
		if (forceInclude == null) {
			if (other.forceInclude != null)
				return false;
		} else if (!forceInclude.equals(other.forceInclude))
			return false;
		if (forceIncludePrecompiledHeader == null) {
			if (other.forceIncludePrecompiledHeader != null)
				return false;
		} else if (!forceIncludePrecompiledHeader.equals(other.forceIncludePrecompiledHeader))
			return false;
		if (forceUsing == null) {
			if (other.forceUsing != null)
				return false;
		} else if (!forceUsing.equals(other.forceUsing))
			return false;
		if (generateWinmd == null) {
			if (other.generateWinmd != null)
				return false;
		} else if (!generateWinmd.equals(other.generateWinmd))
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
		if (linkerInput == null) {
			if (other.linkerInput != null)
				return false;
		} else if (!linkerInput.equals(other.linkerInput))
			return false;
		if (macroDefinitions == null) {
			if (other.macroDefinitions != null)
				return false;
		} else if (!macroDefinitions.equals(other.macroDefinitions))
			return false;
		if (precompiledHeader == null) {
			if (other.precompiledHeader != null)
				return false;
		} else if (!precompiledHeader.equals(other.precompiledHeader))
			return false;
		if (sdks == null) {
			if (other.sdks != null)
				return false;
		} else if (!sdks.equals(other.sdks))
			return false;
		return true;
	}

}
