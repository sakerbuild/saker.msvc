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
package saker.msvc.api.clink;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;

/**
 * Provides access to the output of a linking task that was performed using the MSVC toolchain.
 */
public interface MSVCLinkerWorkerTaskOutput {
	/**
	 * Gets the result path of the linking operation.
	 * <p>
	 * The path usually points to an executable or library based on the operation configuration.
	 * 
	 * @return The execution path of the linking result.
	 */
	public SakerPath getOutputPath();

	/**
	 * Gets the target architecture that was the compilation done for.
	 * 
	 * @return The target architecture.
	 */
	public String getArchitecture();

	/**
	 * Gets the identifier for the linker task.
	 * 
	 * @return The identifier.
	 */
	public CompilationIdentifier getIdentifier();
}
