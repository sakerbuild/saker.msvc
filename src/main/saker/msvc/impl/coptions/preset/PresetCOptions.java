package saker.msvc.impl.coptions.preset;

import java.util.Map;
import java.util.Set;

import saker.compiler.utils.api.options.CompilationIdentifier;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.sdk.support.api.SDKDescription;

public interface PresetCOptions {
	//transient, no real meaning 
	public String getPresetIdentifier();

	//for option merging
	public CompilationIdentifier getIdentifier();

	//for option merging
	public String getLanguage();

	//for option merging
	public String getArchitecture();

	public Set<IncludeDirectoryOption> getIncludeDirectories();

	public Map<String, SDKDescription> getSDKs();

	public Map<String, String> getMacroDefinitions();

	public Set<String> getSimpleCompilerParameters();

	public Set<LibraryPathOption> getLibraryPath();

	public Set<String> getSimpleLinkerParameters();

}
