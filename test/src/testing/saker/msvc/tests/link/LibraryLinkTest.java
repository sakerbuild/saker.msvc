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
public class LibraryLinkTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");
	private static final SakerPath PATH_DLL = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/default/x64/default.dll");
	private static final SakerPath PATH_DLL_UPPER = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/upper/x64/upper.dll");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_DLL).toString(), linkDll(ARCH_X64, langC(123)));
		
		runScriptTask("uppercase");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_DLL_UPPER).toString(), linkDll(ARCH_X64, langC(123)));
	}

}
