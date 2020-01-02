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
package saker.msvc.proc;

import java.io.Closeable;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.lang.annotation.Native;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.SakerPathFiles;
import saker.build.thirdparty.saker.util.io.IOUtils;

public class NativeProcess implements Closeable {
	private static final int DEFAULT_IO_PROCESSING_DIRECT_BUFFER_SIZE = 1024 * 8;
	public static final boolean LOADED;
	static {
		boolean loaded;
		try {
			System.loadLibrary("nativeprocess");
			loaded = true;
		} catch (LinkageError e) {
			//can happen if the classloader was reloaded, but still not garbage collected
			//    or if any other implementation error occurs
			//don't print stacktrace, as we don't need it, but print an error nonetheless
			System.err.println("Failed to load native process library of saker.msvc-proc: " + e);
			loaded = false;
		}
		LOADED = loaded;
	}

	@Native
	public static final int FLAG_MERGE_STDERR = 1 << 0;

	public interface IOProcessor {
		//true to continue
		public boolean standardInputBytesAvailable(ByteBuffer inbuffer);
	}

	protected long nativePtr;
	protected long interruptEventPtr;
	protected final int flags;
	protected final InterrupterSelector interruptor = new InterrupterSelector();

	private final Object interruptSync = new Object();

	public NativeProcess(long nativePtr, long interruptEventPtr, int flags) {
		this.nativePtr = nativePtr;
		this.interruptEventPtr = interruptEventPtr;
		this.flags = flags;
	}

	private static boolean rewindNotifyStandardInput(ByteBuffer buffer, int length, IOProcessor processor) {
		buffer.rewind();
		buffer.limit(length);
		return processor.standardInputBytesAvailable(buffer);
	}

	public static NativeProcess startProcess(SakerPath exe, String[] commands, SakerPath workingdirectory, int flags)
			throws IOException, IllegalArgumentException {
		SakerPathFiles.requireAbsolutePath(exe);
		if (workingdirectory != null) {
			SakerPathFiles.requireAbsolutePath(workingdirectory);
		}
		if (((flags & FLAG_MERGE_STDERR) == FLAG_MERGE_STDERR)) {
		} else {
			throw new UnsupportedOperationException("Non-merged std err is not yet supported.");
		}
		long event = native_createInterruptEvent();
		if (event == 0) {
			throw new IOException("Failed to create process interruption event.");
		}
		long nativeproc;
		try {
			nativeproc = native_startProcess(getPathForNative(exe), commands,
					getPathForNative(workingdirectory == null ? exe.getParent() : workingdirectory), flags,
					UUID.randomUUID().toString(), event);
		} catch (Throwable e) {
			try {
				native_closeInterruptEvent(event);
			} catch (Throwable e2) {
				e.addSuppressed(e2);
			}
			throw e;
		}
		return new NativeProcess(nativeproc, event, flags);
	}

	public void processIO(IOProcessor processor) throws IOException, InterruptedIOException {
		ByteBuffer errbuffer;
		if (((flags & FLAG_MERGE_STDERR) == FLAG_MERGE_STDERR)) {
			errbuffer = null;
		} else {
			errbuffer = ByteBuffer.allocateDirect(DEFAULT_IO_PROCESSING_DIRECT_BUFFER_SIZE)
					.order(ByteOrder.nativeOrder());
		}
		ByteBuffer stdbuffer = ByteBuffer.allocateDirect(DEFAULT_IO_PROCESSING_DIRECT_BUFFER_SIZE)
				.order(ByteOrder.nativeOrder());

		try {
			interruptor.start();
			if (Thread.currentThread().isInterrupted()) {
				throw new InterruptedIOException();
			}
			synchronized (this) {
				if (nativePtr == 0) {
					throw new IllegalStateException("closed.");
				}
				try {
					native_processIO(nativePtr, processor, stdbuffer, errbuffer);
				} catch (InterruptedException e) {
					//reinterrupt the thread, as we don't directly throw the interrupted exception
					Thread.currentThread().interrupt();
					throw new InterruptedIOException(e.getMessage());
				}
			}
		} finally {
			interruptor.finish();
		}
	}

