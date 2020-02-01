package testing.saker.msvc.tests.compile.pch;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class OncePrecompileHeaderCompileTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.cpp.obj");
	private static final SakerPath PATH_MAINCPP2_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main2.cpp.obj");
	private static final SakerPath PATH_MAINCPP3_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main3.cpp.obj");
	private static final SakerPath PATH_MAINCPP4_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main4.cpp.obj");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("main.cpp");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 222, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 222, 123));
		assertPrecompilationRunOnlyOnce();

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("pch.h"), "333".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertPrecompilationRunOnlyOnce();
		
		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 456));
		assertEquals(files.getAllBytes(PATH_MAINCPP2_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP3_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
		assertEquals(files.getAllBytes(PATH_MAINCPP4_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 333, 123));
	}

}
