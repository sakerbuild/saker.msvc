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
public class InsertIncludeOrderTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	@Override
	protected void runTestImpl() throws Throwable {
		//create the dir as git doesnt track empty directories
		files.createDirectories(PATH_WORKING_DIRECTORY.resolve("inc1"));
		
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123, 456));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("inc1/header.h"), "10".getBytes());
		runScriptTask("build");
		assertNotEmpty(getMetric().getRunTaskIdFactories());
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 10, 456));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("inc2/header.h"), "20".getBytes());
		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.delete(PATH_WORKING_DIRECTORY.resolve("inc1/header.h"));
		runScriptTask("build");
		assertNotEmpty(getMetric().getRunTaskIdFactories());
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 20, 456));
	}

}
