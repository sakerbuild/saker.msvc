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

public class VersionsMSVCSDKReferenceEnvironmentProperty implements EnvironmentProperty<SDKReference>, Externalizable {
	private static final long serialVersionUID = 1L;

	public static final String VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX = "saker.msvc.sdk.install.location.";
	public static final String VERSIONED_LEGACY_INSTALL_LOCATION_ENV_PARAMETER_PREFIX = "saker.msvc.legacy.sdk.install.location.";

	private Set<String> regularVersions;
	private Set<String> legacyVersions;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionsMSVCSDKReferenceEnvironmentProperty() {
	}

	public VersionsMSVCSDKReferenceEnvironmentProperty(Set<String> regularVersions, Set<String> legacyVersions) {
		this.regularVersions = regularVersions;
		this.legacyVersions = legacyVersions;
	}

	@Override
	public SDKReference getCurrentValue(SakerEnvironment environment) throws Exception {
		if (TestFlag.ENABLED) {
			for (String v : TestFlag.metric().getPresentMSVCSDKVersions(environment)) {
				if (!TestFlag.metric().isMSVCSDKLegacy(environment, v)) {
					if (MSVCUtils.getSDKVersionsPredicate(regularVersions).test(v)) {
						return new RegularLayoutVCToolsSDKReference(v,
								TestFlag.metric().getMSVCSDKBasePath(environment, v));
					}
				}
			}
			for (String v : TestFlag.metric().getPresentMSVCSDKVersions(environment)) {
				if (TestFlag.metric().isMSVCSDKLegacy(environment, v)) {
					if (MSVCUtils.getSDKVersionsPredicate(legacyVersions).test(v)) {
						return new LegacyLayoutVCToolsSDKReference(v,
								TestFlag.metric().getMSVCSDKBasePath(environment, v));
					}
				}
			}
		}

		SDKReference sdkref;
		if (regularVersions == null || !regularVersions.isEmpty()) {
			Predicate<? super String> versionpredicate = MSVCUtils.getSDKVersionsPredicate(regularVersions);
			for (Entry<String, String> entry : environment.getUserParameters().entrySet()) {
				String key = entry.getKey();
				if (!key.startsWith(VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX)) {
					continue;
				}
				String verstr = key.substring(VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX.length());
				if (versionpredicate.test(verstr)) {
					SakerPath installdir = SakerPath.valueOf(entry.getValue());
					return new RegularLayoutVCToolsSDKReference(verstr, installdir);
				}
			}

			sdkref = MSVCUtils.searchMSVCRegularToolchainInStudioDir(
					MSVCUtils.PATH_PROGRAM_FILES_X86_MICROSOFT_VISUAL_STUDIO, versionpredicate);
			if (sdkref != null) {
				return sdkref;
			}
			sdkref = MSVCUtils.searchMSVCRegularToolchainInStudioDir(
					MSVCUtils.PATH_PROGRAM_FILES_MICROSOFT_VISUAL_STUDIO, versionpredicate);
			if (sdkref != null) {
				return sdkref;
			}
		}
		if (legacyVersions == null || !legacyVersions.isEmpty()) {
			Predicate<? super String> versionpredicate = MSVCUtils.getSDKVersionsPredicate(legacyVersions);
			for (Entry<String, String> entry : environment.getUserParameters().entrySet()) {
				String key = entry.getKey();
				if (!key.startsWith(VERSIONED_LEGACY_INSTALL_LOCATION_ENV_PARAMETER_PREFIX)) {
					continue;
				}
				String verstr = key.substring(VERSIONED_INSTALL_LOCATION_ENV_PARAMETER_PREFIX.length());
				if (versionpredicate.test(verstr)) {
					SakerPath installdir = SakerPath.valueOf(entry.getValue());
					return new LegacyLayoutVCToolsSDKReference(verstr, installdir);
				}
			}

			sdkref = MSVCUtils.searchMSVCLegacyToolchainInProgramFiles(MSVCUtils.PATH_PROGRAM_FILES_X86,
					versionpredicate);
			if (sdkref != null) {
				return sdkref;
			}
			sdkref = MSVCUtils.searchMSVCLegacyToolchainInProgramFiles(MSVCUtils.PATH_PROGRAM_FILES, versionpredicate);
			if (sdkref != null) {
				return sdkref;
			}
		}
		throw new FileNotFoundException("MSVC SDK not found for regular versions: " + regularVersions
				+ " and legacy versions: " + legacyVersions);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, regularVersions);
		SerialUtils.writeExternalCollection(out, legacyVersions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		regularVersions = SerialUtils.readExternalImmutableNavigableSet(in);
		legacyVersions = SerialUtils.readExternalImmutableNavigableSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((legacyVersions == null) ? 0 : legacyVersions.hashCode());
		result = prime * result + ((regularVersions == null) ? 0 : regularVersions.hashCode());
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
		VersionsMSVCSDKReferenceEnvironmentProperty other = (VersionsMSVCSDKReferenceEnvironmentProperty) obj;
		if (legacyVersions == null) {
			if (other.legacyVersions != null)
				return false;
		} else if (!legacyVersions.equals(other.legacyVersions))
			return false;
		if (regularVersions == null) {
			if (other.regularVersions != null)
				return false;
		} else if (!regularVersions.equals(other.regularVersions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + "regularVersions=" + regularVersions + ", " + "legacyVersions="
				+ legacyVersions + "]";
	}

}
