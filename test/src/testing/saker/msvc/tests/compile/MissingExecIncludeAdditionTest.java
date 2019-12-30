package testing.saker.msvc.tests.compile;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class MissingExecIncludeAdditionTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	@Override
	protected void runTestImpl() throws Throwable {
		assertException(Exception.class, () -> runScriptTask("build"));

		//no changes, so nothing should be reinvoked
		assertException(Exception.class, () -> runScriptTask("build"));
		assertEmpty(getMetric().getRunTaskIdDeltas());

		files.putFile(PATH_WORKING_DIRECTORY.resolve("include/header.h"), "10".getBytes());
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 10, 456));
	}

}
