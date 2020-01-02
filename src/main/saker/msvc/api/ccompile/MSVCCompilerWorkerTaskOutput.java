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
package saker.msvc.api.ccompile;

import java.util.Collection;
import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.options.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;

/**
 * Provides access to the output of a C/C++ compilation task.
 * <p>
 * The interface provides access to the results of a C/C++ compilation that was done using the MSVC toolchain.
 */
public interface MSVCCompilerWorkerTaskOutput {
	/**
	 * Gets the collection of execution paths that point to the resulting object files.
	 * <p>
	 * Each path is a result of a single source file compilation.
	 * 
	 * @return An immutable collection of object file paths.
	 */
	public Collection<SakerPath> getObjectFilePaths();

	/**
	 * Gets the target architecture that was the compilation done for.
	 * 
	 * @return The target architecture.
	 */
	public String getArchitecture();

	/**
	 * Gets the compilation identifier of the compilation task.
	 * 
	 * @return The identifier.
	 */
	public CompilationIdentifier getIdentifier();

	/**
	 * Gets the SDKs that were used during the compilation.
	 * <p>
	 * The result contains all SDKs that were specified by the user, even if they weren't actually used for the
	 * compilation.
	 * 
	 * @return The SDKs.
	 */
	public Map<String, SDKDescription> getSDKs();
}
