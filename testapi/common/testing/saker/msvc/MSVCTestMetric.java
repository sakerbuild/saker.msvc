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
package testing.saker.msvc;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import saker.build.file.path.SakerPath;
import saker.build.runtime.environment.SakerEnvironment;

public interface MSVCTestMetric {
	public default int runProcess(List<String> command, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		throw new UnsupportedOperationException();
	}

	public default Set<String> getPresentMSVCSDKVersions(SakerEnvironment environment) {
		return Collections.emptySet();
	}

	public default SakerPath getMSVCSDKBasePath(SakerEnvironment environment, String version) {
		throw new UnsupportedOperationException();
	}

	public default boolean isMSVCSDKLegacy(SakerEnvironment environment, String version) {
		throw new UnsupportedOperationException();
	}

	public default String getSystemArchitecture() {
		return "x64";
	}

	public default void compiling(SakerPath path, SakerEnvironment environment) {
	}

	@FunctionalInterface
	public interface MetricProcessIOConsumer {
		public void handleOutput(ByteBuffer bytes) throws IOException;
	}
}
