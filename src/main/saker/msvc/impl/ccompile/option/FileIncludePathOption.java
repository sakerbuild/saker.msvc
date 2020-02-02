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
package saker.msvc.impl.ccompile.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.std.api.file.location.FileLocation;

public class FileIncludePathOption implements FileIncludePath, IncludePathOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private FileLocation fileLocation;

	/**
	 * For {@link Externalizable}.
	 */
	public FileIncludePathOption() {
	}

	public FileIncludePathOption(FileLocation fileLocation) {
		this.fileLocation = fileLocation;
	}

	@Override
	public void accept(IncludePathVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public FileLocation getFileLocation() {
		return fileLocation;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(fileLocation);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		fileLocation = SerialUtils.readExternalObject(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((fileLocation == null) ? 0 : fileLocation.hashCode());
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
		FileIncludePathOption other = (FileIncludePathOption) obj;
		if (fileLocation == null) {
			if (other.fileLocation != null)
				return false;
		} else if (!fileLocation.equals(other.fileLocation))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (fileLocation != null ? "fileLocation=" + fileLocation : "") + "]";
	}
}
