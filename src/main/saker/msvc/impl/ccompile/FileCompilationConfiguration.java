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

import saker.std.api.file.location.FileLocation;

public class FileCompilationConfiguration extends FileCompilationProperties {
	private static final long serialVersionUID = 1L;

	private String outFileName;
	//TODO the precompiled headers don't need full configuration as their options are the same as this
	private FileCompilationConfiguration precompiledHeader;

	/**
	 * For {@link Externalizable}.
	 */
	public FileCompilationConfiguration() {
	}

	public FileCompilationConfiguration(FileLocation fileLocation, String outFileName) {
		super(fileLocation);
		this.outFileName = outFileName;
	}

	/**
	 * Returns the base output file name.
	 */
	public String getOutFileName() {
		return outFileName;
	}

	public FileCompilationConfiguration getPrecompiledHeader() {
		return precompiledHeader;
	}

	public void setPrecompiledHeader(FileCompilationConfiguration precompiledHeader) {
		this.precompiledHeader = precompiledHeader;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeObject(outFileName);
		out.writeObject(precompiledHeader);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		super.readExternal(in);
		outFileName = (String) in.readObject();
		precompiledHeader = (FileCompilationConfiguration) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + ((outFileName == null) ? 0 : outFileName.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!super.equals(obj))
			return false;
		if (getClass() != obj.getClass())
			return false;
		FileCompilationConfiguration other = (FileCompilationConfiguration) obj;
		if (outFileName == null) {
			if (other.outFileName != null)
				return false;
		} else if (!outFileName.equals(other.outFileName))
			return false;
		if (precompiledHeader == null) {
			if (other.precompiledHeader != null)
				return false;
		} else if (!precompiledHeader.equals(other.precompiledHeader))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FileCompilationConfiguration[" + (outFileName != null ? "outFileName=" + outFileName + ", " : "")
				+ (language != null ? "language=" + language : "") + "]";
	}

}