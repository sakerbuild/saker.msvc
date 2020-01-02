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
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collections;
import java.util.Set;

import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.IndeterminateSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKReference;

public class VersionsMSVCSDKDescription implements IndeterminateSDKDescription, Externalizable {
	private static final long serialVersionUID = 1L;

	private Set<String> regularVersions;
	private Set<String> legacyVersions;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionsMSVCSDKDescription() {
	}

	private VersionsMSVCSDKDescription(Set<String> regularVersions, Set<String> legacyVersions) {
		this.regularVersions = regularVersions;
		this.legacyVersions = legacyVersions;
	}

	public static SDKDescription create(Set<String> regularVersions, Set<String> legacyVersions) {
		//if only a single version possible, we don't need indeterminate sdk description
		if (regularVersions != null && legacyVersions != null) {
			if ((regularVersions.isEmpty() && legacyVersions.size() <= 1)
					|| (legacyVersions.isEmpty() && regularVersions.size() <= 1)) {
				return EnvironmentSDKDescription
						.create(new VersionsMSVCSDKReferenceEnvironmentProperty(regularVersions, legacyVersions));
			}
			//else there are at least 2 versions specified in the sets
		}
		return new VersionsMSVCSDKDescription(regularVersions, legacyVersions);
	}

	@Override
	public SDKDescription getBaseSDKDescription() {
		return EnvironmentSDKDescription
				.create(new VersionsMSVCSDKReferenceEnvironmentProperty(regularVersions, legacyVersions));
	}

	@Override
	public SDKDescription pinSDKDescription(SDKReference sdkreference) {
		if (sdkreference instanceof RegularLayoutVCToolsSDKReference) {
			return create(Collections.singleton(((AbstractVCToolsSDKReference) sdkreference).getVersion()),
					Collections.emptySet());
		}
		if (sdkreference instanceof LegacyLayoutVCToolsSDKReference) {
			return create(Collections.emptySet(),
					Collections.singleton(((AbstractVCToolsSDKReference) sdkreference).getVersion()));
		}
		//shouldn't happen, but handle just in case
		return getBaseSDKDescription();
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
		VersionsMSVCSDKDescription other = (VersionsMSVCSDKDescription) obj;
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
