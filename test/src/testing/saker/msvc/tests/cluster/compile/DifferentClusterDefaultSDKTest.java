package testing.saker.msvc.tests.cluster.compile;

import java.nio.file.Path;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.EnvironmentTestCaseConfiguration;
import testing.saker.msvc.tests.MSVCTestCase;
import testing.saker.msvc.tests.mock.EnvironmentSDKMockingMSVCTestMetric;
import testing.saker.msvc.tests.mock.MockingMSVCTestMetric;

@SakerTest
public class DifferentClusterDefaultSDKTest extends MSVCTestCase {
	private static final class InnerClassClusterForcingEnvironmentSDKMockingMSVCTestMetric
			extends EnvironmentSDKMockingMSVCTestMetric {
		private InnerClassClusterForcingEnvironmentSDKMockingMSVCTestMetric(Path testsdkdirectory) {
			super(testsdkdirectory);
		}

		@Override
		public boolean isForceInnerTaskClusterInvocation(Object taskfactory) {
			if (taskfactory.getClass().getName()
					.equals("saker.msvc.impl.ccompile.MSVCCCompileWorkerTaskFactory$SourceCompilerInnerTaskFactory")) {
				return true;
			}
			return super.isForceInnerTaskClusterInvocation(taskfactory);
		}
	}

	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	private EnvironmentSDKMockingMSVCTestMetric baseMetric;

	@Override
	protected Set<EnvironmentTestCaseConfiguration> getTestConfigurations() {
		return EnvironmentTestCaseConfiguration.builder(super.getTestConfigurations())
				.setClusterNames(ImmutableUtils.singletonSet(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME)).build();
	}

	@Override
	public void executeRunning() throws Exception {
		baseMetric = new InnerClassClusterForcingEnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
		initMetric(baseMetric);
		testing.saker.build.flag.TestFlag.set(baseMetric);
		super.executeRunning();
	}

	@Override
	protected MockingMSVCTestMetric createMetricImpl() {
		EnvironmentSDKMockingMSVCTestMetric metric = new InnerClassClusterForcingEnvironmentSDKMockingMSVCTestMetric(
				getTestSDKDirectory());
		initMetric(metric);
		return metric;
	}

	private void initMetric(EnvironmentSDKMockingMSVCTestMetric metric) {
		metric.clearSDKs();
		metric.addMSVCDefaultSDK(null, "1.0", false);
		metric.addMSVCDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, "2.0", false);
		metric.addMSVCDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, "1.0", false);

		metric.addWindowsKitsDefaultSDK(null, "1.0");
		metric.addWindowsKitsDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, "1.0");
	}

	@Override
	protected void runTestImpl() throws Throwable {
		//the msvc sdk 1.0 is present on executor and cluster both. 2.0 only on the cluster
		//the compilation should be done on the cluster
		runScriptTask("build");
		assertEmpty(getMetric().getCompiledFileClusterNames());
		assertMap(baseMetric.getCompiledFileClusterNames()).contains(PATH_WORKING_DIRECTORY.resolve("main.c"),
				SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME);
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compileVer("1.0", LANG_C, ARCH_X64, 123));
	}

}
