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
public class SimpleClusterCompileTest extends MSVCTestCase {
	public static final String DEFAULT_CLUSTER_NAME = "cluster";

	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

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
		runScriptTask("build");
		assertMap(baseMetric.getCompiledFileClusterNames()).contains(PATH_WORKING_DIRECTORY.resolve("main.c"),
				SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME);
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123));
	}

}
