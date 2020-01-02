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

import java.io.Externalizable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Predicate;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.FileEntry;
import saker.build.file.provider.LocalFileProvider;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.function.Functionals;
import saker.msvc.impl.ccompile.SDKBasedExecutionEnvironmentSelector;
import saker.msvc.impl.sdk.AbstractVCToolsSDKReference;
import saker.msvc.impl.sdk.LegacyLayoutVCToolsSDKReference;
import saker.msvc.impl.sdk.RegularLayoutVCToolsSDKReference;
import saker.msvc.impl.sdk.VersionsMSVCSDKDescription;
import saker.msvc.impl.sdk.VersionsWindowsKitsSDKDescription;
import saker.msvc.impl.sdk.WindowsKitsSDKReference;
import saker.msvc.proc.NativeProcess;
import saker.nest.bundle.BundleIdentifier;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.IndeterminateSDKDescription;
import saker.sdk.support.api.ResolvedSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKDescriptionVisitor;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.UserSDKDescription;
import saker.std.api.environment.qualifier.AnyEnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifierVisitor;
import saker.std.api.environment.qualifier.PropertyEnvironmentQualifier;
import testing.saker.msvc.TestFlag;

public class MSVCUtils {
	public static final String SDK_NAME_WINDOWS_KITS = "WindowsKits";

	public static final String SDK_NAME_MSVC = "MSVC";
	public static final String MSVC_IDENTIFIER_LIB_X64 = "lib.x64";
	public static final String MSVC_IDENTIFIER_LIB_X86 = "lib.x86";
	public static final String MSVC_IDENTIFIER_INCLUDE = "include";

	public static final String VC_EXECUTABLE_NAME_CL = "cl";
	public static final String VC_EXECUTABLE_NAME_LINK = "link";

	public static final SakerPath PATH_PROGRAM_FILES = SakerPath.valueOf("c:/Program Files");
	public static final SakerPath PATH_PROGRAM_FILES_X86 = SakerPath.valueOf("c:/Program Files (x86)");
	public static final SakerPath PATH_PROGRAM_FILES_MICROSOFT_VISUAL_STUDIO = SakerPath
			.valueOf("c:/Program Files/Microsoft Visual Studio/");
	public static final SakerPath PATH_PROGRAM_FILES_X86_MICROSOFT_VISUAL_STUDIO = SakerPath
			.valueOf("c:/Program Files (x86)/Microsoft Visual Studio/");

	public static final SDKDescription DEFAULT_MSVC_SDK_DESCRIPTION = VersionsMSVCSDKDescription.create(null, null);
	public static final SDKDescription DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION = VersionsWindowsKitsSDKDescription
			.create(null);

