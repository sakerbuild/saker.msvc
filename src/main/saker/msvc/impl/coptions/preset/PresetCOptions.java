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
package saker.msvc.impl.coptions.preset;

import java.util.List;
import java.util.Map;

import saker.compiler.utils.api.CompilationIdentifier;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.sdk.support.api.SDKDescription;
import saker.std.api.file.location.FileLocation;

public interface PresetCOptions {
	//transient, no real meaning 
	public String getPresetIdentifier();

	//for option merging
	public CompilationIdentifier getIdentifier();

	//for option merging
	public String getLanguage();

	//for option merging
	public String getArchitecture();

	public List<CompilationPathOption> getIncludeDirectories();

	public Map<String, SDKDescription> getSDKs();

	public Map<String, String> getMacroDefinitions();

	public List<SimpleParameterOption> getSimpleCompilerParameters();

	public List<CompilationPathOption> getLibraryPath();

	public List<SimpleParameterOption> getSimpleLinkerParameters();

	public Boolean getGenerateWinmd();

	public FileLocation getPrecompiledHeader();

	public List<CompilationPathOption> getForceInclude();

	public Boolean getForceIncludePrecompiledHeader();

	public List<CompilationPathOption> getForceUsing();

	public List<CompilationPathOption> getLinkerInput();
}
