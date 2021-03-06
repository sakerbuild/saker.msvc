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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.thirdparty.saker.util.StringUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.msvc.MSVCTestMetric.MetricProcessIOConsumer;

public class LinkMockProcess {
	private LinkMockProcess() {
		throw new UnsupportedOperationException();
	}

	public static int run(List<String> commands, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		try {
			try (UnsyncByteArrayOutputStream baos = new UnsyncByteArrayOutputStream()) {
				int resultCode;
				try (PrintStream out = new PrintStream(baos)) {
					SakerPath exepath = SakerPath.valueOf(commands.get(0));

					String targetarch = MockingMSVCTestMetric.getMSVCExeTargetArchitecture(exepath);
					String version = MockingMSVCTestMetric.getMSVCExeVersion(exepath);
					if (!Objects.equals(targetarch, CLMockProcess.requireCommand(commands, "/MACHINE:"))) {
						throw new IllegalArgumentException("Architecture mismatch." + targetarch + " - "
								+ CLMockProcess.requireCommand(commands, "/MACHINE:"));
					}

					SakerPath outpath = SakerPath.valueOf(CLMockProcess.requireCommand(commands, "/OUT:"));

					resultCode = executeLinking(targetarch, out, commands, outpath, version);
				}
				if (stdoutconsumer != null) {
					stdoutconsumer.handleOutput(ByteBuffer.wrap(baos.getBuffer(), 0, baos.size()));
				}
				return resultCode;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static int executeLinking(String targetarch, PrintStream stdout, List<String> commands,
			SakerPath outputpath, String version) {
		if (!containsIgnoreCase(commands, "/nologo")) {
			stdout.println("Mock compiler: " + version + " for " + targetarch);
		}

		Collection<Path> inputfiles = getInputFilePaths(commands);
		List<SakerPath> libpathdirs = getLibPathDirectoriesFromCommands(commands);
		Set<SakerPath> includedlibs = new TreeSet<>();

		SakerPath winmdpath = null;
		if (isGenerateWinmd(commands)) {
			winmdpath = getWinmdPath(commands);
			Objects.requireNonNull(winmdpath, "winmdpath");
		}

		UnsyncByteArrayOutputStream fileoutbuf = new UnsyncByteArrayOutputStream();
		UnsyncByteArrayOutputStream winmdoutbuf = new UnsyncByteArrayOutputStream();
		try (PrintStream outps = new PrintStream(fileoutbuf)) {
			if (containsIgnoreCase(commands, "/dll")) {
				outps.println(MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_DLL, targetarch));
			} else {
				outps.println(MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_EXE, targetarch));
			}
			outps.println("#version " + version);
			for (Path inputfile : inputfiles) {
				winmdoutbuf.write(
						(StringUtils.toHexString(LocalFileProvider.getInstance().hash(inputfile, "MD5").getHash())
								+ "\n").getBytes(StandardCharsets.UTF_8));

				try (BufferedReader reader = Files.newBufferedReader(inputfile)) {
					String firstline = reader.readLine();
					String expectedtype = MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_OBJ,
							targetarch);
					if (!expectedtype.equals(firstline)) {
						throw new IllegalArgumentException(
								"Input file not " + expectedtype + ": " + inputfile + " -> " + firstline);
					}
					handleLinkFileLines(targetarch, libpathdirs, includedlibs, outps, reader);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -99;
		}
		try {
			Files.write(LocalFileProvider.toRealPath(outputpath), fileoutbuf.toByteArray());
			if (winmdpath != null) {
				Files.write(LocalFileProvider.toRealPath(winmdpath), winmdoutbuf.toByteArray());
			}
		} catch (IOException e) {
			e.printStackTrace();
			return -99;
		}
		return 0;
	}

	private static void handleLinkFileLines(String targetarch, List<SakerPath> libpathdirs, Set<SakerPath> includedlibs,
			PrintStream outps, BufferedReader reader) throws IOException {
		for (String line; (line = reader.readLine()) != null;) {
			if (line.startsWith("#lib ")) {
				SakerPath libpath = SakerPath.valueOf(line.substring(5));
				handleLib(libpathdirs, includedlibs, libpath, targetarch, outps);
				continue;
			}
			if (line.startsWith("#version ")) {
				//skip
				continue;
			}
			int lineval = Integer.parseInt(line);
			outps.println(lineval);
		}
	}

	private static void handleLib(List<SakerPath> libpathdirs, Set<SakerPath> includedlibs, SakerPath libpath,
			String targetarch, PrintStream outps) {
		List<Exception> causes = new ArrayList<>();
		for (SakerPath libpathdir : libpathdirs) {
			SakerPath libabssakerpath = libpathdir.resolve(libpath);
			if (includedlibs.contains(libabssakerpath)) {
				//already included
				break;
			}
			Path libabspath = LocalFileProvider.toRealPath(libabssakerpath);
			try (BufferedReader reader = Files.newBufferedReader(libabspath)) {
				includedlibs.add(libabssakerpath);
				String firstline = reader.readLine();
				String expectedtype = MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_LIB,
						targetarch);
				if (!expectedtype.equals(firstline)) {
					throw new IllegalArgumentException(
							"Lib input file not " + expectedtype + ": " + libabssakerpath + " -> " + firstline);
				}
				handleLinkFileLines(targetarch, libpathdirs, includedlibs, outps, reader);
				return;
			} catch (IOException e) {
				causes.add(e);
				continue;
			}
		}
		IllegalArgumentException exc = new IllegalArgumentException("Lib not found: " + libpath);
		for (Exception c : causes) {
			exc.addSuppressed(c);
		}
		throw exc;
	}

	private static Collection<Path> getInputFilePaths(List<String> commands) {
		Set<Path> result = new LinkedHashSet<>();
		Iterator<String> it = commands.iterator();
		//skip start command
		it.next();

		while (it.hasNext()) {
			String cmd = it.next();
			if (cmd.equalsIgnoreCase("/nologo") || cmd.startsWith("/LIBPATH:") || cmd.startsWith("/SUBSYSTEM:")
					|| cmd.equalsIgnoreCase("/DLL") || cmd.startsWith("/OUT:") || cmd.startsWith("/MACHINE:")
					|| cmd.equalsIgnoreCase("/DEBUG") || cmd.equalsIgnoreCase("/INCREMENTAL")
					|| cmd.equalsIgnoreCase("/INCREMENTAL:NO")) {
				continue;
			}
			if (StringUtils.startsWithIgnoreCase(cmd, "/WINMD")) {
				continue;
			}
			result.add(LocalFileProvider.toRealPath(SakerPath.valueOf(cmd)));
		}
		return result;
	}

	private static List<SakerPath> getLibPathDirectoriesFromCommands(List<String> commands) {
		List<SakerPath> result = new ArrayList<>();
		for (String cmd : commands) {
			if (cmd.startsWith("/LIBPATH:")) {
				result.add(SakerPath.valueOf(cmd.substring(9)));
			}
		}
		return result;
	}

	private static boolean isGenerateWinmd(List<String> commands) {
		for (String cmd : commands) {
			if ("/WINMD".equalsIgnoreCase(cmd)) {
				return true;
			}
		}
		return false;
	}

	private static SakerPath getWinmdPath(List<String> commands) {
		for (String cmd : commands) {
			if (!StringUtils.startsWithIgnoreCase(cmd, "/WINMDFILE:")) {
				continue;
			}
			return SakerPath.valueOf(cmd.substring(11));
		}
		return null;
	}

	public static boolean containsIgnoreCase(List<String> commands, String cmd) {
		for (String c : commands) {
			if (cmd.equalsIgnoreCase(c)) {
				return true;
			}
		}
		return false;
	}
}
