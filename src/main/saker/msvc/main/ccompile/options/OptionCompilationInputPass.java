package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public interface OptionCompilationInputPass {
	public Collection<MultiFileLocationTaskOption> getFiles();

	public String getLanguage();

	public Collection<IncludeDirectoryTaskOption> getIncludeDirectories();

	public CompilationIdentifierTaskOption getSubIdentifier();

	public Map<String, String> getMacroDefinitions();

	public Collection<String> getSimpleParameters();

	public Collection<MSVCCompilerOptions> getCompilerOptions();
}
