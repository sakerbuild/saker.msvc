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
package saker.msvc.impl.coptions.preset;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Collection;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.io.SerialUtils;

public class SimpleCOptionsPresetTaskOutput implements COptionsPresetTaskOutput, Externalizable {
	private static final long serialVersionUID = 1L;

	private Collection<PresetCOptions> presets;

	/**
	 * For {@link Externalizable}.
	 */
	public SimpleCOptionsPresetTaskOutput() {
	}

	public SimpleCOptionsPresetTaskOutput(Collection<? extends PresetCOptions> presets) {
		this.presets = ImmutableUtils.makeImmutableLinkedHashSet(presets);
	}

	@Override
	public Collection<PresetCOptions> getPresets() {
		return presets;
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
		SerialUtils.writeExternalCollection(out, presets);
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
		presets = SerialUtils.readExternalImmutableLinkedHashSet(in);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((presets == null) ? 0 : presets.hashCode());
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
		SimpleCOptionsPresetTaskOutput other = (SimpleCOptionsPresetTaskOutput) obj;
		if (presets == null) {
			if (other.presets != null)
				return false;
		} else if (!presets.equals(other.presets))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[" + (presets != null ? "presets=" + presets : "") + "]";
	}

}
