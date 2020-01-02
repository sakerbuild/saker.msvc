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
package testing.saker.msvc.tests.mock;

import java.io.InputStream;
import java.io.OutputStream;

import saker.build.thirdparty.saker.util.io.StreamUtils;

public class MockedProcess extends Process {
	private int resultCode = 0;
	private InputStream output;

	public MockedProcess(int resultCode, InputStream output) {
		this.resultCode = resultCode;
		this.output = output;
	}

	@Override
	public OutputStream getOutputStream() {
		return StreamUtils.nullOutputStream();
	}

	@Override
	public InputStream getInputStream() {
		return output;
	}

	@Override
	public InputStream getErrorStream() {
		return StreamUtils.nullInputStream();
	}

	@Override
	public int waitFor() throws InterruptedException {
		return resultCode;
	}

	@Override
	public int exitValue() {
		return resultCode;
	}

	@Override
	public void destroy() {
	}

}
