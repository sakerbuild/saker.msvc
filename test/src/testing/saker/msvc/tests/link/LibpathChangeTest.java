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
public class LibpathChangeTest extends MSVCTestCase {
	private static final SakerPath PATH_EXE = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/default/x64/default.exe");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_EXE).toString(), binaryX64Exe1_0(c(123), lib(456)));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("libpath/mylib.lib"), "lib_x64\n789");
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_EXE).toString(), binaryX64Exe1_0(c(123), lib(789)));
	}

}
