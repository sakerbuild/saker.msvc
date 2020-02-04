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
package saker.msvc.main.coptions;

import java.util.Collection;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.main.ccompile.options.MSVCCompilerOptions;
import saker.msvc.main.clink.options.MSVCLinkerOptions;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public final class CommonPresetCOptionsTaskOption implements MSVCLinkerOptions, MSVCCompilerOptions {
	private COptionsPresetTaskOutput preset;

	public CommonPresetCOptionsTaskOption(COptionsPresetTaskOutput preset) {
		this.preset = preset;
	}

	@Override
	public void accept(MSVCCompilerOptions.Visitor visitor) {
		visitor.visit(preset);
	}

	@Override
	public void accept(MSVCLinkerOptions.Visitor visitor) {
		visitor.visit(preset);
	}

	@Override
	public CompilationIdentifierTaskOption getIdentifier() {
		return null;
	}

	@Override
	public String getLanguage() {
		return null;
	}

	@Override
	public String getArchitecture() {
		return null;
	}

	@Override
	public CommonPresetCOptionsTaskOption clone() {
		return this;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return null;
	}

	@Override
	public Collection<String> getSimpleCompilerParameters() {
		return null;
	}
}
