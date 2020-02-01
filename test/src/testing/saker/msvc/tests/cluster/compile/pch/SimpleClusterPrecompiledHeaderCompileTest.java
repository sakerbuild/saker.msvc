package testing.saker.msvc.tests.cluster.compile.pch;

import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.EnvironmentTestCaseConfiguration;
import testing.saker.msvc.tests.MSVCTestCase;
import testing.saker.msvc.tests.cluster.compile.SimpleClusterCompileTest;
import testing.saker.msvc.tests.mock.EnvironmentSDKMockingMSVCTestMetric;
import testing.saker.msvc.tests.mock.MockingMSVCTestMetric;

@SakerTest
public class SimpleClusterPrecompiledHeaderCompileTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.cpp.obj");

	private MockingMSVCTestMetric baseMetric;

	@Override
	protected Set<EnvironmentTestCaseConfiguration> getTestConfigurations() {
		return EnvironmentTestCaseConfiguration.builder(super.getTestConfigurations())
				.setClusterNames(ImmutableUtils.singletonSet(DEFAULT_CLUSTER_NAME)).build();
	}

	@Override
	public void executeRunning() throws Exception {
		baseMetric = new EnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
		testing.saker.build.flag.TestFlag.set(baseMetric);
		super.executeRunning();
	}

	@Override
	protected MockingMSVCTestMetric createMetricImpl() {
		return new EnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
	}

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");

		runScriptTask("build");
		assertMap(baseMetric.getCompiledFileClusterNames()).contains(maincpppath,
				SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME);
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 222, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("pch.h"), "333".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));

		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 456));
	}

}
