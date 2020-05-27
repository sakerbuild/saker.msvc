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

import java.util.Locale;

import saker.build.file.path.SakerPath;

/**
 * Layout introduced with Visual Studio 2017.
 * <p>
 * The associated base directory points to a versioned directory that holds the VC tools.
 * <p>
 * E.g. <code>c:\Program Files (x86)\Microsoft Visual Studio\2019\Community\VC\Tools\MSVC\14.22.27905\</code>
 * 
 * @see https://devblogs.microsoft.com/cppblog/compiler-tools-layout-in-visual-studio-15/
 */
public class RegularLayoutVCToolsSDKReference extends AbstractVCToolsSDKReference {
	private static final long serialVersionUID = 1L;

	public RegularLayoutVCToolsSDKReference() {
		super();
	}

	public RegularLayoutVCToolsSDKReference(String version, SakerPath baseDirectory) {
		super(version, baseDirectory);
	}

	@Override
	public SakerPath getPath(String identifier) throws Exception {
		switch (identifier.toLowerCase(Locale.ENGLISH)) {
			case EXE_CL_X86_X86: {
				return baseDirectory.resolve("bin", "Hostx86", "x86", "cl.exe");
			}
			case EXE_CL_X86_X64: {
				return baseDirectory.resolve("bin", "Hostx86", "x64", "cl.exe");
			}
			case EXE_CL_X86_ARM: {
				return baseDirectory.resolve("bin", "Hostx86", "arm", "cl.exe");
			}
			case EXE_CL_X86_ARM64: {
				return baseDirectory.resolve("bin", "Hostx86", "arm64", "cl.exe");
			}
			case EXE_CL_X64_X86: {
				return baseDirectory.resolve("bin", "Hostx64", "x86", "cl.exe");
			}
			case EXE_CL_X64_X64: {
				return baseDirectory.resolve("bin", "Hostx64", "x64", "cl.exe");
			}
			case EXE_CL_X64_ARM: {
				return baseDirectory.resolve("bin", "Hostx64", "arm", "cl.exe");
			}
			case EXE_CL_X64_ARM64: {
				return baseDirectory.resolve("bin", "Hostx64", "arm64", "cl.exe");
			}

			case EXE_LINK_X86_X86: {
				return baseDirectory.resolve("bin", "Hostx86", "x86", "link.exe");
			}
			case EXE_LINK_X86_X64: {
				return baseDirectory.resolve("bin", "Hostx86", "x64", "link.exe");
			}
			case EXE_LINK_X86_ARM: {
				return baseDirectory.resolve("bin", "Hostx86", "arm", "link.exe");
			}
			case EXE_LINK_X86_ARM64: {
				return baseDirectory.resolve("bin", "Hostx86", "arm64", "link.exe");
			}
			case EXE_LINK_X64_X86: {
				return baseDirectory.resolve("bin", "Hostx64", "x86", "link.exe");
			}
			case EXE_LINK_X64_X64: {
				return baseDirectory.resolve("bin", "Hostx64", "x64", "link.exe");
			}
			case EXE_LINK_X64_ARM: {
				return baseDirectory.resolve("bin", "Hostx64", "arm", "link.exe");
			}
			case EXE_LINK_X64_ARM64: {
				return baseDirectory.resolve("bin", "Hostx64", "arm64", "link.exe");
			}

			case LIB_X86: {
				return baseDirectory.resolve("lib", "x86");
			}
			case LIB_X64: {
				return baseDirectory.resolve("lib", "x64");
			}
			case LIB_ARM: {
				return baseDirectory.resolve("lib", "arm");
			}
			case LIB_ARM64: {
				return baseDirectory.resolve("lib", "arm64");
			}

			case LIB_STORE_X86: {
				return baseDirectory.resolve("lib", "x86", "store");
			}
			case LIB_STORE_X64: {
				return baseDirectory.resolve("lib", "x64", "store");
			}
			case LIB_STORE_ARM: {
				return baseDirectory.resolve("lib", "arm", "store");
			}
			case LIB_STORE_ARM64: {
				return baseDirectory.resolve("lib", "arm64", "store");
			}
			case STORE_REFERENCES_PLATFORM_WINMD: {
				return baseDirectory.resolve("lib", "store", "references", "platform.winmd");
			}

			case INCLUDE: {
				return baseDirectory.resolve("include");
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
}
