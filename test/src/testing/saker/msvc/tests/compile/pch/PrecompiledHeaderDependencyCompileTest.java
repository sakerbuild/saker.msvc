package testing.saker.msvc.tests.compile.pch;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class PrecompiledHeaderDependencyCompileTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINCPP_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.cpp.obj");

	@Override
	protected void runTestImpl() throws Throwable {
		SakerPath pchpath = PATH_WORKING_DIRECTORY.resolve("pch/pch.h");
		SakerPath secondpath = PATH_WORKING_DIRECTORY.resolve("pch/second.h");
		SakerPath maincpppath = PATH_WORKING_DIRECTORY.resolve("src/main.cpp");

		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 999, 222, 123));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		files.putFile(secondpath, "888".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 888, 222, 123));

		files.putFile(maincpppath, files.getAllBytes(maincpppath).toString().replace("123", "456"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 888, 222, 456));

		files.putFile(pchpath, files.getAllBytes(pchpath).toString().replace("222", "333"));
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINCPP_OBJ).toString(), compile(LANG_CPP, ARCH_X64, 888, 333, 456));
	}
}
