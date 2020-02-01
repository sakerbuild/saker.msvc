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
package testing.saker.msvc.tests.cluster.compile;

import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import testing.saker.SakerTest;
import testing.saker.build.tests.EnvironmentTestCaseConfiguration;
import testing.saker.msvc.tests.MSVCTestCase;
import testing.saker.msvc.tests.mock.EnvironmentSDKMockingMSVCTestMetric;
import testing.saker.msvc.tests.mock.MockingMSVCTestMetric;

@SakerTest
public class ClusterSDKModifyTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	private EnvironmentSDKMockingMSVCTestMetric baseMetric;

	private String version;

	@Override
	protected Set<EnvironmentTestCaseConfiguration> getTestConfigurations() {
		return EnvironmentTestCaseConfiguration.builder(super.getTestConfigurations())
				.setClusterNames(ImmutableUtils.singletonSet(DEFAULT_CLUSTER_NAME)).build();
	}

	@Override
	public void executeRunning() throws Exception {
		version = "1.0";
		baseMetric = new EnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
		initDefaultSDK(baseMetric);
		testing.saker.build.flag.TestFlag.set(baseMetric);
		super.executeRunning();
	}

	@Override
	protected MockingMSVCTestMetric createMetricImpl() {
		EnvironmentSDKMockingMSVCTestMetric result = new EnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
		initDefaultSDK(result);
		return result;
	}

	private void initDefaultSDK(EnvironmentSDKMockingMSVCTestMetric result) {
		result.clearMSVCSDKs(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME);
		result.addMSVCDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, version, false);
	}

	@Override
	protected void runTestImpl() throws Throwable {
		version = "1.0";
		initDefaultSDK(baseMetric);

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compileVer(version, LANG_C, ARCH_X64, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		clearClusterEnvironmentCachedProperties(DEFAULT_CLUSTER_NAME);
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		version = "2.0";
		initDefaultSDK(baseMetric);
		clearClusterEnvironmentCachedProperties(DEFAULT_CLUSTER_NAME);
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compileVer(version, LANG_C, ARCH_X64, 123));
	}

}
