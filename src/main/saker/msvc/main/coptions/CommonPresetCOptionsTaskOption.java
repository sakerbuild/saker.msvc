package saker.msvc.main.coptions;

import java.util.Collection;
import java.util.Map;

import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionsPresetTaskOutput;
import saker.msvc.main.ccompile.options.MSVCCompilerOptions;
import saker.msvc.main.ccompile.options.MSVCCompilerOptionsVisitor;
import saker.msvc.main.clink.options.MSVCLinkerOptions;
import saker.msvc.main.clink.options.MSVCLinkerOptionsVisitor;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public final class CommonPresetCOptionsTaskOption implements MSVCLinkerOptions, MSVCCompilerOptions {
	private COptionsPresetTaskOutput preset;

	public CommonPresetCOptionsTaskOption(COptionsPresetTaskOutput preset) {
		this.preset = preset;
	}

	@Override
	public void accept(MSVCLinkerOptionsVisitor visitor) {
		visitor.visit(preset);
	}

	@Override
	public void accept(MSVCCompilerOptionsVisitor visitor) {
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
