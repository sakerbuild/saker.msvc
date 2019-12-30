package saker.msvc.impl.sdk.option;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.ccompile.option.IncludeDirectoryVisitor;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.clink.option.LibraryPathVisitor;
import saker.sdk.support.api.SDKPathReference;

public class CommonSDKPathReferenceOption implements IncludeDirectoryOption, LibraryPathOption, Externalizable {
	private static final long serialVersionUID = 1L;

	private SDKPathReference pathReference;

	/**
	 * For {@link Externalizable}.
	 */
	public CommonSDKPathReferenceOption() {
	}

	public CommonSDKPathReferenceOption(SDKPathReference pathReference) {
		this.pathReference = pathReference;
	}

	public CommonSDKPathReferenceOption(String sdkname, String pathidentifier) {
		this.pathReference = SDKPathReference.create(sdkname, pathidentifier);
	}

	@Override
	public void accept(LibraryPathVisitor visitor) {
		visitor.visit(pathReference);
	}

	@Override
	public void accept(IncludeDirectoryVisitor visitor) {
		visitor.visit(pathReference);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(pathReference);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		pathReference = (SDKPathReference) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((pathReference == null) ? 0 : pathReference.hashCode());
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
		CommonSDKPathReferenceOption other = (CommonSDKPathReferenceOption) obj;
		if (pathReference == null) {
			if (other.pathReference != null)
				return false;
		} else if (!pathReference.equals(other.pathReference))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + pathReference + "]";
	}

}
