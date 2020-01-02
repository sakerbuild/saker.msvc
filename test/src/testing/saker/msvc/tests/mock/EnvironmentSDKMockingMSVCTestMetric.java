/*
 * Copyright (C) 2020 Bence Sipka
 *
 * This program is free software: you can redistribute it and/or modify 
 * it under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, version 3.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package testing.saker.msvc.tests.mock;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.function.Functionals;
import testing.saker.msvc.tests.cluster.compile.SimpleClusterCompileTest;

public class EnvironmentSDKMockingMSVCTestMetric extends MockingMSVCTestMetric {
	private Map<String, Set<TestMSVCSDKConfig>> defaultClusterMSVCSDKs = new HashMap<>();
	private Map<String, Set<TestWindowsKitsSDKConfig>> defaultClusterWindowsKits = new HashMap<>();

	public EnvironmentSDKMockingMSVCTestMetric(Path testsdkdirectory) {
		super(testsdkdirectory);
		addMSVCDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, MockingMSVCTestMetric.DEFAULT_VERSION, false);
		addWindowsKitsDefaultSDK(SimpleClusterCompileTest.DEFAULT_CLUSTER_NAME, MockingMSVCTestMetric.DEFAULT_VERSION);
	}

	public void clearSDKs() {
		clearMSVCSDKs();
		clearWindowsKitsSDKs();
	}

	public void clearWindowsKitsSDKs() {
		defaultClusterWindowsKits.clear();
	}

	public void clearWindowsKitsSDKs(String clustername) {
		defaultClusterWindowsKits.remove(clustername);
	}

	public void clearMSVCSDKs() {
		defaultClusterMSVCSDKs.clear();
	}

	public void clearMSVCSDKs(String clustername) {
		defaultClusterMSVCSDKs.remove(clustername);
	}

	public void addWindowsKitsDefaultSDK(String clustername, String version) {
		defaultClusterWindowsKits.computeIfAbsent(clustername, Functionals.linkedHashSetComputer())
				.add(new TestWindowsKitsSDKConfig(version));
	}

	public void addMSVCDefaultSDK(String clustername, String version, boolean legacyLayout) {
		defaultClusterMSVCSDKs.computeIfAbsent(clustername, Functionals.linkedHashSetComputer())
				.add(new TestMSVCSDKConfig(version, legacyLayout));
	}

	@Override
	public Set<String> getPresentMSVCSDKVersions(SakerEnvironment environment) {
		String clustername = environment.getUserParameters().get(SimpleClusterCompileTest.TEST_CLUSTER_NAME_ENV_PARAM);
		Set<TestMSVCSDKConfig> sdk = defaultClusterMSVCSDKs.get(clustername);
		if (sdk == null) {
			throw new RuntimeException("SDK not found: " + clustername);
		}
		Set<String> result = new LinkedHashSet<>();
		for (TestMSVCSDKConfig s : sdk) {
			result.add(s.version);
		}
		return result;
	}

	@Override
	public Set<String> getPresentWindowsKitsSDKVersions(SakerEnvironment environment) {
		String clustername = environment.getUserParameters().get(SimpleClusterCompileTest.TEST_CLUSTER_NAME_ENV_PARAM);
		Set<TestWindowsKitsSDKConfig> sdk = defaultClusterWindowsKits.get(clustername);
		if (sdk == null) {
			throw new RuntimeException("SDK not found: " + clustername);
		}
		Set<String> result = new LinkedHashSet<>();
		for (TestWindowsKitsSDKConfig s : sdk) {
			result.add(s.version);
		}
		return result;
	}

	@Override
	public SakerPath getWindowsKitsSDKBasePath(SakerEnvironment environment, String version) {
		String clustername = environment.getUserParameters().get(SimpleClusterCompileTest.TEST_CLUSTER_NAME_ENV_PARAM);
		Set<TestWindowsKitsSDKConfig> sdk = defaultClusterWindowsKits.get(clustername);
		if (sdk == null) {
			throw new RuntimeException("SDK not found: " + clustername);
		}
		for (TestWindowsKitsSDKConfig s : sdk) {
			if (version.equals(s.version)) {
				return SakerPath.valueOf(testSDKDirectory).resolve("windowskits");
			}
		}
		throw new RuntimeException("SDK not found: " + clustername + " - " + version);
	}

	@Override
	public SakerPath getMSVCSDKBasePath(SakerEnvironment environment, String version) {
		String clustername = environment.getUserParameters().get(SimpleClusterCompileTest.TEST_CLUSTER_NAME_ENV_PARAM);
		Set<TestMSVCSDKConfig> sdk = defaultClusterMSVCSDKs.get(clustername);
		if (sdk == null) {
			throw new RuntimeException("SDK not found: " + clustername);
		}
		for (TestMSVCSDKConfig s : sdk) {
			if (version.equals(s.version)) {
				return SakerPath.valueOf(testSDKDirectory.resolve("msvc").resolve(s.version));
			}
		}
		throw new RuntimeException("SDK not found: " + clustername + " - " + version);
	}

	@Override
	public boolean isMSVCSDKLegacy(SakerEnvironment environment, String version) {
		String clustername = environment.getUserParameters().get(SimpleClusterCompileTest.TEST_CLUSTER_NAME_ENV_PARAM);
		Set<TestMSVCSDKConfig> sdk = defaultClusterMSVCSDKs.get(clustername);
		if (sdk == null) {
			throw new RuntimeException("SDK not found: " + clustername);
		}
		for (TestMSVCSDKConfig s : sdk) {
			if (version.equals(s.version)) {
				return s.legacyLayout;
			}
		}
		throw new RuntimeException("SDK not found: " + clustername + " - " + version);
	}

	public static class TestWindowsKitsSDKConfig {
		private String version;

		public TestWindowsKitsSDKConfig(String version) {
			this.version = version;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestWindowsKitsSDKConfig other = (TestWindowsKitsSDKConfig) obj;
			if (version == null) {
				if (other.version != null)
					return false;
			} else if (!version.equals(other.version))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TestWindowsKitsSDKConfig[" + (version != null ? "version=" + version : "") + "]";
		}
	}

	public static class TestMSVCSDKConfig {
		private String version;
		private boolean legacyLayout;

		public TestMSVCSDKConfig(String version, boolean legacyLayout) {
			this.version = version;
			this.legacyLayout = legacyLayout;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + (legacyLayout ? 1231 : 1237);
			result = prime * result + ((version == null) ? 0 : version.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			TestMSVCSDKConfig other = (TestMSVCSDKConfig) obj;
			if (legacyLayout != other.legacyLayout)
				return false;
			if (version == null) {
				if (other.version != null)
					return false;
			} else if (!version.equals(other.version))
				return false;
			return true;
		}

		@Override
		public String toString() {
			return "TestMSVCSDKConfig[" + (version != null ? "version=" + version + ", " : "") + "legacyLayout="
					+ legacyLayout + "]";
		}
	}
}