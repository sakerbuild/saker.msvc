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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map;
import java.util.NavigableMap;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.api.clink.MSVCLinkerWorkerTaskOutput;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKSupportUtils;

public class MSVCLinkerWorkerTaskOutputImpl implements MSVCLinkerWorkerTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier compilationIdentifier;
	private String architecture;
	private SakerPath outputPath;
	private SakerPath outputWinmdPath;
	private NavigableMap<String, SDKDescription> sdkDescriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCLinkerWorkerTaskOutputImpl() {
	}

	public MSVCLinkerWorkerTaskOutputImpl(CompilationIdentifier compilationIdentifier, String architecture,
			SakerPath outputPath, SakerPath outputWinmdPath, NavigableMap<String, SDKDescription> sdkDescriptions) {
		this.compilationIdentifier = compilationIdentifier;
		this.architecture = architecture;
		this.outputPath = outputPath;
		this.outputWinmdPath = outputWinmdPath;
		this.sdkDescriptions = ImmutableUtils.unmodifiableNavigableMap(sdkDescriptions);
	}

	@Override
	public SakerPath getOutputPath() {
		return outputPath;
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
	public SakerPath getOutputWinmdPath() {
		return outputWinmdPath;
	}

	@Override
	public Map<String, SDKDescription> getSDKs() {
		return sdkDescriptions;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(compilationIdentifier);
		out.writeObject(architecture);
		out.writeObject(outputPath);
		out.writeObject(outputWinmdPath);
		SerialUtils.writeExternalMap(out, sdkDescriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		compilationIdentifier = (CompilationIdentifier) in.readObject();
		architecture = (String) in.readObject();
		outputPath = (SakerPath) in.readObject();
		outputWinmdPath = (SakerPath) in.readObject();
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
		MSVCLinkerWorkerTaskOutputImpl other = (MSVCLinkerWorkerTaskOutputImpl) obj;
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
		if (outputPath == null) {
			if (other.outputPath != null)
				return false;
		} else if (!outputPath.equals(other.outputPath))
			return false;
		if (outputWinmdPath == null) {
			if (other.outputWinmdPath != null)
				return false;
		} else if (!outputWinmdPath.equals(other.outputWinmdPath))
			return false;
		if (sdkDescriptions == null) {
			if (other.sdkDescriptions != null)
				return false;
		} else if (!sdkDescriptions.equals(other.sdkDescriptions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ (compilationIdentifier != null ? "compilationIdentifier=" + compilationIdentifier + ", " : "")
				+ (architecture != null ? "architecture=" + architecture + ", " : "")
				+ (outputPath != null ? "outputPath=" + outputPath + ", " : "")
				+ (outputWinmdPath != null ? "outputWinmdPath=" + outputWinmdPath : "") + "]";
	}

}
