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
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class PresetCompileTest extends MSVCTestCase {
	private static final SakerPath PATH_EXE_X64 = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.clink/default/x64/default.exe");
	private static final SakerPath PATH_EXE_X86 = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.clink/default/x86/default.exe");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_EXE_X64).toString(),
				binaryX64Exe1_0(c(123, 2001), lib(2002), c(1001), lib(1002)));

		runScriptTask("buildx86");
		assertEquals(files.getAllBytes(PATH_EXE_X86).toString(),
				binaryX86Exe1_0(c(123, 2001), lib(2003), c(1001), lib(1003)));
	}

}
