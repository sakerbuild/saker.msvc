package saker.msvc.impl.ccompile;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Objects;

import saker.build.runtime.environment.EnvironmentProperty;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.TaskExecutionEnvironmentSelector;
import saker.build.thirdparty.saker.util.io.SerialUtils;
import saker.msvc.impl.MSVCUtils;
import saker.sdk.support.api.EnvironmentSDKDescription;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKDescriptionVisitor;
import saker.sdk.support.api.SDKReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.sdk.support.api.UserSDKDescription;
import saker.std.api.environment.qualifier.AnyEnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifier;
import saker.std.api.environment.qualifier.EnvironmentQualifierVisitor;
import saker.std.api.environment.qualifier.PropertyEnvironmentQualifier;

public final class SDKBasedExecutionEnvironmentSelector implements TaskExecutionEnvironmentSelector, Externalizable {
	private static final long serialVersionUID = 1L;

	//TODO the names are not used.
	private NavigableMap<String, SDKDescription> descriptions;

	/**
	 * For {@link Externalizable}.
	 */
	public SDKBasedExecutionEnvironmentSelector() {
	}

	public SDKBasedExecutionEnvironmentSelector(NavigableMap<String, SDKDescription> descriptions) {
		this.descriptions = descriptions;
	}

	public NavigableMap<String, SDKDescription> getDescriptions() {
		return descriptions;
	}

	@Override
	public EnvironmentSelectionResult isSuitableExecutionEnvironment(SakerEnvironment environment) {
		Map<EnvironmentProperty<?>, Object> qualifierproperties = new HashMap<>();
		for (SDKDescription descr : descriptions.values()) {
			descr.accept(new SDKDescriptionVisitor() {
				@Override
				public void visit(EnvironmentSDKDescription description) {
					EnvironmentProperty<? extends SDKReference> envproperty = SDKSupportUtils
							.getEnvironmentSDKDescriptionReferenceEnvironmentProperty(description);
					SDKReference sdkref = environment.getEnvironmentPropertyCurrentValue(envproperty);
					qualifierproperties.put(envproperty, sdkref);
				}

				@Override
				public void visit(UserSDKDescription description) {
					EnvironmentQualifier qualifier = description.getQualifier();
					if (qualifier == null) {
						//if there's no qualifier, the environment selection shouldn't be done, as the
						//non clusterability should've been detected
						throw new AssertionError("Internal error: User SDK contains null Environment qualifier.");
					}
					qualifier.accept(new EnvironmentQualifierVisitor() {
						@Override
						public void visit(PropertyEnvironmentQualifier qualifier) {
							EnvironmentProperty<?> envproperty = qualifier.getEnvironmentProperty();
							Object currentval = environment.getEnvironmentPropertyCurrentValue(envproperty);
							Object expectedvalue = qualifier.getExpectedValue();
							if (Objects.equals(currentval, expectedvalue)) {
								qualifierproperties.put(envproperty, currentval);
							} else {
								throw new IllegalArgumentException(
										"Unsuitable environment, user SDK qualifier mismatch: " + currentval + " - "
												+ expectedvalue + " for property: " + envproperty);
							}
						}

						@Override
						public void visit(AnyEnvironmentQualifier qualifier) {
							//suitable.
						}
					});
				}
			});
		}
		return new EnvironmentSelectionResult(qualifierproperties);
	}

	public static SDKReference getResolvedSDKReference(SDKDescription description,
			EnvironmentSelectionResult selectionresult) {
		SDKReference[] result = { null };
		description.accept(new SDKDescriptionVisitor() {
			@Override
			public void visit(EnvironmentSDKDescription description) {
				EnvironmentProperty<? extends SDKReference> envproperty = SDKSupportUtils
						.getEnvironmentSDKDescriptionReferenceEnvironmentProperty(description);
				result[0] = (SDKReference) selectionresult.getQualifierEnvironmentProperties().get(envproperty);
			}
		});
		return result[0];
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalMap(out, descriptions);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		descriptions = SerialUtils.readExternalSortedImmutableNavigableMap(in, MSVCUtils.getSDKNameComparator());
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((descriptions == null) ? 0 : descriptions.hashCode());
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
		SDKBasedExecutionEnvironmentSelector other = (SDKBasedExecutionEnvironmentSelector) obj;
		if (descriptions == null) {
			if (other.descriptions != null)
				return false;
		} else if (!descriptions.equals(other.descriptions))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SDKBasedExecutionEnvironmentSelector[" + (descriptions != null ? "descriptions=" + descriptions : "")
				+ "]";
	}
}