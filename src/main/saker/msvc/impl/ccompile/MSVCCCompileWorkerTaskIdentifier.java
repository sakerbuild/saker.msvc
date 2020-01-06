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
import java.util.Objects;

import saker.build.task.identifier.TaskIdentifier;
import saker.compiler.utils.api.CompilationIdentifier;

public class MSVCCCompileWorkerTaskIdentifier implements TaskIdentifier, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier passIdentifier;
	private String architecture;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCCompileWorkerTaskIdentifier() {
	}

	public MSVCCCompileWorkerTaskIdentifier(CompilationIdentifier passIdentifier, String architecture) {
		Objects.requireNonNull(passIdentifier, "pass identifier");
		Objects.requireNonNull(architecture, "architecture");
		this.passIdentifier = passIdentifier;
		this.architecture = architecture;
	}

	public CompilationIdentifier getPassIdentifier() {
		return passIdentifier;
	}

	public String getArchitecture() {
		return architecture;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(passIdentifier);
		out.writeObject(architecture);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		passIdentifier = (CompilationIdentifier) in.readObject();
		architecture = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((architecture == null) ? 0 : architecture.hashCode());
		result = prime * result + ((passIdentifier == null) ? 0 : passIdentifier.hashCode());
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
		MSVCCCompileWorkerTaskIdentifier other = (MSVCCCompileWorkerTaskIdentifier) obj;
		if (architecture == null) {
			if (other.architecture != null)
				return false;
		} else if (!architecture.equals(other.architecture))
			return false;
		if (passIdentifier == null) {
			if (other.passIdentifier != null)
				return false;
		} else if (!passIdentifier.equals(other.passIdentifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + passIdentifier + "/" + architecture + "]";
	}

}
