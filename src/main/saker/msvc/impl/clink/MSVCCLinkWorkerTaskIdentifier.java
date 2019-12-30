package saker.msvc.impl.clink;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Objects;

import saker.build.task.identifier.TaskIdentifier;
import saker.compiler.utils.api.options.CompilationIdentifier;

public class MSVCCLinkWorkerTaskIdentifier implements TaskIdentifier, Externalizable {
	private static final long serialVersionUID = 1L;

	private CompilationIdentifier passIdentifier;
	private String architecture;

	/**
	 * For {@link Externalizable}.
	 */
	public MSVCCLinkWorkerTaskIdentifier() {
	}

	public MSVCCLinkWorkerTaskIdentifier(CompilationIdentifier passIdentifier, String architecture) {
		Objects.requireNonNull(passIdentifier, "pass identifier");
		Objects.requireNonNull(architecture, "architecture");
		this.passIdentifier = passIdentifier;
		this.architecture = architecture;
	}

	public CompilationIdentifier getPassIdentifier() {
		return passIdentifier;
	}

	public String getArchitecture() {
		return architecture;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(passIdentifier);
		out.writeObject(architecture);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		passIdentifier = (CompilationIdentifier) in.readObject();
		architecture = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((architecture == null) ? 0 : architecture.hashCode());
		result = prime * result + ((passIdentifier == null) ? 0 : passIdentifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MSVCCLinkWorkerTaskIdentifier other = (MSVCCLinkWorkerTaskIdentifier) obj;
		if (architecture == null) {
			if (other.architecture != null)
				return false;
		} else if (!architecture.equals(other.architecture))
			return false;
		if (passIdentifier == null) {
			if (other.passIdentifier != null)
				return false;
		} else if (!passIdentifier.equals(other.passIdentifier))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + passIdentifier + "/" + architecture + "]";
	}

}
