package saker.msvc.impl.option;

import saker.sdk.support.api.SDKPathCollectionReference;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKPropertyCollectionReference;
import saker.sdk.support.api.SDKPropertyReference;

public abstract class SimpleParameterOption {
	public abstract void accept(Visitor visitor);

	@Override
	public abstract boolean equals(Object obj);

	@Override
	public abstract int hashCode();

	public interface Visitor {
		public default void visit(String value) {
			throw new UnsupportedOperationException("Unsupported simple parameter: " + value);
		}

		public default void visit(SDKPathCollectionReference value) {
			throw new UnsupportedOperationException("Unsupported simple parameter: " + value);
		}

		public default void visit(SDKPropertyCollectionReference value) {
			throw new UnsupportedOperationException("Unsupported simple parameter: " + value);
		}

		public default void visit(SDKPathReference value) {
			visit(SDKPathCollectionReference.valueOf(value));
		}

		public default void visit(SDKPropertyReference value) {
			visit(SDKPropertyCollectionReference.valueOf(value));
		}
	}

	public static SimpleParameterOption create(String value) {
		return new StringSimpleParameterOption(value);
	}

	public static SimpleParameterOption create(SDKPathCollectionReference value) {
		return new SDKPathCollectionSimpleParameterOption(value);
	}

	public static SimpleParameterOption create(SDKPropertyCollectionReference value) {
		return new SDKPropertyCollectionSimpleParameterOption(value);
	}

	public static SimpleParameterOption create(SDKPathReference value) {
		return new SDKPathReferenceSimpleParameterOption(value);
	}

	public static SimpleParameterOption create(SDKPropertyReference value) {
		return new SDKPropertyReferenceSimpleParameterOption(value);
	}
}
