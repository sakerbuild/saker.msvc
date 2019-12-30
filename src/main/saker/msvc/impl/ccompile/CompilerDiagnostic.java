package saker.msvc.impl.ccompile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.StringUtils;

public class CompilerDiagnostic implements Externalizable, Comparable<CompilerDiagnostic> {
	private static final long serialVersionUID = 1L;

	private SakerPath path;
	private int severity;
	private int lineIndex;
	private String errorCode;
	private String description;

	/**
	 * For {@link Externalizable}.
	 */
	public CompilerDiagnostic() {
	}

	public CompilerDiagnostic(SakerPath path, int severity, int lineIndex, String errorCode, String description) {
		this.path = path;
		this.severity = severity;
		this.lineIndex = lineIndex;
		this.errorCode = errorCode;
		this.description = description;
	}

	public SakerPath getPath() {
		return path;
	}

	public String getDescription() {
		return description;
	}

	public String getErrorCode() {
		return errorCode;
	}

	public int getLineIndex() {
		return lineIndex;
	}

	public int getSeverity() {
		return severity;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(path);
		out.writeInt(severity);
		out.writeInt(lineIndex);
		out.writeObject(errorCode);
		out.writeObject(description);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		path = (SakerPath) in.readObject();
		severity = in.readInt();
		lineIndex = in.readInt();
		errorCode = (String) in.readObject();
		description = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((description == null) ? 0 : description.hashCode());
		result = prime * result + ((errorCode == null) ? 0 : errorCode.hashCode());
		result = prime * result + lineIndex;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + severity;
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
		CompilerDiagnostic other = (CompilerDiagnostic) obj;
		if (description == null) {
			if (other.description != null)
				return false;
		} else if (!description.equals(other.description))
			return false;
		if (errorCode == null) {
			if (other.errorCode != null)
				return false;
		} else if (!errorCode.equals(other.errorCode))
			return false;
		if (lineIndex != other.lineIndex)
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (severity != other.severity)
			return false;
		return true;
	}

	@Override
	public int compareTo(CompilerDiagnostic o) {
		int cmp;
		if (this.path == null) {
			if (o.path != null) {
				return -1;
			}
		} else {
			if (o.path == null) {
				return 1;
			}
			cmp = this.path.compareTo(o.path);
			if (cmp != 0) {
				return cmp;
			}
		}
		cmp = Integer.compare(this.severity, o.severity);
		if (cmp != 0) {
			return cmp;
		}
		cmp = Integer.compare(this.lineIndex, o.lineIndex);
		if (cmp != 0) {
			return cmp;
		}
		cmp = StringUtils.compareStringsNullFirst(this.errorCode, o.errorCode);
		if (cmp != 0) {
			return cmp;
		}
		cmp = StringUtils.compareStringsNullFirst(this.description, o.description);
		if (cmp != 0) {
			return cmp;
		}
		return 0;
	}

}