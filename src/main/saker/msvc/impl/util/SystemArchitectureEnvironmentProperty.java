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
package saker.msvc.impl.util;

import java.io.ByteArrayOutputStream;
import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

import saker.build.file.provider.LocalFileProvider;
import saker.build.runtime.environment.EnvironmentProperty;
import saker.build.runtime.environment.SakerEnvironment;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.function.LazySupplier;
import saker.build.thirdparty.saker.util.io.StreamUtils;
import saker.nest.bundle.NestBundleClassLoader;
import testing.saker.msvc.TestFlag;

public final class SystemArchitectureEnvironmentProperty implements EnvironmentProperty<String>, Externalizable {
	private static final Supplier<String> ARCHITECTURE_READ_SUPPLIER = LazySupplier
			.of(SystemArchitectureEnvironmentProperty::readProcessorArchitecture);
	private static final long serialVersionUID = 1L;

	public static final SystemArchitectureEnvironmentProperty INSTANCE = new SystemArchitectureEnvironmentProperty();

	/**
	 * For {@link Externalizable}.
	 */
	public SystemArchitectureEnvironmentProperty() {
	}

	@Override
	public String getCurrentValue(SakerEnvironment environment) throws Exception {
		return ARCHITECTURE_READ_SUPPLIER.get();
	}

	private static String readProcessorArchitecture() {
		if (TestFlag.ENABLED) {
			return TestFlag.metric().getSystemArchitecture();
		}

		NestBundleClassLoader cl = (NestBundleClassLoader) SystemArchitectureEnvironmentProperty.class.getClassLoader();
		Path filepath = cl.getBundle().getBundleStoragePath().resolve("host_arch");
		try {
			String arch = LocalFileProvider.getInstance().getAllBytes(filepath).toString();
			if ("x64".equalsIgnoreCase(arch)) {
				return "x64";
			}
			if ("x86".equalsIgnoreCase(arch)) {
				return "x86";
			}
			//unrecognized architecture
		} catch (IOException e2) {
		}
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			ProcessBuilder pb = new ProcessBuilder("systeminfo", "/FO", "LIST");
			Process proc = pb.start();
			StreamUtils.copyStream(proc.getInputStream(), baos);
			int res = proc.waitFor();
			if (res != 0) {
				throw new IOException("Failed to execute systeminfo to determine system architecture.");
			}
			String val = baos.toString();
			int index = val.indexOf("System Type:");
			if (index < 0) {
				throw new IOException("\"System Type:\" not found in systeminfo command output.");
			}
			val = val.substring(index + 12, val.indexOf('\n', index));
			String result;
			if (val.contains("x64")) {
				result = "x64";
			} else if (val.contains("x86")) {
				result = "x86";
			} else {
				throw new IOException("Failed to determine system type from value: " + val);
			}
			try {
				LocalFileProvider.getInstance().createDirectories(filepath.getParent());
				Files.write(filepath, result.getBytes(StandardCharsets.UTF_8));
			} catch (IOException e) {
			}
			return result;
		} catch (IOException e) {
			throw new UncheckedIOException("Failed to execute systeminfo.", e);
		} catch (InterruptedException e) {
			throw new RuntimeException("Failed to execute systeminfo.", e);
		}
	}

	@Override
	public void writeExternal(ObjectOutput out) throws IOException {
	}

	@Override
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {
	}

	@Override
	public int hashCode() {
		return getClass().getName().hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return ObjectUtils.isSameClass(this, obj);
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "[]";
	}

}
