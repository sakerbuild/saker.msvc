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
package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.msvc.main.options.SimpleParameterTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class SimpleMSVCLinkerOptions implements MSVCLinkerOptions {
	private CompilationIdentifierTaskOption identifier;
	private String architecture;
	private Collection<LinkerInputPassTaskOption> input;
	private Collection<CompilationPathTaskOption> libraryPath;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private List<SimpleParameterTaskOption> simpleParameters;
	private Boolean generateWinmd;
	private String binaryName;

	public SimpleMSVCLinkerOptions() {
	}

	public SimpleMSVCLinkerOptions(MSVCLinkerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.architecture = copy.getArchitecture();
		this.input = ObjectUtils.cloneArrayList(copy.getLinkerInput(), LinkerInputPassTaskOption::clone);
		this.libraryPath = ObjectUtils.cloneArrayList(copy.getLibraryPath(), CompilationPathTaskOption::clone);
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.simpleParameters = ObjectUtils.cloneArrayList(copy.getSimpleLinkerParameters());
		this.generateWinmd = copy.getGenerateWinmd();
		this.binaryName = copy.getBinaryName();
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

	@Override
	public CompilationIdentifierTaskOption getIdentifier() {
		return identifier;
	}

	@Override
	public Collection<LinkerInputPassTaskOption> getLinkerInput() {
		return input;
	}

	@Override
	public String getArchitecture() {
		return architecture;
	}

	@Override
	public Collection<CompilationPathTaskOption> getLibraryPath() {
		return libraryPath;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return sdks;
	}

	@Override
	public List<SimpleParameterTaskOption> getSimpleLinkerParameters() {
		return simpleParameters;
	}

	@Override
	public Boolean getGenerateWinmd() {
		return generateWinmd;
	}

	@Override
	public String getBinaryName() {
		return binaryName;
	}

	@Override
	public MSVCLinkerOptions clone() {
		return this;
	}

}
