package saker.msvc.main.clink.options;

import java.util.Collection;
import java.util.Map;

import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.sdk.support.main.option.SDKDescriptionTaskOption;

public class SimpleMSVCLinkerOptions implements MSVCLinkerOptions {
	private CompilationIdentifierTaskOption identifier;
	private String architecture;
	private Collection<LinkerInputPassTaskOption> input;
	private Collection<LibraryPathTaskOption> libraryPath;
	private Map<String, SDKDescriptionTaskOption> sdks;
	private Collection<String> simpleParameters;

	public SimpleMSVCLinkerOptions() {
	}

	public SimpleMSVCLinkerOptions(MSVCLinkerOptions copy) {
		this.identifier = ObjectUtils.clone(copy.getIdentifier(), CompilationIdentifierTaskOption::clone);
		this.architecture = copy.getArchitecture();
		this.input = ObjectUtils.cloneArrayList(copy.getLinkerInput(), LinkerInputPassTaskOption::clone);
		this.libraryPath = ObjectUtils.cloneArrayList(copy.getLibraryPath());
		this.sdks = ObjectUtils.cloneTreeMap(copy.getSDKs(), Functionals.identityFunction(),
				SDKDescriptionTaskOption::clone);
		this.simpleParameters = ObjectUtils.cloneArrayList(copy.getSimpleLinkerParameters());
	}

	@Override
	public void accept(MSVCLinkerOptionsVisitor visitor) {
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
	public Collection<LibraryPathTaskOption> getLibraryPath() {
		return libraryPath;
	}

	@Override
	public Map<String, SDKDescriptionTaskOption> getSDKs() {
		return sdks;
	}

	@Override
	public Collection<String> getSimpleLinkerParameters() {
		return simpleParameters;
	}

	@Override
	public MSVCLinkerOptions clone() {
		return this;
	}

}
