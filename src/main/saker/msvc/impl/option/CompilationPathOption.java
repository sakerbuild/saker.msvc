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
package saker.msvc.impl.option;

import saker.sdk.support.api.SDKPathReference;

public interface CompilationPathOption {
	public void accept(Visitor visitor);

	@Override
	public boolean equals(Object obj);

	@Override
	public int hashCode();

	public interface Visitor {
		public default void visit(FileCompilationPathOption includepath) {
			throw new UnsupportedOperationException("Unsupported include path: " + includepath);
		}

		public default void visit(SDKPathReference includepath) {
			throw new UnsupportedOperationException("Unsupported include path: " + includepath);
		}
	}

}
