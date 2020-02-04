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
package saker.msvc.main.clink.options;

import java.util.Collection;

import saker.build.task.TaskContext;
import saker.msvc.impl.option.CompilationPathOption;

public class SimpleLibraryPathTaskOption implements LibraryPathTaskOption {

	private Collection<CompilationPathOption> libraryPath;

	public SimpleLibraryPathTaskOption(Collection<CompilationPathOption> libraryPath) {
		this.libraryPath = libraryPath;
	}

	@Override
	public LibraryPathTaskOption clone() {
		return this;
	}

	@Override
	public Collection<CompilationPathOption> toLibraryPath(TaskContext taskcontext) {
		return libraryPath;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((libraryPath == null) ? 0 : libraryPath.hashCode());
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
		SimpleLibraryPathTaskOption other = (SimpleLibraryPathTaskOption) obj;
		if (libraryPath == null) {
			if (other.libraryPath != null)
				return false;
		} else if (!libraryPath.equals(other.libraryPath))
			return false;
		return true;
	}

}
