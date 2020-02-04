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
import java.util.Collections;

import saker.build.file.path.SakerPath;
import saker.build.task.TaskContext;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.util.option.CommonFilePathOption;
import saker.std.api.file.location.ExecutionFileLocation;

final class RelativePathLibraryPathTaskOption implements LibraryPathTaskOption {
	private final SakerPath path;

	public RelativePathLibraryPathTaskOption(SakerPath path) {
		this.path = path;
	}

	@Override
	public LibraryPathTaskOption clone() {
		return this;
	}

	@Override
	public Collection<LibraryPathOption> toLibraryPath(TaskContext taskcontext) {
		return Collections.singleton(new CommonFilePathOption(
				ExecutionFileLocation.create(taskcontext.getTaskWorkingDirectoryPath().resolve(path))));
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
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
		RelativePathLibraryPathTaskOption other = (RelativePathLibraryPathTaskOption) obj;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		return true;
	}

}