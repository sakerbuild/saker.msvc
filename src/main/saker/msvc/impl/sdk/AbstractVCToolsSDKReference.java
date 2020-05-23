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

import saker.build.file.path.SakerPath;
import saker.sdk.support.api.SDKReference;

public abstract class AbstractVCToolsSDKReference implements SDKReference, Externalizable {
	private static final long serialVersionUID = 1L;
	
	public static final String SDK_NAME = "MSVC";

	public static final String PREFIX_WORKING_DIRECTORY = "workdir.";
	public static final String PREFIX_EXE = "exe.";

	public static final String EXE_CL_X86_X86 = PREFIX_EXE + "cl.x86.x86";
	public static final String EXE_CL_X86_X64 = PREFIX_EXE + "cl.x86.x64";
	public static final String EXE_CL_X86_ARM = PREFIX_EXE + "cl.x86.arm";
	public static final String EXE_CL_X86_ARM64 = PREFIX_EXE + "cl.x86.arm64";
	public static final String EXE_CL_X64_X86 = PREFIX_EXE + "cl.x64.x86";
	public static final String EXE_CL_X64_X64 = PREFIX_EXE + "cl.x64.x64";
	public static final String EXE_CL_X64_ARM = PREFIX_EXE + "cl.x64.arm";
	public static final String EXE_CL_X64_ARM64 = PREFIX_EXE + "cl.x64.arm64";

	public static final String EXE_LINK_X86_X86 = PREFIX_EXE + "link.x86.x86";
	public static final String EXE_LINK_X86_X64 = PREFIX_EXE + "link.x86.x64";
	public static final String EXE_LINK_X86_ARM = PREFIX_EXE + "link.x86.arm";
	public static final String EXE_LINK_X86_ARM64 = PREFIX_EXE + "link.x86.arm64";
	public static final String EXE_LINK_X64_X86 = PREFIX_EXE + "link.x64.x86";
	public static final String EXE_LINK_X64_X64 = PREFIX_EXE + "link.x64.x64";
	public static final String EXE_LINK_X64_ARM = PREFIX_EXE + "link.x64.arm";
	public static final String EXE_LINK_X64_ARM64 = PREFIX_EXE + "link.x64.arm64";

	public static final String WORKDIR_EXE_CL_X86_X86 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x86.x86";
	public static final String WORKDIR_EXE_CL_X86_X64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x86.x64";
	public static final String WORKDIR_EXE_CL_X86_ARM = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x86.arm";
	public static final String WORKDIR_EXE_CL_X86_ARM64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x86.arm64";
	public static final String WORKDIR_EXE_CL_X64_X86 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x64.x86";
	public static final String WORKDIR_EXE_CL_X64_X64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x64.x64";
	public static final String WORKDIR_EXE_CL_X64_ARM = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x64.arm";
	public static final String WORKDIR_EXE_CL_X64_ARM64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "cl.x64.arm64";

	public static final String WORKDIR_EXE_LINK_X86_X86 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x86.x86";
	public static final String WORKDIR_EXE_LINK_X86_X64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x86.x64";
	public static final String WORKDIR_EXE_LINK_X86_ARM = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x86.arm";
	public static final String WORKDIR_EXE_LINK_X86_ARM64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x86.arm64";
	public static final String WORKDIR_EXE_LINK_X64_X86 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x64.x86";
	public static final String WORKDIR_EXE_LINK_X64_X64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x64.x64";
	public static final String WORKDIR_EXE_LINK_X64_ARM = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x64.arm";
	public static final String WORKDIR_EXE_LINK_X64_ARM64 = PREFIX_WORKING_DIRECTORY + PREFIX_EXE + "link.x64.arm64";

	public static final String LIB_X86 = "lib.x86";
	public static final String LIB_ARM = "lib.arm";
	public static final String LIB_ARM64 = "lib.arm64";
	public static final String LIB_X64 = "lib.x64";

	public static final String LIB_STORE_X86 = "lib.store.x86";
	public static final String LIB_STORE_ARM = "lib.store.arm";
	public static final String LIB_STORE_ARM64 = "lib.store.arm64";
	public static final String LIB_STORE_X64 = "lib.store.x64";

	public static final String INCLUDE = "include";

	public static final String STORE_REFERENCES_PLATFORM_WINMD = "lib.store.references.platform.winmd";

	protected String version;
	protected transient SakerPath baseDirectory;

	/**
	 * For {@link Externalizable}.
	 */
	public AbstractVCToolsSDKReference() {
	}

	public AbstractVCToolsSDKReference(String version, SakerPath baseDirectory) {
		this.version = version;
		this.baseDirectory = baseDirectory;
	}

	public String getVersion() {
		return version;
	}

	public SakerPath getBaseDirectory() {
		return baseDirectory;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(version);
		out.writeObject(baseDirectory);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		version = (String) in.readObject();
		baseDirectory = (SakerPath) in.readObject();
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
		AbstractVCToolsSDKReference other = (AbstractVCToolsSDKReference) obj;
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
