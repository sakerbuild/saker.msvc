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

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.main.options.CompilationPathTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class SimpleMSVCCCompilerOptions implements MSVCCompilerOptions {
	private CompilationIdentifierTaskOption identifier;
	private String language;
	private Collection<CompilationPathTaskOption> includeDirectories;
	private String architecture;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private Map<String, String> macroDefinitions;
	private Collection<String> simpleParameters;

	public SimpleMSVCCCompilerOptions() {
		super();
	}

	public SimpleMSVCCCompilerOptions(MSVCCompilerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.language = copy.getLanguage();
		this.includeDirectories = ObjectUtils.cloneArrayList(copy.getIncludeDirectories(),
				CompilationPathTaskOption::clone);
		this.architecture = copy.getArchitecture();
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.macroDefinitions = ObjectUtils.clone(copy.getMacroDefinitions(),
				ImmutableUtils::makeImmutableLinkedHashMap);
		this.simpleParameters = ImmutableUtils.makeImmutableList(copy.getSimpleCompilerParameters());
	}

	@Override
	public MSVCCompilerOptions clone() {
		return this;
	}

	@Override
	public CompilationIdentifierTaskOption getIdentifier() {
		return identifier;
	}

	@Override
	public String getLanguage() {
		return language;
	}

	@Override
	public Collection<CompilationPathTaskOption> getIncludeDirectories() {
		return includeDirectories;
	}

	@Override
	public String getArchitecture() {
		return architecture;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return sdks;
	}

	@Override
	public Map<String, String> getMacroDefinitions() {
		return macroDefinitions;
	}

	@Override
	public Collection<String> getSimpleCompilerParameters() {
		return simpleParameters;
	}

	@Override
	public void accept(Visitor visitor) {
		visitor.visit(this);
	}

}
