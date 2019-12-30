package testing.saker.msvc.tests.mock;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListMap;

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

	public MockingMSVCTestMetric(Path testSDKDirectory) {
		this.testSDKDirectory = testSDKDirectory;
	}

	@Override
	public Process startProcess(ProcessBuilder pb) throws IOException {
		List<String> command = pb.command();
		System.out.println("MockingMSVCTestMetric.startProcess() " + command);
		SakerPath exepath = SakerPathFiles.requireAbsolutePath(SakerPath.valueOf(command.get(0)));
		if (exepath.getFileName().equalsIgnoreCase("cl.exe")) {
			return CLMockProcess.run(pb);
		}
		if (exepath.getFileName().equalsIgnoreCase("link.exe")) {
			return LinkMockProcess.run(pb);
		}
		throw new IOException("Exe not found: " + command);
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
