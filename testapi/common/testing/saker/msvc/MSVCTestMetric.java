package testing.saker.msvc;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;

public interface MSVCTestMetric {
	public default Process startProcess(ProcessBuilder pb) throws IOException {
		return pb.start();
	}

	public default Set<String> getPresentMSVCSDKVersions(SakerEnvironment environment) {
		return Collections.emptySet();
	}

	public default Set<String> getPresentWindowsKitsSDKVersions(SakerEnvironment environment) {
		return Collections.emptySet();
	}

	public default SakerPath getMSVCSDKBasePath(SakerEnvironment environment, String version) {
		throw new UnsupportedOperationException();
	}

	public default boolean isMSVCSDKLegacy(SakerEnvironment environment, String version) {
		throw new UnsupportedOperationException();
	}

	public default SakerPath getWindowsKitsSDKBasePath(SakerEnvironment environment, String version) {
		return null;
	}

	public default String getSystemArchitecture() {
		return "x64";
	}

	public default void compiling(SakerPath path, SakerEnvironment environment) {
	}
}
