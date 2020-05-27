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
package testing.saker.msvc.tests.link;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class GenerateWinmdTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.cpp.obj");
	private static final SakerPath PATH_EXE = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/default/x64/default.exe");
	private static final SakerPath PATH_WINMD = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.clink/default/x64/default.winmd");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		String winmdcontent1 = winmd(PATH_MAINCPP_OBJ);
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langCpp(123)));
		assertEquals(files.getAllBytes(PATH_WINMD).toString(), winmdcontent1);

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.cpp"), "456".getBytes());
		runScriptTask("build");
		String winmdcontent2 = winmd(PATH_MAINCPP_OBJ);
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langCpp(456)));
		assertEquals(files.getAllBytes(PATH_WINMD).toString(), winmdcontent2);
		assertNotEquals(winmdcontent1, winmdcontent2);
	}
}
