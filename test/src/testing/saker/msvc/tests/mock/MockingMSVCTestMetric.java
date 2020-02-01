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
package testing.saker.msvc.tests.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.LongAdder;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ObjectUtils;
import testing.saker.build.tests.CollectingTestMetric;
import testing.saker.build.tests.EnvironmentTestCase;
import testing.saker.msvc.MSVCTestMetric;

public class MockingMSVCTestMetric extends CollectingTestMetric implements MSVCTestMetric {
	public static final int MOCK_MULTIPLIER_LANGUAGE_C = 29;
	public static final int MOCK_MULTIPLIER_LANGUAGE_CPP = 31;

	public static final String DEFAULT_VERSION = "1.0";

	public static final String TYPE_OBJ = "obj";
	public static final String TYPE_DLL = "dll";
	public static final String TYPE_EXE = "exe";
	public static final String TYPE_LIB = "lib";

	protected Path testSDKDirectory;

	protected ConcurrentSkipListMap<SakerPath, String> compiledFileClusterNames = new ConcurrentSkipListMap<>();
	protected ConcurrentHashMap<List<String>, LongAdder> runCommands = new ConcurrentHashMap<>();

	public MockingMSVCTestMetric(Path testSDKDirectory) {
		this.testSDKDirectory = testSDKDirectory;
	}

	@Override
	public int runProcess(List<String> command, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		runCommands.computeIfAbsent(command, x -> new LongAdder()).increment();
		System.out.println("MockingMSVCTestMetric.startProcess() " + command);
		SakerPath exepath = SakerPathFiles.requireAbsolutePath(SakerPath.valueOf(command.get(0)));
		if (exepath.getFileName().equalsIgnoreCase("cl.exe")) {
			return CLMockProcess.run(command, mergestderr, stdoutconsumer, stderrconsumer);
		}
		if (exepath.getFileName().equalsIgnoreCase("link.exe")) {
			return LinkMockProcess.run(command, mergestderr, stdoutconsumer, stderrconsumer);
		}
		throw new IOException("Exe not found: " + command);
	}

	public Map<List<String>, Long> getProcessInvocationFrequencies() {
		HashMap<List<String>, Long> result = new HashMap<>();
		for (Entry<List<String>, LongAdder> entry : runCommands.entrySet()) {
			result.put(entry.getKey(), entry.getValue().longValue());
		}
		return result;
	}

	@Override
	public Set<String> getPresentMSVCSDKVersions(SakerEnvironment environment) {
		return Collections.singleton(DEFAULT_VERSION);
	}

	@Override
	public Set<String> getPresentWindowsKitsSDKVersions(SakerEnvironment environment) {
		return Collections.singleton(DEFAULT_VERSION);
	}

	@Override
	public SakerPath getWindowsKitsSDKBasePath(SakerEnvironment environment, String version) {
		return SakerPath.valueOf(testSDKDirectory).resolve("windowskits");
	}

	@Override
	public SakerPath getMSVCSDKBasePath(SakerEnvironment environment, String version) {
		return SakerPath.valueOf(testSDKDirectory.resolve("msvc").resolve(version));
	}

	@Override
	public boolean isMSVCSDKLegacy(SakerEnvironment environment, String version) {
		return false;
	}

	@Override
	public void compiling(SakerPath path, SakerEnvironment environment) {
		compiledFileClusterNames.put(path, ObjectUtils
				.nullDefault(environment.getUserParameters().get(EnvironmentTestCase.TEST_CLUSTER_NAME_ENV_PARAM), ""));
	}

	public ConcurrentSkipListMap<SakerPath, String> getCompiledFileClusterNames() {
		return compiledFileClusterNames;
	}

	public static String getMSVCExeHostArchitecture(SakerPath command) {
		String n = command.getName(command.getNameCount() - 3);
		if (!n.startsWith("Host")) {
			throw new IllegalArgumentException(command.toString());
		}
		return n.substring(4);
	}

	public static String getMSVCExeTargetArchitecture(SakerPath command) {
		return command.getName(command.getNameCount() - 2);
	}

	public static String getMSVCExeVersion(SakerPath command) {
		return command.getName(command.getNameCount() - 5);
	}

	public static String createFileTypeLine(String type, String arch) {
		return type + "_" + arch;
	}
}
