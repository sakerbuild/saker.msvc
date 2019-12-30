package saker.msvc.impl.sdk;

import java.util.Locale;

import saker.build.file.path.SakerPath;

/**
 * Pre Visual Studio 2017 layout.
 * <p>
 * The associated base directory points to the VC tools root directory.
 * <p>
 * E.g. <code>c:\Program Files (x86)\Microsoft Visual Studio 14.0\VC\</code>
 */
public class LegacyLayoutVCToolsSDKReference extends AbstractVCToolsSDKReference {

	private static final long serialVersionUID = 1L;

	public LegacyLayoutVCToolsSDKReference() {
		super();
	}

	public LegacyLayoutVCToolsSDKReference(String version, SakerPath baseDirectory) {
		super(version, baseDirectory);
	}

	@Override
	public SakerPath getPath(String identifier) throws Exception {
		switch (identifier.toLowerCase(Locale.ENGLISH)) {
			case EXE_CL_X86_X86: {
				return baseDirectory.resolve("bin", "cl.exe");
			}
			case EXE_CL_X86_ARM: {
				return baseDirectory.resolve("bin", "x86_arm", "cl.exe");
			}
			case EXE_CL_X86_X64: {
				return baseDirectory.resolve("bin", "x86_amd64", "cl.exe");
			}
			case EXE_CL_X64_X86: {
				return baseDirectory.resolve("bin", "amd64_x86", "cl.exe");
			}
			case EXE_CL_X64_ARM: {
				return baseDirectory.resolve("bin", "amd64_arm", "cl.exe");
			}
			case EXE_CL_X64_X64: {
				return baseDirectory.resolve("bin", "amd64", "cl.exe");
			}

			case EXE_LINK_X86_X86: {
				return baseDirectory.resolve("bin", "link.exe");
			}
			case EXE_LINK_X86_ARM: {
				return baseDirectory.resolve("bin", "x86_arm", "link.exe");
			}
			case EXE_LINK_X86_X64: {
				return baseDirectory.resolve("bin", "x86_amd64", "link.exe");
			}
			case EXE_LINK_X64_X86: {
				return baseDirectory.resolve("bin", "amd64_x86", "link.exe");
			}
			case EXE_LINK_X64_ARM: {
				return baseDirectory.resolve("bin", "amd64_arm", "link.exe");
			}
			case EXE_LINK_X64_X64: {
				return baseDirectory.resolve("bin", "amd64", "link.exe");
			}

			case WORKDIR_EXE_CL_X86_X64:
			case WORKDIR_EXE_CL_X86_ARM:
			case WORKDIR_EXE_CL_X86_X86:
			case WORKDIR_EXE_LINK_X86_X64:
			case WORKDIR_EXE_LINK_X86_ARM:
			case WORKDIR_EXE_LINK_X86_X86: {
				return baseDirectory.resolve("bin");
			}
			case WORKDIR_EXE_CL_X64_X64:
			case WORKDIR_EXE_CL_X64_ARM:
			case WORKDIR_EXE_CL_X64_X86:
			case WORKDIR_EXE_LINK_X64_X64:
			case WORKDIR_EXE_LINK_X64_ARM:
			case WORKDIR_EXE_LINK_X64_X86: {
				return baseDirectory.resolve("bin", "amd64");
			}

			case LIB_X86: {
				return baseDirectory.resolve("lib");
			}
			case LIB_ARM: {
				return baseDirectory.resolve("lib", "arm");
			}
			case LIB_X64: {
				return baseDirectory.resolve("lib", "amd64");
			}

			case LIB_STORE_X86: {
				return baseDirectory.resolve("lib", "store");
			}
			case LIB_STORE_X64: {
				return baseDirectory.resolve("lib", "store", "amd64");
			}
			case LIB_STORE_ARM: {
				return baseDirectory.resolve("lib", "store", "arm");
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
