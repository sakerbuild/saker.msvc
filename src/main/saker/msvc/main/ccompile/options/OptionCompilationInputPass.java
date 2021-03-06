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
package saker.msvc.main.ccompile.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.msvc.main.options.SimpleParameterTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public interface OptionCompilationInputPass {
	public Collection<MultiFileLocationTaskOption> getFiles();

	public String getLanguage();

	public Collection<CompilationPathTaskOption> getIncludeDirectories();

	public CompilationIdentifierTaskOption getSubIdentifier();

	public Map<String, String> getMacroDefinitions();

	public List<SimpleParameterTaskOption> getSimpleParameters();

	public Collection<MSVCCompilerOptions> getCompilerOptions();

	public FileLocationTaskOption getPrecompiledHeader();

	public Collection<CompilationPathTaskOption> getForceInclude();

	public Boolean getForceIncludePrecompiledHeader();

	public Collection<CompilationPathTaskOption> getForceUsing();
}
