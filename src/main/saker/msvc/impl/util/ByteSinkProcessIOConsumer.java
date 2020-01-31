package saker.msvc.impl.util;

import java.io.IOException;
import java.nio.ByteBuffer;

import saker.build.thirdparty.saker.util.io.ByteArrayRegion;
import saker.build.thirdparty.saker.util.io.ByteSink;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import saker.process.api.ProcessIOConsumer;

public class ByteSinkProcessIOConsumer implements ProcessIOConsumer {
	private ByteSink outputSink;

	private transient UnsyncByteArrayOutputStream buf;

	public ByteSinkProcessIOConsumer(ByteSink stdoutsink) {
		this.outputSink = stdoutsink;
	}

	@Override
	public void handleOutput(ByteBuffer bytes) throws IOException {
		if (bytes.hasArray()) {
			outputSink.write(ByteArrayRegion.wrap(bytes.array(), bytes.arrayOffset(), bytes.limit()));
		} else {
			if (buf == null) {
				buf = new UnsyncByteArrayOutputStream(bytes.limit());
			} else {
				buf.reset();
			}
			buf.write(bytes);
			buf.writeTo(outputSink);
		}
	}
}
