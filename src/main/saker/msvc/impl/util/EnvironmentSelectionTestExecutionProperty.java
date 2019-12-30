package saker.msvc.impl.util;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.runtime.execution.ExecutionProperty;
import saker.build.task.EnvironmentSelectionResult;
import saker.build.task.TaskExecutionEnvironmentSelector;

public class EnvironmentSelectionTestExecutionProperty
		implements ExecutionProperty<EnvironmentSelectionResult>, Externalizable {
	private static final long serialVersionUID = 1L;

	private TaskExecutionEnvironmentSelector selector;

	/**
	 * For {@link Externalizable}.
	 */
	public EnvironmentSelectionTestExecutionProperty() {
	}

	public EnvironmentSelectionTestExecutionProperty(TaskExecutionEnvironmentSelector selector) {
		this.selector = selector;
	}

	@Override
	public EnvironmentSelectionResult getCurrentValue(ExecutionContext executioncontext) throws Exception {
		return executioncontext.testEnvironmentSelection(selector, null);
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		out.writeObject(selector);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		selector = (TaskExecutionEnvironmentSelector) in.readObject();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((selector == null) ? 0 : selector.hashCode());
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
		EnvironmentSelectionTestExecutionProperty other = (EnvironmentSelectionTestExecutionProperty) obj;
		if (selector == null) {
			if (other.selector != null)
				return false;
		} else if (!selector.equals(other.selector))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (selector != null ? "selector=" + selector : "") + "]";
	}

}
