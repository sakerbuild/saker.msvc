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
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import saker.build.file.path.SakerPath;
import saker.build.file.provider.LocalFileProvider;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.build.thirdparty.saker.util.io.UnsyncByteArrayOutputStream;
import testing.saker.SakerTestCase;
import testing.saker.msvc.MSVCTestMetric.MetricProcessIOConsumer;

public class CLMockProcess {
	private CLMockProcess() {
		throw new UnsupportedOperationException();
	}

	public static int run(List<String> commands, boolean mergestderr, MetricProcessIOConsumer stdoutconsumer,
			MetricProcessIOConsumer stderrconsumer) throws IOException {
		try {
			try (UnsyncByteArrayOutputStream stdoutbaos = new UnsyncByteArrayOutputStream();
					UnsyncByteArrayOutputStream stderrbaos = new UnsyncByteArrayOutputStream()) {
				int resultCode;
				try (PrintStream stdout = new PrintStream(stdoutbaos);
						PrintStream stderr = mergestderr ? stdout : new PrintStream(stderrbaos)) {
					//cl should compile only
					SakerTestCase.assertTrue(commands.contains("/c"));

					//should ignore environment variables
					SakerTestCase.assertTrue(commands.contains("/X"));

					SakerPath exepath = SakerPath.valueOf(commands.get(0));

					String targetarch = MockingMSVCTestMetric.getMSVCExeTargetArchitecture(exepath);
					String version = MockingMSVCTestMetric.getMSVCExeVersion(exepath);

					SakerPath outputpath = SakerPath.valueOf(requireCommand(commands, "/Fo"));
					String inputcmd = getInputFileCommand(commands);
					SakerPath inputpath = SakerPath.valueOf(inputcmd.substring(3));

					resultCode = executeCompilation(inputpath, outputpath, stdout, stderr, commands, targetarch,
							getInputCommandLanguage(inputcmd), version);
				}
				if (stdoutconsumer != null) {
					stdoutconsumer.handleOutput(ByteBuffer.wrap(stdoutbaos.getBuffer(), 0, stdoutbaos.size()));
				}
				if (!mergestderr && stderrconsumer != null) {
					stderrconsumer.handleOutput(ByteBuffer.wrap(stderrbaos.getBuffer(), 0, stderrbaos.size()));
				}
				return resultCode;
			}
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		return -1;
	}

	private static String getInputCommandLanguage(String inputcmd) {
		if (inputcmd.startsWith("/Tc")) {
			return "c";
		}
		if (inputcmd.startsWith("/Tp")) {
			return "c++";
		}
		throw new IllegalArgumentException(inputcmd);
	}

	private static String nextLine(BufferedReader reader, Deque<String> pendinglines) throws IOException {
		if (pendinglines.isEmpty()) {
			return reader.readLine();
		}
		return pendinglines.pollFirst();
	}

	private static String getDefineValue(List<String> commands, String word) {
		String prefix = "/D" + word;
		for (String cmd : commands) {
			if (!cmd.startsWith(prefix)) {
				continue;
			}
			String val = cmd.substring(prefix.length());
			if (val.isEmpty()) {
				return "";
			}
			if (!val.startsWith("=")) {
				throw new IllegalArgumentException("Command: " + cmd);
			}
			return val.substring(1);
		}
		return null;
	}

	private static int executeCompilation(SakerPath inputpath, SakerPath outputpath, PrintStream stdout,
			PrintStream stderr, List<String> commands, String targetarch, String language, String version) {
		if (!commands.contains("/nologo")) {
			stdout.println("Mock compiler: " + version + " " + language + " for " + targetarch);
		}
		List<SakerPath> includedirs = getIncludeDirectoriesFromCommands(commands);
		Deque<String> pendinglines = new ArrayDeque<>();
		Set<SakerPath> includedpaths = new TreeSet<>();
		Set<SakerPath> referencedlibs = new TreeSet<>();

		UnsyncByteArrayOutputStream fileoutbuf = new UnsyncByteArrayOutputStream();
		try (BufferedReader reader = Files.newBufferedReader(LocalFileProvider.toRealPath(inputpath));
				PrintStream outps = new PrintStream(fileoutbuf)) {
			outps.println(MockingMSVCTestMetric.createFileTypeLine(MockingMSVCTestMetric.TYPE_OBJ, targetarch));
			outps.println("#version " + version);
			if (commands.contains("/MT")) {
				if (ObjectUtils.containsAny(commands,
						ImmutableUtils.asUnmodifiableArrayList("/MTd", "/MD", "/MDd", "/LD", "/LDd"))) {
					throw new IllegalArgumentException("Conflicting runtime library options: " + commands);
				}
				outps.println("#lib LIBCMT.lib");
			} else if (commands.contains("/MTd")) {
				if (ObjectUtils.containsAny(commands,
						ImmutableUtils.asUnmodifiableArrayList("/MT", "/MD", "/MDd", "/LD", "/LDd"))) {
					throw new IllegalArgumentException("Conflicting runtime library options: " + commands);
				}
				outps.println("#lib LIBCMTD.lib");
			} else if (commands.contains("/MD")) {
				if (ObjectUtils.containsAny(commands,
						ImmutableUtils.asUnmodifiableArrayList("/MTd", "/MT", "/MDd", "/LD", "/LDd"))) {
					throw new IllegalArgumentException("Conflicting runtime library options: " + commands);
				}
				outps.println("#lib MSVCRT.lib");
			} else if (commands.contains("/MDd")) {
				if (ObjectUtils.containsAny(commands,
						ImmutableUtils.asUnmodifiableArrayList("/MTd", "/MT", "/MD", "/LD", "/LDd"))) {
					throw new IllegalArgumentException("Conflicting runtime library options: " + commands);
				}
				outps.println("#lib MSVCRTD.lib");
			} else if (ObjectUtils.containsAny(commands, ImmutableUtils.asUnmodifiableArrayList("/LD", "/LDd"))) {
				throw new IllegalArgumentException("Unsupported runtime library options: " + commands);
			}
			for (String line; (line = nextLine(reader, pendinglines)) != null;) {
				if (line.isEmpty()) {
					continue;
				}
				if (line.startsWith("#include ")) {
					String includephrase = line.substring(9).trim();
					if (includephrase.isEmpty()) {
						return -2;
					}
					SakerPath includepath = SakerPath.valueOf(includephrase.substring(1, includephrase.length() - 1));
					if (includephrase.charAt(0) == '<' && includephrase.charAt(includephrase.length() - 1) == '>') {
						includeBracketIncludePath(includedirs, pendinglines, includedpaths, includepath, stdout, stderr,
								commands);
					} else if (includephrase.charAt(0) == '\"'
							&& includephrase.charAt(includephrase.length() - 1) == '\"') {
						throw new UnsupportedOperationException();
					} else {
						return -3;
					}
					continue;
				}
				if (line.startsWith("#lib ")) {
					String libphrase = line.substring(5).trim();
					if (libphrase.isEmpty()) {
						return -4;
					}
					SakerPath libpath = SakerPath.valueOf(libphrase);
					if (referencedlibs.add(libpath)) {
						outps.println(line);
					}
					continue;
				}
				int lineval;
				try {
					lineval = Integer.parseInt(line);
				} catch (NumberFormatException e) {
					String defineval = getDefineValue(commands, line);
					if (defineval == null) {
						throw new IllegalArgumentException("Illegal token: " + line);
					}
					if (defineval.isEmpty()) {
						//skip
						continue;
					}
					lineval = Integer.parseInt(defineval);
				}
				int langmultiplier = getLanguageMockMultipler(language);
				outps.println(lineval * langmultiplier * getArchitectureMultiplier(targetarch));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return -99;
		}
		try {
			Files.write(LocalFileProvider.toRealPath(outputpath), fileoutbuf.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
			return -99;
		}
		return 0;
	}

	public static int getArchitectureMultiplier(String targetarch) {
		return Integer.parseInt(targetarch.substring(1));
	}

	public static int getLanguageMockMultipler(String language) {
		if ("c".equalsIgnoreCase(language)) {
			return MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_C;
		}
		if ("c++".equalsIgnoreCase(language)) {
			return MockingMSVCTestMetric.MOCK_MULTIPLIER_LANGUAGE_CPP;
		}
		throw new IllegalArgumentException("Unknown language: " + language);
	}

	private static void includeBracketIncludePath(List<SakerPath> includedirs, Deque<String> pendinglines,
			Set<SakerPath> includedpaths, SakerPath includepath, PrintStream stdout, PrintStream stderr,
			List<String> commands) {
		List<Exception> causes = new ArrayList<>();
		for (SakerPath includedir : includedirs) {
			SakerPath resolvedincludepath = includedir.resolve(includepath);
			if (includedpaths.contains(resolvedincludepath)) {
				return;
			}
			try {
				Path realpath = LocalFileProvider.toRealPath(resolvedincludepath);
				List<String> alllines = Files.readAllLines(realpath);
				includedpaths.add(resolvedincludepath);
				//XXX indent the real path based on the include stack
				if (commands.contains("/showIncludes")) {
					stderr.println("Note: including file: " + realpath);
				}
				pendinglines.addAll(alllines);
				return;
			} catch (IOException e) {
				causes.add(e);
				continue;
			}
		}
		//some arbitrary error data
		//XXX track if needed
		printErrorMessage(stdout, "c:\\some\\path\\to\\source", "123", "fatal error", "C1083",
				"Cannot open include file: '" + Paths.get(includepath.toString()) + "': No such file or directory");
		IllegalArgumentException exc = new IllegalArgumentException("Included file not found: " + includepath);
		for (Exception e : causes) {
			exc.addSuppressed(e);
		}
		throw exc;
	}

	private static void printErrorMessage(PrintStream stdout, String logpath, String linenum, String severity,
			String errornum, String description) {
		stdout.println(logpath + "(" + linenum + "): " + severity + " " + errornum + ": " + description);
	}

	private static List<SakerPath> getIncludeDirectoriesFromCommands(List<String> commands) {
		List<SakerPath> result = new ArrayList<>();
		for (String cmd : commands) {
			if (cmd.startsWith("/I")) {
				result.add(SakerPath.valueOf(cmd.substring(2)));
			}
		}
		return result;
	}

	private static String getInputFileCommand(List<String> commands) {
		for (String cmd : commands) {
			if (cmd.startsWith("/Tp")) {
				return cmd;
			}
			if (cmd.startsWith("/Tc")) {
				return cmd;
			}
		}
		throw new IllegalArgumentException("No input file.");
	}

	public static String requireCommand(List<String> commands, String prefix) {
		for (String cmd : commands) {
			if (cmd.startsWith(prefix)) {
				return cmd.substring(prefix.length());
			}
		}
		throw new IllegalArgumentException("No command found with prefix: " + prefix);
	}

}