	public int exitValue() throws IllegalThreadStateException, IOException {
		synchronized (this) {
			if (nativePtr == 0) {
				throw new IllegalStateException("closed.");
			}
			return native_getExitCode(nativePtr);
		}
	}

	public int waitFor() throws InterruptedException, IOException {
		return waitForNativeImpl(-1);
	}

	public int waitFor(long timeout, TimeUnit unit) throws InterruptedException, IOException {
		long millis = unit.toMillis(timeout);
		return waitForNativeImpl(millis);
	}

	@Override
	public void close() throws IOException {
		Throwable t = null;
		try {
			try {
				closeNativeCore();
			} catch (Throwable e) {
				t = e;
				throw e;
			}
		} finally {
			try {
				closeNativeEvent();
			} catch (Throwable e) {
				IOUtils.addExc(e, t);
				throw e;
			}
		}
	}

	private void closeNativeEvent() throws IOException {
		synchronized (interruptSync) {
			long ptr = interruptEventPtr;
			if (ptr == 0) {
				return;
			}
			interruptEventPtr = 0;
			native_closeInterruptEvent(ptr);
		}
	}

	private void closeNativeCore() throws IOException {
		synchronized (this) {
			long ptr = nativePtr;
			if (ptr == 0) {
				return;
			}
			nativePtr = 0;
			native_close(ptr);
			//the following doesn't really do anything
			interruptor.close();
		}
	}

	private void interruptNative() {
		synchronized (interruptSync) {
			if (interruptEventPtr == 0) {
				return;
			}
			native_interrupt(interruptEventPtr);
		}
	}

	private int waitForNativeImpl(long millis) throws IOException, InterruptedException {
		try {
			interruptor.start();
			if (Thread.interrupted()) {
				throw new InterruptedException();
			}
			synchronized (this) {
				if (nativePtr == 0) {
					throw new IllegalStateException("closed.");
				}
				return native_waitFor(nativePtr, millis);
			}
		} finally {
			interruptor.finish();
		}
	}

	private static String getPathForNative(SakerPath path) {
		String pathstr = path.toString();
		//MAX_PATH is 260
		if (pathstr.length() > 230) {
			String slashreplaced = pathstr.replace('/', '\\');
			//UNC is based on WindowsPath.addPrefixIfNeeded implementation
			if (slashreplaced.startsWith("\\\\")) {
				return "\\\\?\\UNC" + slashreplaced.substring(1);
			}
			return "\\\\?\\" + slashreplaced;
		}
		return pathstr;
	}

	private static native long native_startProcess(String exe, String[] commands, String workingdirectory, int flags,
			String pipeid, long interrupteventptr) throws IOException;

	private static native long native_createInterruptEvent();

	private static native int native_waitFor(long nativeptr, long timeoutmillis)
			throws InterruptedException, IOException;

	private static native void native_processIO(long nativeptr, IOProcessor processor, ByteBuffer bytedirectbuffer,
			ByteBuffer errbytedirectbuffer) throws IOException, InterruptedException;

	private static native int native_getExitCode(long nativeptr) throws IOException, IllegalThreadStateException;

	private static native void native_close(long nativeptr) throws IOException;

	private static native void native_closeInterruptEvent(long interrupteventptr) throws IOException;

	private static native void native_interrupt(long interrupteventptr);

	//based on https://github.com/NWilson/javaInterruptHook
	private class InterrupterSelector extends AbstractSelector {
		protected InterrupterSelector() {
			super(null);
		}

		public void start() {
			begin();
		}

		public void finish() {
			end();
		}

		@Override
		protected void implCloseSelector() throws IOException {
		}

		@Override
		protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<SelectionKey> keys() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Set<SelectionKey> selectedKeys() {
			throw new UnsupportedOperationException();
		}

		@Override
		public int selectNow() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int select(long timeout) throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public int select() throws IOException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Selector wakeup() {
			interruptNative();
			return this;
		}
	}
}
