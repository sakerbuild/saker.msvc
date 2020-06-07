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

import java.util.Map;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.sdk.support.api.SDKDescription;

/**
 * Provides access to the output of a linking task that was performed using the MSVC toolchain.
 */
public interface MSVCLinkerWorkerTaskOutput {
	/**
	 * Gets the result path of the linking operation.
	 * <p>
	 * The path usually points to an executable or library based on the operation configuration.
	 * 
	 * @return The absolute execution path of the linking result.
	 */
	public SakerPath getOutputPath();

	/**
	 * Gets the output path of the generated <code>.winmd</code> file.
	 * <p>
	 * The result may be <code>null</code> if the linking operation was not explicitly configured to generate Windows
	 * Metadata.
	 * 
	 * @return The absolute execution path to the Windows Metadata file or <code>null</code> if none.
	 * @since saker.msvc 0.8.5
	 */
	public SakerPath getOutputWinmdPath();

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

	/**
	 * Gets the SDKs that were used during the linking.
	 * <p>
	 * The result contains the resolved SDK descriptions with their configuration pinned to the ones that were used
	 * during linking.
	 * 
	 * @return The SDKs.
	 * @since saker.msvc 0.8.5
	 */
	public Map<String, SDKDescription> getSDKs();
}
