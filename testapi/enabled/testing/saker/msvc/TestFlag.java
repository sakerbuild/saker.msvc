package testing.saker.msvc;

public class TestFlag {
	private static final MSVCTestMetric NULL_METRIC_INSTANCE = new MSVCTestMetric() {
	};
	public static final boolean ENABLED = true;

	public static MSVCTestMetric metric() {
		Object res = testing.saker.build.flag.TestFlag.metric();
		if (res instanceof MSVCTestMetric) {
			return (MSVCTestMetric) res;
		}
		return NULL_METRIC_INSTANCE;
	}

	private TestFlag() {
		throw new UnsupportedOperationException();
	}
}