	public static void removeEnvironmentVariablesFromProcess(ProcessBuilder pb) {
		Map<String, String> env = pb.environment();
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
	 * @param versions
	 *            The suitable versions or <code>null</code> if any.
	 * @return
	 */
	public static SDKDescription getWindowsKitsSDKDescription(Set<String> versions) {
		if (versions == null) {
			return DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION;
		}
		return VersionsWindowsKitsSDKDescription.create(versions);
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

	public static Comparator<String> getSDKNameComparator() {
		return SDKSupportUtils.getSDKNameComparator();
	}

	public static Comparator<String> getLinkerParameterIgnoreCaseComparator() {
		return IgnoreCaseComparator.INSTANCE;
	}

	private static final class IgnoreCaseComparator implements Comparator<String>, Externalizable {
		private static final long serialVersionUID = 1L;

		public static final IgnoreCaseComparator INSTANCE = new IgnoreCaseComparator();

		/**
		 * For {@link Externalizable}.
		 */
		public IgnoreCaseComparator() {
		}

		@Override
		public int compare(String o1, String o2) {
			return o1.compareToIgnoreCase(o2);
		}

		@Override
		public void writeExternal(ObjectOutput out) throws IOException {
		}

		@Override
		public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		}

		@Override
		public int hashCode() {
			return getClass().getName().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return ObjectUtils.isSameClass(this, obj);
		}
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

	public static Process startProcess(ProcessBuilder pb) throws IOException {
		if (TestFlag.ENABLED) {
			return TestFlag.metric().startProcess(pb);
		}
		return pb.start();
	}

	public static int runClProcess(SakerPath clexe, List<String> commands, SakerPath workingdir,
			NativeProcess.IOProcessor ioprocessor) throws IllegalArgumentException, IOException, InterruptedException {
		//TODO handle environment variables 
		if (TestFlag.ENABLED || !NativeProcess.LOADED) {
			Process proc = createClProcess(clexe, commands, workingdir);
			InputStream in = proc.getInputStream();
			byte[] barray = new byte[1024 * 8];
			ByteBuffer buf = ByteBuffer.wrap(barray);
			while (true) {
				int read = in.read(barray);
				if (read <= 0) {
					break;
				}
				buf.rewind();
				buf.limit(read);
				ioprocessor.standardInputBytesAvailable(buf);
			}
			return proc.waitFor();
		} else {
			int result;
			try (NativeProcess nativeproc = NativeProcess.startProcess(clexe,
					commands.toArray(ObjectUtils.EMPTY_STRING_ARRAY), workingdir, NativeProcess.FLAG_MERGE_STDERR)) {
				nativeproc.processIO(ioprocessor);
				result = nativeproc.waitFor();
			}
			return result;
		}
	}

	private static Process createClProcess(SakerPath clexe, List<String> commands, SakerPath workingdir)
			throws IOException {
		ProcessBuilder pb = createClProcessBuilder(clexe, commands, workingdir);

		if (TestFlag.ENABLED) {
			return TestFlag.metric().startProcess(pb);
		}

		return pb.start();
	}

	private static ProcessBuilder createClProcessBuilder(SakerPath clexe, List<String> commands, SakerPath workingdir) {
		ArrayList<String> cmdlist = new ArrayList<>();
		cmdlist.add(clexe.toString());
		ObjectUtils.addAll(cmdlist, commands);
		ProcessBuilder pb = new ProcessBuilder(cmdlist);
		pb.redirectErrorStream(true);
		pb.directory(new File((workingdir == null ? clexe.getParent() : workingdir).toString()));
		return pb;
	}

	private MSVCUtils() {
		throw new UnsupportedOperationException();
	}

	public static SDKBasedExecutionEnvironmentSelector createEnvironmentSelectorForSDKs(
			NavigableMap<String, SDKDescription> sdks) {
		boolean[] clusterable = { true };
		for (SDKDescription desc : sdks.values()) {
			desc.accept(new SDKDescriptionVisitor() {
				@Override
				public void visit(EnvironmentSDKDescription description) {
					//stay clusterable as true
				}

				@Override
				public void visit(ResolvedSDKDescription description) {
					clusterable[0] = false;
				}

				@Override
				public void visit(IndeterminateSDKDescription description) {
					SDKDescription basesdk = description.getBaseSDKDescription();
					Objects.requireNonNull(basesdk, "base sdk description");
					basesdk.accept(this);
				}

				@Override
				public void visit(UserSDKDescription description) {
					EnvironmentQualifier qualifier = description.getQualifier();
					if (qualifier == null) {
						//only the local should be used
						clusterable[0] = false;
						return;
					}
					qualifier.accept(new EnvironmentQualifierVisitor() {
						@Override
						public void visit(PropertyEnvironmentQualifier qualifier) {
							//stay clusterable as true 
						}

						@Override
						public void visit(AnyEnvironmentQualifier qualifier) {
							//stay clusterable as true 
						}
					});
				}
			});
			if (!clusterable[0]) {
				return null;
			}
		}
		return new SDKBasedExecutionEnvironmentSelector(ImmutableUtils.makeImmutableNavigableMap(sdks));
	}

	public static SDKBasedExecutionEnvironmentSelector undefaultizeSDKEnvironmentSelector(
			TaskExecutionEnvironmentSelector selector, EnvironmentSelectionResult envselectionresult) {
		Objects.requireNonNull(selector, "selector");
		SDKBasedExecutionEnvironmentSelector sdksel = (SDKBasedExecutionEnvironmentSelector) selector;
		TreeMap<String, SDKDescription> ndescriptions = new TreeMap<>(sdksel.getDescriptions());
		for (Entry<String, SDKDescription> entry : ndescriptions.entrySet()) {
			SDKDescription desc = entry.getValue();
			if (desc instanceof IndeterminateSDKDescription) {
				IndeterminateSDKDescription indeterminate = (IndeterminateSDKDescription) desc;
				SDKReference actualreference = SDKBasedExecutionEnvironmentSelector.getResolvedSDKReference(desc,
						envselectionresult);
				SDKDescription pinneddescription = indeterminate.pinSDKDescription(actualreference);
				entry.setValue(pinneddescription);
			}
		}
		return new SDKBasedExecutionEnvironmentSelector(ImmutableUtils.makeImmutableNavigableMap(ndescriptions));
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

	public static WindowsKitsSDKReference searchWindowsKitsInProgramFiles(SakerPath programfiles,
			Predicate<? super String> versionpredicate) {
		LocalFileProvider fp = LocalFileProvider.getInstance();

		SakerPath winkitsdir = programfiles.resolve("Windows Kits");
		NavigableMap<String, ? extends FileEntry> entries;
		try {
			//Expected to contain version numbers directories. E.g. 10, 8.1
			entries = fp.getDirectoryEntries(winkitsdir);
		} catch (IOException e) {
			return null;
		}
		NavigableSet<String> descendingverdirectories = new TreeSet<>(
				Collections.reverseOrder(BundleIdentifier::compareVersionNumbers));
		for (Entry<String, ? extends FileEntry> verentry : entries.entrySet()) {
			if (!verentry.getValue().isDirectory()) {
				continue;
			}
			String dirname = verentry.getKey();
			//same version number semantics as bundle identifiers
			if (!BundleIdentifier.isValidVersionNumber(dirname)) {
				continue;
			}
			descendingverdirectories.add(dirname);
		}
		//Search for Windows.h to validate the SDK install.
		//expect it to be at Include/<sdkversion>/um/Windows.h
		for (String versiondir : descendingverdirectories) {
			SakerPath versionedwinkitsdir = winkitsdir.resolve(versiondir);
			SakerPath includedir = versionedwinkitsdir.resolve("Include");
			NavigableMap<String, ? extends FileEntry> includedirentries;
			try {
				includedirentries = fp.getDirectoryEntries(includedir);
			} catch (IOException e) {
				continue;
			}
			NavigableSet<String> descendingincludeverdirectories = new TreeSet<>(
					Collections.reverseOrder(BundleIdentifier::compareVersionNumbers));
			for (Entry<String, ? extends FileEntry> incdirentry : includedirentries.entrySet()) {
				if (!incdirentry.getValue().isDirectory()) {
					continue;
				}
				String version = incdirentry.getKey();
				if (!BundleIdentifier.isValidVersionNumber(version)) {
					continue;
				}
				if (!versionpredicate.test(version)) {
					continue;
				}
				descendingincludeverdirectories.add(version);
			}
			for (String includeverdir : descendingincludeverdirectories) {
				SakerPath winhpath = includedir.resolve(includeverdir, "um", "Windows.h");
				try {
					if (fp.getFileAttributes(winhpath).isRegularFile()) {
						return new WindowsKitsSDKReference(versionedwinkitsdir, includeverdir);
					}
				} catch (IOException e) {
					continue;
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
}
