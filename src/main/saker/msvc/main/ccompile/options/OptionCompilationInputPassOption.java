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
import java.util.Map;

import saker.build.task.TaskContext;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.std.main.file.option.FileLocationTaskOption;
import saker.std.main.file.option.MultiFileLocationTaskOption;

public class OptionCompilationInputPassOption
		implements OptionCompilationInputPass, CompilationInputPassOption, CompilationInputPassTaskOption {
	private Collection<MultiFileLocationTaskOption> files;
	private Collection<IncludeDirectoryTaskOption> includeDirectories;
	private CompilationIdentifierTaskOption subIdentifier;
	private Map<String, String> macroDefinitions;
	private Collection<String> simpleParameters;
	private Collection<MSVCCompilerOptions> compilerOptions;
	private String language;
	private FileLocationTaskOption precompiledHeader;

	public OptionCompilationInputPassOption(CompilationInputPassTaskOption copy) {
		this.files = ObjectUtils.cloneArrayList(copy.getFiles(), MultiFileLocationTaskOption::clone);
		this.includeDirectories = ObjectUtils.cloneArrayList(copy.getIncludeDirectories(),
				IncludeDirectoryTaskOption::clone);
		this.subIdentifier = ObjectUtils.clone(copy.getSubIdentifier(), CompilationIdentifierTaskOption::clone);
		this.macroDefinitions = ImmutableUtils.makeImmutableNavigableMap(copy.getMacroDefinitions());
		this.simpleParameters = ImmutableUtils.makeImmutableList(copy.getSimpleParameters());
		this.compilerOptions = ObjectUtils.cloneArrayList(copy.getCompilerOptions(), MSVCCompilerOptions::clone);
		this.language = copy.getLanguage();
		this.precompiledHeader = ObjectUtils.clone(copy.getPrecompiledHeader(), FileLocationTaskOption::clone);
	}

	@Override
	public void accept(CompilationInputPassOptionVisitor visitor) {
		visitor.visit(this);
	}

	@Override
	public CompilationInputPassTaskOption clone() {
		return this;
	}

	@Override
	public Collection<MultiFileLocationTaskOption> getFiles() {
		return files;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public Collection<IncludeDirectoryTaskOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public CompilationIdentifierTaskOption getSubIdentifier() {
		return subIdentifier;
	}

	@Override
	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	@Override
	public Collection<String> getSimpleParameters() {
		return simpleParameters;
	}

	@Override
	public Collection<MSVCCompilerOptions> getCompilerOptions() {
		return compilerOptions;
	}

	@Override
	public CompilationInputPassOption toCompilationInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public FileLocationTaskOption getPrecompiledHeader() {
		return precompiledHeader;
	}
}
