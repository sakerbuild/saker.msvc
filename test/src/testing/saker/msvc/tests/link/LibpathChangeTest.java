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
