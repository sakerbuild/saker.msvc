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
package saker.msvc.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.FileEntry;
import saker.build.file.provider.LocalFileProvider;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.msvc.impl.sdk.AbstractVCToolsSDKReference;
import saker.msvc.impl.sdk.LegacyLayoutVCToolsSDKReference;
import saker.msvc.impl.sdk.RegularLayoutVCToolsSDKReference;
import saker.msvc.impl.sdk.VersionsMSVCSDKDescription;
import saker.nest.bundle.BundleIdentifier;
import saker.process.api.ProcessIOConsumer;
import saker.process.api.SakerProcess;
import saker.process.api.SakerProcessBuilder;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKPropertyCollectionReference;
import saker.sdk.support.api.SDKPropertyReference;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.exc.SDKPathNotFoundException;
import saker.std.api.file.location.FileLocation;
import saker.std.api.util.SakerStandardUtils;
import testing.saker.msvc.TestFlag;

public class MSVCUtils {
	public static final String SDK_NAME_MSVC = AbstractVCToolsSDKReference.SDK_NAME;

	public static final String VC_EXECUTABLE_NAME_CL = "cl";
	public static final String VC_EXECUTABLE_NAME_LINK = "link";

	public static final SakerPath PATH_PROGRAM_FILES = SakerPath.valueOf("c:/Program Files");
	public static final SakerPath PATH_PROGRAM_FILES_X86 = SakerPath.valueOf("c:/Program Files (x86)");
	public static final SakerPath PATH_PROGRAM_FILES_MICROSOFT_VISUAL_STUDIO = SakerPath
			.valueOf("c:/Program Files/Microsoft Visual Studio/");
	public static final SakerPath PATH_PROGRAM_FILES_X86_MICROSOFT_VISUAL_STUDIO = SakerPath
			.valueOf("c:/Program Files (x86)/Microsoft Visual Studio/");

	public static final SDKDescription DEFAULT_MSVC_SDK_DESCRIPTION = VersionsMSVCSDKDescription.create(null, null);

	public static void removeEnvironmentVariablesFromProcess(ProcessBuilder pb) {
		Map<String, String> env = pb.environment();
		removeMSVCEnvironmentVariables(env);
	}

	public static void removeMSVCEnvironmentVariables(Map<String, String> env) {
		env.remove("CL");
		env.remove("_CL_");
		env.remove("INCLUDE");
		env.remove("LIBPATH");

		env.remove("LINK");
		env.remove("_LINK_");
		env.remove("LIB");
		//https://docs.microsoft.com/en-us/cpp/build/reference/linkrepro?view=vs-2019
		env.remove("link_repro");
	}

	/**
	 * @param regularversions
	 *            The suitable regular layout versions or <code>null</code> if any.
	 * @param legacyversions
	 *            The suitable legacy layout versions or <code>null</code> if any.
	 * @return
	 */
	public static SDKDescription getMSVCDescription(Set<String> regularversions, Set<String> legacyversions) {
		if (regularversions == null && legacyversions == null) {
			return DEFAULT_MSVC_SDK_DESCRIPTION;
		}
		return VersionsMSVCSDKDescription.create(regularversions, legacyversions);
	}

	public static SakerPath getVCSDKExecutablePath(SDKReference vcsdk, String hostarchitecture,
			String targetarchitecture, String executablename) throws Exception {
		return vcsdk.getPath(AbstractVCToolsSDKReference.PREFIX_EXE + executablename + "." + hostarchitecture + "."
				+ targetarchitecture);
	}

	public static SakerPath getVCSDKExecutableWorkingDirectoryPath(SDKReference vcsdk, String hostarchitecture,
			String targetarchitecture, String executablename) throws Exception {
		return vcsdk
				.getPath(AbstractVCToolsSDKReference.PREFIX_WORKING_DIRECTORY + AbstractVCToolsSDKReference.PREFIX_EXE
						+ executablename + "." + hostarchitecture + "." + targetarchitecture);
	}

