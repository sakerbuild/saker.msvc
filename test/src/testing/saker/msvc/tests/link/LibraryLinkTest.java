package testing.saker.msvc.tests.link;

import saker.build.file.path.SakerPath;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class LibraryLinkTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");
	private static final SakerPath PATH_DLL = PATH_BUILD_DIRECTORY.resolve("saker.msvc.clink/default/x64/default.dll");

	@Override
	protected void runTestImpl() throws Throwable {
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123));
		assertEquals(files.getAllBytes(PATH_DLL).toString(), linkDll(ARCH_X64, langC(123)));
	}

}
