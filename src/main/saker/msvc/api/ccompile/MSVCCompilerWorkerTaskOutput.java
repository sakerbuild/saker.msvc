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