	public static int runMSVCProcess(List<String> commands, SakerPath workingdir, ProcessIOConsumer stdoutconsumer,
			ProcessIOConsumer stderrconsumer, boolean mergestderr)
			throws IllegalStateException, IOException, InterruptedException {
		if (TestFlag.ENABLED) {
			return TestFlag.metric().runProcess(commands, mergestderr,
					stdoutconsumer == null ? null : stdoutconsumer::handleOutput,
					stderrconsumer == null ? null : stderrconsumer::handleOutput);
		}
		SakerProcessBuilder pb = SakerProcessBuilder.create();
		pb.setCommand(commands);
		pb.setStandardOutputConsumer(stdoutconsumer);
		if (mergestderr) {
			pb.setStandardErrorMerge(true);
		} else {
			pb.setStandardErrorConsumer(stderrconsumer);
		}
		pb.setWorkingDirectory(workingdir);
		removeMSVCEnvironmentVariables(pb.getEnvironment());

		try (SakerProcess proc = pb.start()) {
			proc.processIO();
			return proc.waitFor();
		}
	}

	private MSVCUtils() {
		throw new UnsupportedOperationException();
	}

	public static AbstractVCToolsSDKReference searchMSVCLegacyToolchainInProgramFiles(SakerPath programfiles,
			Predicate<? super String> versionpredicate) {
		LocalFileProvider fp = LocalFileProvider.getInstance();

		//The valid entries are expected to be "Microsoft Visual Studio <version>". E.g. Microsoft Visual Studio 14.0
		NavigableMap<String, ? extends FileEntry> programfilesentries;
		try {
			programfilesentries = fp.getDirectoryEntries(programfiles);
		} catch (IOException e) {
			return null;
		}
		//the version numbers in the directory name have the same semantics as the bundle identifier version numbers
		//descending by version
		NavigableMap<String, String> vsverdirs = new TreeMap<>(
				Collections.reverseOrder(BundleIdentifier::compareVersionNumbers));
		for (Entry<String, ? extends FileEntry> entry : programfilesentries.entrySet()) {
			if (!entry.getValue().isDirectory()) {
				continue;
			}
			String dirname = entry.getKey();
			if (!StringUtils.startsWithIgnoreCase(dirname, "Microsoft Visual Studio ")) {
				continue;
			}
			String verafter = dirname.substring(24);
			if (!BundleIdentifier.isValidVersionNumber(verafter)) {
				continue;
			}
			if (!versionpredicate.test(verafter)) {
				continue;
			}
			vsverdirs.put(verafter, dirname);
		}
		for (Entry<String, String> entry : vsverdirs.entrySet()) {
			SakerPath vcdirpath = programfiles.resolve(entry.getValue(), "VC");
			//x86 host x86 target cl exe 
			SakerPath clexepath = vcdirpath.resolve("bin", "cl.exe");
			FileEntry clexeattrs;
			try {
				clexeattrs = fp.getFileAttributes(clexepath);
			} catch (IOException e) {
				continue;
			}
			if (clexeattrs.isRegularFile()) {
				return new LegacyLayoutVCToolsSDKReference(entry.getKey(), vcdirpath);
			}
		}
		return null;
	}

