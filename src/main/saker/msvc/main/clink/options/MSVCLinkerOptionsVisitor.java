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

import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;

public interface MSVCLinkerOptionsVisitor {
	public default void visit(MSVCLinkerOptions options) {
		throw new UnsupportedOperationException("Unsupported linker options: " + options);
	}

	public default void visit(COptionsPresetTaskOutput options) {
		throw new UnsupportedOperationException("Unsupported linker options: " + options);
	}
}
