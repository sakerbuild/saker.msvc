package saker.msvc.impl.sdk;

import java.io.Externalizable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Predicate;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.EnvironmentProperty;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.msvc.impl.MSVCUtils;
import saker.sdk.support.api.SDKReference;
import testing.saker.msvc.TestFlag;

public class VersionsWindowsKitsSDKReferenceEnvironmentProperty
		implements EnvironmentProperty<SDKReference>, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final String VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX = "saker.msvc.windowskits.sdk.install.location.";

	private Set<String> versions;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionsWindowsKitsSDKReferenceEnvironmentProperty() {
	}

	public VersionsWindowsKitsSDKReferenceEnvironmentProperty(Set<String> version) {
		this.versions = version;
	}

	@Override
	public SDKReference getCurrentValue(SakerEnvironment environment) throws Exception {
		Predicate<? super String> versionpredicate = MSVCUtils.getSDKVersionsPredicate(versions);
		if (TestFlag.ENABLED) {
			for (String v : TestFlag.metric().getPresentWindowsKitsSDKVersions(environment)) {
				if (versionpredicate.test(v)) {
					return new WindowsKitsSDKReference(TestFlag.metric().getWindowsKitsSDKBasePath(environment, v), v);
				}
			}
		}
		
		for (Entry<String, String> entry : environment.getUserParameters().entrySet()) {
			String key = entry.getKey();
			if (!key.startsWith(VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX)) {
				continue;
			}
			String verstr = key.substring(VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX.length());
			if (versionpredicate.test(verstr)) {
				SakerPath installdir = SakerPath.valueOf(entry.getKey());
				return new WindowsKitsSDKReference(installdir, verstr);
			}
		}

		WindowsKitsSDKReference sdkref;
		sdkref = MSVCUtils.searchWindowsKitsInProgramFiles(MSVCUtils.PATH_PROGRAM_FILES_X86, versionpredicate);
		if (sdkref != null) {
			return sdkref;
		}
		sdkref = MSVCUtils.searchWindowsKitsInProgramFiles(MSVCUtils.PATH_PROGRAM_FILES, versionpredicate);
		if (sdkref != null) {
			return sdkref;
		}
		throw new FileNotFoundException("Windows Kits SDK not found for versions: " + versions);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, versions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		versions = SerialUtils.readExternalImmutableNavigableSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((versions == null) ? 0 : versions.hashCode());
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
		VersionsWindowsKitsSDKReferenceEnvironmentProperty other = (VersionsWindowsKitsSDKReferenceEnvironmentProperty) obj;
		if (versions == null) {
			if (other.versions != null)
				return false;
		} else if (!versions.equals(other.versions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + versions + "]";
	}

}
