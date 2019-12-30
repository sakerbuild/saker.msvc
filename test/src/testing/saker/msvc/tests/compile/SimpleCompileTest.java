package testing.saker.msvc.tests.compile;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class SimpleCompileTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.cpp.obj");
	private static final SakerPath PATH_EXE = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/default/x64/default.exe");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langC(123), langCpp(123)));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.c"), "456".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langC(456), langCpp(123)));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("main.cpp"), "456".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 456));
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langC(456), langCpp(456)));

		files.putFile(PATH_WORKING_DIRECTORY.resolve("add.c"), "1".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_EXE).toString(), linkExe(ARCH_X64, langC(1), langC(456), langCpp(456)));
	}

}
