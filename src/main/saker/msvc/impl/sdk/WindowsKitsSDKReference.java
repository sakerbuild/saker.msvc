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
package saker.msvc.impl.sdk;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Locale;

import saker.build.file.path.SakerPath;
import saker.sdk.support.api.SDKReference;

public class WindowsKitsSDKReference implements SDKReference, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final String SDK_NAME = "WindowsKits";

	public static final String INCLUDE_UCRT = "include.ucrt";
	public static final String INCLUDE_CPPWINRT = "include.cppwinrt";
	public static final String INCLUDE_WINRT = "include.winrt";
	public static final String INCLUDE_SHARED = "include.shared";
	public static final String INCLUDE_UM = "include.um";

	public static final String LIB_X86_UCRT = "lib.x86.ucrt";
	public static final String LIB_X86_UM = "lib.x86.um";

	public static final String LIB_X64_UCRT = "lib.x64.ucrt";
	public static final String LIB_X64_UM = "lib.x64.um";

	public static final String LIB_ARM_UCRT = "lib.arm.ucrt";
	public static final String LIB_ARM_UM = "lib.arm.um";

	public static final String LIB_ARM64_UCRT = "lib.arm64.ucrt";
	public static final String LIB_ARM64_UM = "lib.arm64.um";

	public static final String BIN_ARM64 = "bin.arm64";
	public static final String BIN_ARM = "bin.arm";
	public static final String BIN_X64 = "bin.x64";
	public static final String BIN_X86 = "bin.x86";

	public static final String REFERENCES = "references";

	private String version;
	private transient SakerPath baseDirectory;

	/**
	 * For {@link Externalizable}.
	 */
	public WindowsKitsSDKReference() {
	}

	public WindowsKitsSDKReference(SakerPath baseDirectory, String version) {
		this.baseDirectory = baseDirectory;
		this.version = version;
	}

	public SakerPath getBaseDirectory() {
		return baseDirectory;
	}

	public String getVersion() {
		return version;
	}

	@Override
	public SakerPath getPath(String identifier) throws Exception {
		switch (identifier.toLowerCase(Locale.ENGLISH)) {
			case BIN_ARM: {
				return baseDirectory.resolve("bin", version, "arm");
			}
			case BIN_ARM64: {
				return baseDirectory.resolve("bin", version, "arm64");
			}
			case BIN_X64: {
				return baseDirectory.resolve("bin", version, "x64");
			}
			case BIN_X86: {
				return baseDirectory.resolve("bin", version, "x86");
			}

			case INCLUDE_UCRT: {
				return baseDirectory.resolve("Include", version, "ucrt");
			}
			case INCLUDE_CPPWINRT: {
				return baseDirectory.resolve("Include", version, "cppwinrt");
			}
			case INCLUDE_WINRT: {
				return baseDirectory.resolve("Include", version, "winrt");
			}
			case INCLUDE_SHARED: {
				return baseDirectory.resolve("Include", version, "shared");
			}
			case INCLUDE_UM: {
				return baseDirectory.resolve("Include", version, "um");
			}

			case LIB_X64_UCRT: {
				return baseDirectory.resolve("Lib", version, "ucrt", "x64");
			}
			case LIB_X64_UM: {
				return baseDirectory.resolve("Lib", version, "um", "x64");
			}

			case LIB_X86_UCRT: {
				return baseDirectory.resolve("Lib", version, "ucrt", "x86");
			}
			case LIB_X86_UM: {
				return baseDirectory.resolve("Lib", version, "um", "x86");
			}

			case LIB_ARM64_UCRT: {
				return baseDirectory.resolve("Lib", version, "ucrt", "arm64");
			}
			case LIB_ARM64_UM: {
				return baseDirectory.resolve("Lib", version, "um", "arm64");
			}

			case LIB_ARM_UCRT: {
				return baseDirectory.resolve("Lib", version, "ucrt", "arm");
			}
			case LIB_ARM_UM: {
				return baseDirectory.resolve("Lib", version, "um", "arm");
			}

			case REFERENCES: {
				return baseDirectory.resolve("References", version);
			}

			default: {
				break;
			}
		}
		return null;
	}

	@Override
	public String getProperty(String identifier) throws Exception {
		return null;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(baseDirectory);
		out.writeObject(version);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		baseDirectory = (SakerPath) in.readObject();
		version = (String) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((version == null) ? 0 : version.hashCode());
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
		WindowsKitsSDKReference other = (WindowsKitsSDKReference) obj;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (baseDirectory != null ? "baseDirectory=" + baseDirectory + ", " : "")
				+ (version != null ? "version=" + version : "") + "]";
	}

}
