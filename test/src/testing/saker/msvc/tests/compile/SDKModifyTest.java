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
package testing.saker.msvc.tests.compile;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.function.Functionals;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;
import testing.saker.msvc.tests.mock.EnvironmentSDKMockingMSVCTestMetric;
import testing.saker.msvc.tests.mock.MockingMSVCTestMetric;

@SakerTest
public class SDKModifyTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	private String version;

	@Override
	public void executeRunning() throws Exception {
		version = "1.0";
		super.executeRunning();
	}

	@Override
	protected MockingMSVCTestMetric createMetricImpl() {
		EnvironmentSDKMockingMSVCTestMetric result = new EnvironmentSDKMockingMSVCTestMetric(getTestSDKDirectory());
		result.addMSVCDefaultSDK(null, version, false);
		return result;
	}

	@Override
	protected void runTestImpl() throws Throwable {
		version = "1.0";

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compileVer(version, LANG_C, ARCH_X64, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		environment.invalidateEnvironmentPropertiesWaitExecutions(Functionals.alwaysPredicate());
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		version = "2.0";
		environment.invalidateEnvironmentPropertiesWaitExecutions(Functionals.alwaysPredicate());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compileVer(version, LANG_C, ARCH_X64, 123));
	}

}
