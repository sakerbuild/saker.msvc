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

public class VersionsWindowsKitsSDKDescription implements IndeterminateSDKDescription, Externalizable {
	private static final long serialVersionUID = 1L;

	private Set<String> versions;

	/**
	 * For {@link Externalizable}.
	 */
	public VersionsWindowsKitsSDKDescription() {
	}

	private VersionsWindowsKitsSDKDescription(Set<String> versions) {
		this.versions = versions;
	}

	public static SDKDescription create(Set<String> versions) {
		if (versions != null && versions.size() == 1) {
			//only a single version possible, we don't need indeterminate sdk description
			return EnvironmentSDKDescription.create(new VersionsWindowsKitsSDKReferenceEnvironmentProperty(versions));
		}
		return new VersionsWindowsKitsSDKDescription(versions);
	}

	@Override
	public SDKDescription getBaseSDKDescription() {
		return EnvironmentSDKDescription.create(new VersionsWindowsKitsSDKReferenceEnvironmentProperty(versions));
	}

	@Override
	public SDKDescription pinSDKDescription(SDKReference sdkreference) {
		if (sdkreference instanceof WindowsKitsSDKReference) {
			return EnvironmentSDKDescription.create(new VersionsWindowsKitsSDKReferenceEnvironmentProperty(
					Collections.singleton(((WindowsKitsSDKReference) sdkreference).getVersion())));
		}
		//shouldn't happen, but handle just in case
		return getBaseSDKDescription();
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
		VersionsWindowsKitsSDKDescription other = (VersionsWindowsKitsSDKDescription) obj;
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
