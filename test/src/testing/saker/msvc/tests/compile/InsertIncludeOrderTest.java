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
