package saker.msvc.api.clink;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.options.CompilationIdentifier;

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
