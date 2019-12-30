package saker.msvc.impl.clink;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.compiler.utils.api.options.CompilationIdentifier;
import saker.msvc.api.clink.MSVCLinkerWorkerTaskOutput;

public class MSVCLinkerWorkerTaskOutputImpl implements MSVCLinkerWorkerTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier compilationIdentifier;
	private String architecture;
	private SakerPath outputPath;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCLinkerWorkerTaskOutputImpl() {
	}

	public MSVCLinkerWorkerTaskOutputImpl(CompilationIdentifier compilationIdentifier, String architecture,
			SakerPath outputPath) {
		this.compilationIdentifier = compilationIdentifier;
		this.architecture = architecture;
		this.outputPath = outputPath;
	}

	@Override
	public SakerPath getOutputPath() {
		return outputPath;
	}

	@Override
	public String getArchitecture() {
		return architecture;
	}

	@Override
	public CompilationIdentifier getIdentifier() {
		return compilationIdentifier;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(compilationIdentifier);
		out.writeObject(architecture);
		out.writeObject(outputPath);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		compilationIdentifier = (CompilationIdentifier) in.readObject();
		architecture = (String) in.readObject();
		outputPath = (SakerPath) in.readObject();
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "["
				+ (compilationIdentifier != null ? "compilationIdentifier=" + compilationIdentifier + ", " : "")
				+ (architecture != null ? "architecture=" + architecture + ", " : "")
				+ (outputPath != null ? "outputPath=" + outputPath : "") + "]";
	}

}
