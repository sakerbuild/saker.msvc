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
