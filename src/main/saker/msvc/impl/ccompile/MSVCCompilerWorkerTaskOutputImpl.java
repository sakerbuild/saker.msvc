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
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.api.ccompile.MSVCCompilerWorkerTaskOutput;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;

public class MSVCCompilerWorkerTaskOutputImpl implements MSVCCompilerWorkerTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier compilationIdentifier;
	private String architecture;
	private NavigableSet<SakerPath> objectFilePaths;
	private NavigableMap<String, SDKDescription> sdkDescriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCompilerWorkerTaskOutputImpl() {
	}

	public MSVCCompilerWorkerTaskOutputImpl(CompilationIdentifier compilationIdentifier, String architecture,
			NavigableMap<String, SDKDescription> sdkDescriptions) {
		this.compilationIdentifier = compilationIdentifier;
		this.architecture = architecture;
		this.sdkDescriptions = ImmutableUtils.unmodifiableNavigableMap(sdkDescriptions);
	}

	public void setObjectFilePaths(NavigableSet<SakerPath> objectFilePaths) {
		this.objectFilePaths = objectFilePaths;
	}

	@Override
	public Collection<SakerPath> getObjectFilePaths() {
		return objectFilePaths;
	}

	@Override
	public String getArchitecture() {
		return architecture;
	}

	@Override
	public CompilationIdentifier getIdentifier() {
		return compilationIdentifier;
	}

	@Override
	public Map<String, SDKDescription> getSDKs() {
		return sdkDescriptions;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, objectFilePaths);
		out.writeObject(architecture);
		out.writeObject(compilationIdentifier);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		objectFilePaths = SerialUtils.readExternalSortedImmutableNavigableSet(in);
		architecture = (String) in.readObject();
		compilationIdentifier = (CompilationIdentifier) in.readObject();
		sdkDescriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in,
				SDKSupportUtils.getSDKNameComparator());
	}

	@Override
	public int hashCode() {
		return (compilationIdentifier == null) ? 0 : compilationIdentifier.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MSVCCompilerWorkerTaskOutputImpl other = (MSVCCompilerWorkerTaskOutputImpl) obj;
		if (architecture == null) {
			if (other.architecture != null)
				return false;
		} else if (!architecture.equals(other.architecture))
			return false;
		if (compilationIdentifier == null) {
			if (other.compilationIdentifier != null)
				return false;
		} else if (!compilationIdentifier.equals(other.compilationIdentifier))
			return false;
		if (objectFilePaths == null) {
			if (other.objectFilePaths != null)
				return false;
		} else if (!objectFilePaths.equals(other.objectFilePaths))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		return true;
	}

}
