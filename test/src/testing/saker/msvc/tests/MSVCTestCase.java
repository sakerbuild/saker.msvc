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
package testing.saker.msvc.tests;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Objects;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.build.tests.EnvironmentTestCaseConfiguration;
import testing.saker.msvc.tests.mock.CLMockProcess;
import testing.saker.msvc.tests.mock.MockingMSVCTestMetric;
import testing.saker.nest.util.RepositoryLoadingVariablesMetricEnvironmentTestCase;

public abstract class MSVCTestCase extends RepositoryLoadingVariablesMetricEnvironmentTestCase {
	protected static final String LINE_SEPARATOR = System.lineSeparator();
	protected static final int MULTI_C_X64 = MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * 64;
	protected static final int MULTI_C_X86 = MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * 86;
	protected static final int MULTI_CPP_X64 = MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * 64;
	protected static final int MULTI_CPP_X86 = MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * 86;

	protected static final String LANG_C = "c";
	protected static final String ARCH_X64 = "x64";
	protected static final String LANG_CPP = "c++";
	protected static final String ARCH_X86 = "x86";

	protected static final String VER_1_0 = "1.0";

	public static final String DEFAULT_CLUSTER_NAME = "cluster";

	@Override
	protected MockingMSVCTestMetric createMetricImpl() {
		return new MockingMSVCTestMetric(getTestSDKDirectory());
	}

	@Override
	protected Set<EnvironmentTestCaseConfiguration> getTestConfigurations() {
		Path testsdkdir = getTestSDKDirectory();
		if (testsdkdir == null) {
			return super.getTestConfigurations();
		}
		testsdkdir = testsdkdir.resolve("windowskits");
		ArrayList<EnvironmentTestCaseConfiguration> configs = ObjectUtils.newArrayList(super.getTestConfigurations());
		for (ListIterator<EnvironmentTestCaseConfiguration> it = configs.listIterator(); it.hasNext();) {
			EnvironmentTestCaseConfiguration c = it.next();
			EnvironmentTestCaseConfiguration.Builder builder = EnvironmentTestCaseConfiguration.builder(c);
			TreeMap<String, String> envparams = ObjectUtils.newTreeMap(c.getEnvironmentUserParameters());
			envparams.put("saker.windows.sdk.windowskits.install.location.1.0", testsdkdir.toString());
			builder.setEnvironmentUserParameters(envparams);
			it.set(builder.build());
		}
		return ObjectUtils.newLinkedHashSet(configs);
	}

	protected Path getTestSDKDirectory() {
		Path basedir = getTestingBaseWorkingDirectory();
		if (basedir == null) {
			return null;
		}
		return basedir.resolve("testsdk");
	}

	@Override
	protected MockingMSVCTestMetric getMetric() {
		return (MockingMSVCTestMetric) super.getMetric();
	}

	public static String src(String... lines) {
		return StringUtils.toStringJoin(LINE_SEPARATOR, lines);
	}

	public static int langC(int val) {
		return val * MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_C;
	}

	public static int langCpp(int val) {
		return val * MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP;
	}

	public static String linkExe(String arch, int... lines) {
		return linkTypeImpl(VER_1_0, MockingMSVCTestMetric.TYPE_EXE, arch, lines);
	}

	public static String linkDll(String arch, int... lines) {
		return linkTypeImpl(VER_1_0, MockingMSVCTestMetric.TYPE_DLL, arch, lines);
	}

	public static String linkExeVer(String version, String arch, int... lines) {
		return linkTypeImpl(version, MockingMSVCTestMetric.TYPE_EXE, arch, lines);
	}

	public static String linkDllVer(String version, String arch, int... lines) {
		return linkTypeImpl(version, MockingMSVCTestMetric.TYPE_DLL, arch, lines);
	}

	public String winmd(SakerPath... sourcefiles) throws IOException, NoSuchAlgorithmException {
		try (UnsyncByteArrayOutputStream winmdoutbuf = new UnsyncByteArrayOutputStream()) {
			for (SakerPath inputfile : sourcefiles) {
				winmdoutbuf.write((StringUtils.toHexString(files.hash(inputfile, "MD5").getHash()) + "\n")
						.getBytes(StandardCharsets.UTF_8));
			}
			return winmdoutbuf.toString();
		}
	}

	private static String linkTypeImpl(String version, String type, String arch, int... lines) {
		int mult = CLMockProcess.getArchitectureMultiplier(arch);
		List<String> vals = new ArrayList<>();
		vals.add(MockingMSVCTestMetric.createFileTypeLine(type, arch));
		vals.add("#version " + version);
		for (int l : lines) {
			vals.add(l * mult + "");
		}

		return StringUtils.toStringJoin(null, LINE_SEPARATOR, vals, LINE_SEPARATOR);
	}

	public static String compile(String lang, String arch, int... lines) {
		return compileVer(VER_1_0, lang, arch, lines);
	}

	public static String compileVer(String version, String lang, String arch, int... lines) {
		int mult = CLMockProcess.getArchitectureMultiplier(arch) * CLMockProcess.getLanguageMockMultipler(lang);
		List<String> res = new ArrayList<>();
		res.add(MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_OBJ, arch));
		res.add("#version " + version);
		for (int l : lines) {
			res.add(l * mult + "");
		}
		return StringUtils.toStringJoin(null, LINE_SEPARATOR, res, LINE_SEPARATOR);
	}

	public static String binaryX64Exe1_0(BinaryLine... lines) {
		return binaryImpl(MockingMSVCTestMetric.TYPE_EXE, "x64", "1.0", lines);
	}

	public static String binaryX86Exe1_0(BinaryLine... lines) {
		return binaryImpl(MockingMSVCTestMetric.TYPE_EXE, "x86", "1.0", lines);
	}

	private static String binaryImpl(String type, String arch, String version, BinaryLine... lines) {
		List<String> vals = new ArrayList<>();

		vals.add(MockingMSVCTestMetric.createFileTypeLine(type, arch));
		vals.add("#version " + version);
		for (BinaryLine l : lines) {
			l.process(vals, arch, version);
		}

		return StringUtils.toStringJoin(null, LINE_SEPARATOR, vals, LINE_SEPARATOR);
	}

	public static BinaryLine c(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_C * val
							* CLMockProcess.getArchitectureMultiplier(architecture) + "");
				}
			}
		};
	}

	public static BinaryLine cpp(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP * val
							* CLMockProcess.getArchitectureMultiplier(architecture) + "");
				}
			}
		};
	}

	public static BinaryLine lib(int... vals) {
		return new BinaryLine() {
			@Override
			public void process(List<String> output, String architecture, String version) {
				for (int val : vals) {
					output.add(val + "");
				}
			}
		};
	}

	@FunctionalInterface
	public interface BinaryLine {
		public void process(List<String> output, String architecture, String version);
	}

	protected void assertHeaderPrecompilationWasntRun() {
		for (Entry<List<String>, Long> entry : getMetric().getProcessInvocationFrequencies().entrySet()) {
			if (entry.getKey().contains("/Yc")) {
				throw new AssertionError("Header was precompiled: " + entry.getKey());
			}
		}
	}

	protected void assertHeaderPrecompilationRunOnlyOnce() {
		for (Entry<List<String>, Long> entry : getMetric().getProcessInvocationFrequencies().entrySet()) {
			if (entry.getKey().contains("/Yc")) {
				if (entry.getValue() > 1) {
					fail("Precompiled more than once: " + entry.getKey());
				}
			}
		}
	}

}