	public static AbstractVCToolsSDKReference searchMSVCRegularToolchainInStudioDir(SakerPath studiosbasedir,
			Predicate<? super String> versionpredicate) {
		LocalFileProvider fp = LocalFileProvider.getInstance();

		//The entries are expected to be the year number version of the Visual Studio. E.g. 2019
		NavigableMap<String, ? extends FileEntry> installversionentries;
		try {
			installversionentries = fp.getDirectoryEntries(studiosbasedir);
		} catch (IOException e) {
			return null;
		}
		//descending install version number directories
		NavigableMap<Integer, String> versiondirs = new TreeMap<>(Comparator.reverseOrder());
		for (Entry<String, ? extends FileEntry> entry : installversionentries.entrySet()) {
			if (!entry.getValue().isDirectory()) {
				continue;
			}
			int versionnum;
			try {
				versionnum = Integer.parseInt(entry.getKey());
			} catch (NumberFormatException e) {
				continue;
			}
			versiondirs.put(versionnum, entry.getKey());
		}
		for (Entry<Integer, String> entry : versiondirs.entrySet()) {
			SakerPath versiondir = studiosbasedir.resolve(entry.getValue());
			NavigableMap<String, ? extends FileEntry> vsinstallentries;
			try {
				//TODO sort these entries by type? Enterprise, Professional, Community? (Professional may not exist, check...)

				//The entries is expected to be the type of installation. E.g. Community
				vsinstallentries = fp.getDirectoryEntries(versiondir);
			} catch (IOException e) {
				//not valid dir
				continue;
			}
			for (Entry<String, ? extends FileEntry> installentry : vsinstallentries.entrySet()) {
				if (!installentry.getValue().isDirectory()) {
					continue;
				}
				SakerPath msvcdirpath = versiondir.resolve(installentry.getKey(), "VC", "Tools", "MSVC");
				NavigableMap<String, ? extends FileEntry> msvcdirentries;
				try {
					//The entries are expected to be version numers. E.g. 14.22.27905
					msvcdirentries = fp.getDirectoryEntries(msvcdirpath);
				} catch (IOException e) {
					continue;
				}

				//we use the bundle identifier version methods as it is semantically the same
				//descending version numbers
				NavigableSet<String> versiondirectories = new TreeSet<>(
						Collections.reverseOrder(BundleIdentifier::compareVersionNumbers));
				for (Entry<String, ? extends FileEntry> msvctoolsverentry : msvcdirentries.entrySet()) {
					if (!msvctoolsverentry.getValue().isDirectory()) {
						continue;
					}
					String version = msvctoolsverentry.getKey();
					if (!BundleIdentifier.isValidVersionNumber(version)) {
						continue;
					}
					if (!versionpredicate.test(version)) {
						continue;
					}
					versiondirectories.add(version);
				}
				for (String versiondirname : versiondirectories) {
					SakerPath versionedmsvctoolsdirpath = msvcdirpath.resolve(versiondirname);
					SakerPath clexepath = versionedmsvctoolsdirpath.resolve("bin", "Hostx86", "x86", "cl.exe");
					//we expect the cl compiler exe to be present in the above path
					try {
						if (fp.getFileAttributes(clexepath).isRegularFile()) {
							return new RegularLayoutVCToolsSDKReference(versiondirname, versionedmsvctoolsdirpath);
						}
					} catch (IOException e) {
						continue;
					}
				}
			}
		}
		return null;
	}

	public static Predicate<? super String> getSDKVersionsPredicate(Set<String> versions) {
		if (versions == null) {
			return Functionals.alwaysPredicate();
		}
		return versions::contains;
	}

	public static String getFileName(FileLocation fl) {
		return SakerStandardUtils.getFileLocationFileName(fl);
	}

	public static void evaluateSimpleParameters(List<String> result, List<? extends SimpleParameterOption> params,
			Map<String, ? extends SDKReference> sdks) throws Exception {
		if (params == null) {
			return;
		}
		SimpleParameterOption.Visitor visitor = new SimpleParameterOption.Visitor() {
			@Override
			public void visit(String value) {
				result.add(value);
			}

			@Override
			public void visit(SDKPathCollectionReference value) {
				try {
					Collection<SakerPath> paths = value.getValue(sdks);
					if (paths == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					for (SakerPath p : paths) {
						result.add(p.toString());
					}
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPropertyCollectionReference value) {
				try {
					Collection<String> props = value.getValue(sdks);
					if (props == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.addAll(props);
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPathReference value) {
				try {
					SakerPath p = value.getValue(sdks);
					if (p == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.add(p.toString());
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

			@Override
			public void visit(SDKPropertyReference value) {
				try {
					String prop = value.getValue(sdks);
					if (prop == null) {
						throw new SDKPathNotFoundException("No SDK paths found for: " + value);
					}
					result.add(prop);
				} catch (Exception e) {
					throw ObjectUtils.sneakyThrow(e);
				}
			}

		};
		for (SimpleParameterOption p : params) {
			p.accept(visitor);
		}
	}

}
