package testing.saker.msvc.tests.compile;

import java.util.Map;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ObjectUtils;
import testing.saker.SakerTest;
import testing.saker.msvc.tests.MSVCTestCase;

@SakerTest
public class MacroDefineTest extends MSVCTestCase {
	private static final SakerPath PATH_MAINC_OBJ = PATH_BUILD_DIRECTORY
			.resolve("saker.msvc.ccompile/default/x64/main.c.obj");

	private int macroVal;

	@Override
	protected Map<String, ?> getTaskVariables() {
		TreeMap<String, Object> result = ObjectUtils.newTreeMap(super.getTaskVariables());
		result.put("test.macro.val", macroVal);
		return result;
	}

	@Override
	protected void runTestImpl() throws Throwable {
		macroVal = 456;
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123, macroVal));

		runScriptTask("build");
		assertEmpty(getMetric().getRunTaskIdFactories());

		macroVal = 789;
		runScriptTask("build");
		assertEquals(files.getAllBytes(PATH_MAINC_OBJ).toString(), compile(LANG_C, ARCH_X64, 123, macroVal));
	}

}
