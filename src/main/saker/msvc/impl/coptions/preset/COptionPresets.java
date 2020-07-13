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

import java.util.Collection;
import java.util.NavigableMap;
import java.util.TreeMap;

import saker.build.file.path.SakerPath;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.option.CompilationPathOption;
import saker.msvc.impl.option.SimpleParameterOption;
import saker.msvc.impl.sdk.AbstractVCToolsSDKReference;
import saker.msvc.impl.util.option.SDKPathReferenceCompilationPathOption;
import saker.msvc.main.coptions.COptionsPresetType;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.api.SDKPathReference;
import saker.sdk.support.api.SDKSupportUtils;
import saker.windows.api.SakerWindowsUtils;

public class COptionPresets {
	private static final SakerPath PATH_WINDOWSAPP_LIB = SakerPath.valueOf("WindowsApp.lib");
	private static final Collection<SimplePresetCOptions> CONSOLE_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X64),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UM), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X86),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UM), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setSDKs(ImmutableUtils.singletonNavigableMap(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
				SakerWindowsUtils.getDefaultWindowsKitsSDK(), SDKSupportUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableList(
				new SimpleParameterOption[] { SimpleParameterOption.create("/SUBSYSTEM:CONSOLE"), }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.INCLUDE),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UM),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_SHARED), }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_CONSOLE", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));

		x86.setPresetIdentifier(COptionsPresetType.CONSOLE + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.CONSOLE + "-x64");
		basepreset.setPresetIdentifier(COptionsPresetType.CONSOLE);

		CONSOLE_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	private static final Collection<SimplePresetCOptions> WIN32_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X64),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UM), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X86),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UM), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setSDKs(ImmutableUtils.singletonNavigableMap(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
				SakerWindowsUtils.getDefaultWindowsKitsSDK(), SDKSupportUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableList(
				new SimpleParameterOption[] { SimpleParameterOption.create("/SUBSYSTEM:WINDOWS"), }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.INCLUDE),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UM),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_SHARED), }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_WINDOWS", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));

		x86.setPresetIdentifier(COptionsPresetType.WIN32 + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.WIN32 + "-x64");
		basepreset.setPresetIdentifier(COptionsPresetType.WIN32);

		WIN32_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	private static final Collection<SimplePresetCOptions> DLL_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X64),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UM), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X86),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UM), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setSDKs(ImmutableUtils.singletonNavigableMap(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
				SakerWindowsUtils.getDefaultWindowsKitsSDK(), SDKSupportUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableList(new SimpleParameterOption[] {
				SimpleParameterOption.create("/DLL"), SimpleParameterOption.create("/SUBSYSTEM:WINDOWS"), }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.INCLUDE),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UM),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_SHARED), }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_WINDOWS", "");
		macrodefs.put("_WINDLL", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));

		x86.setPresetIdentifier(COptionsPresetType.DLL + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.DLL + "-x64");
		basepreset.setPresetIdentifier(COptionsPresetType.DLL);

		DLL_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	private static final Collection<SimplePresetCOptions> UAP_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.LIB_STORE_X64),
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X64),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UM), }));
		x64.setLinkerInput(ImmutableUtils
				.makeImmutableList(new CompilationPathOption[] { new SDKPathReferenceCompilationPathOption(
						SDKPathReference.create(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
								SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X64_UM, PATH_WINDOWSAPP_LIB)), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.LIB_STORE_X86),
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_X86),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UM), }));
		x86.setLinkerInput(ImmutableUtils
				.makeImmutableList(new CompilationPathOption[] { new SDKPathReferenceCompilationPathOption(
						SDKPathReference.create(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
								SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_X86_UM, PATH_WINDOWSAPP_LIB)), }));

		SimplePresetCOptions arm = new SimplePresetCOptions();
		arm.setArchitecture("ARM");
		arm.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.LIB_STORE_ARM),
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.LIB_ARM),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM_UM), }));
		arm.setLinkerInput(ImmutableUtils
				.makeImmutableList(new CompilationPathOption[] { new SDKPathReferenceCompilationPathOption(
						SDKPathReference.create(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
								SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM_UM, PATH_WINDOWSAPP_LIB)), }));

		SimplePresetCOptions arm64 = new SimplePresetCOptions();
		arm64.setArchitecture("ARM64");
		arm64.setLibraryPaths(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.LIB_STORE_ARM64),
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.LIB_ARM64),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM64_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM64_UM), }));
		arm64.setLinkerInput(ImmutableUtils
				.makeImmutableList(new CompilationPathOption[] { new SDKPathReferenceCompilationPathOption(
						SDKPathReference.create(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
								SakerWindowsUtils.SDK_WINDOWSKITS_PATH_LIB_ARM64_UM, PATH_WINDOWSAPP_LIB)), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		TreeMap<String, SDKDescription> sdks = new TreeMap<>(SDKSupportUtils.getSDKNameComparator());
		sdks.put(SakerWindowsUtils.SDK_NAME_WINDOWSUAP, SakerWindowsUtils.getDefaultWindowsUapSDK());
		sdks.put(SakerWindowsUtils.SDK_NAME_WINDOWSKITS, SakerWindowsUtils.getDefaultWindowsKitsSDK());
		basepreset.setSDKs(ImmutableUtils.makeImmutableNavigableMap(sdks));

		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC, AbstractVCToolsSDKReference.INCLUDE),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UCRT),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_UM),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_SHARED),
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_WINRT), }));

		SimplePresetCOptions cpppreset = new SimplePresetCOptions();
		cpppreset.setLanguage("C++");
		cpppreset.setIncludeDirectories(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(SakerWindowsUtils.SDK_NAME_WINDOWSKITS,
						SakerWindowsUtils.SDK_WINDOWSKITS_PATH_INCLUDE_CPPWINRT), }));
		cpppreset.setForceUsing(ImmutableUtils.makeImmutableList(new CompilationPathOption[] {
				new SDKPathReferenceCompilationPathOption(MSVCUtils.SDK_NAME_MSVC,
						AbstractVCToolsSDKReference.STORE_REFERENCES_PLATFORM_WINMD),
				new SDKPathReferenceCompilationPathOption(
						SakerWindowsUtils.getWindowsUapApiContractsWinmdSDKPathCollectionReference()) }));
		cpppreset.setCompileSimpleParameters(
				ImmutableUtils.makeImmutableList(new SimpleParameterOption[] { SimpleParameterOption.create("/ZW"),
						SimpleParameterOption.create("/EHsc"), SimpleParameterOption.create("/ZW:nostdlib") }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_WIN32_WINNT", "0x0A00");
		macrodefs.put("WINAPI_FAMILY", "WINAPI_FAMILY_APP");
		macrodefs.put("__WRL_NO_DEFAULT_LIB__", "");
		macrodefs.put("_UNICODE", "");
		macrodefs.put("UNICODE", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));

		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableList(new SimpleParameterOption[] {
				SimpleParameterOption.create("/SUBSYSTEM:WINDOWS"), SimpleParameterOption.create("/APPCONTAINER"),
				SimpleParameterOption.create("/DYNAMICBASE"), SimpleParameterOption.create("/NXCOMPAT"), }));
		basepreset.setGenerateWinmd(true);

		x86.setPresetIdentifier(COptionsPresetType.UAP + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.UAP + "-x64");
		arm.setPresetIdentifier(COptionsPresetType.UAP + "-ARM");
		arm64.setPresetIdentifier(COptionsPresetType.UAP + "-ARM64");
		basepreset.setPresetIdentifier(COptionsPresetType.UAP);
		cpppreset.setPresetIdentifier(COptionsPresetType.UAP + "-cxx");

		UAP_PRESETS = ImmutableUtils
				.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, cpppreset, x64, x86 });
	}

	//based on Visual Studio default command line for console apps
	private static final Collection<SimplePresetCOptions> OPTIMIZE_DEBUG_PRESETS;
	static {
		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setPresetIdentifier(COptionsPresetType.OPTIMIZE_DEBUG);
		basepreset.setCompileSimpleParameters(ImmutableUtils.makeImmutableList(new SimpleParameterOption[] {
				// /Od (Disable (Debug)) : Turns off all optimizations in the program and speeds compilation.
				SimpleParameterOption.create("/Od"),

		}));
		NavigableMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_DEBUG", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));
		OPTIMIZE_DEBUG_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset });
	}

	//based on Visual Studio default command line for console apps
	private static final Collection<SimplePresetCOptions> OPTIMIZE_RELEASE_PRESETS;
	static {
		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setPresetIdentifier(COptionsPresetType.OPTIMIZE_RELEASE);
		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableList(new SimpleParameterOption[] {
				// /LTCG (Link-time Code Generation)
				SimpleParameterOption.create("/LTCG"),
				// /OPT:REF eliminates functions and data that are never referenced
				SimpleParameterOption.create("/OPT:REF"),
				// /OPT:ICF Use ICF[=iterations] to perform identical COMDAT folding.
				SimpleParameterOption.create("/OPT:ICF"),

		}));
		basepreset.setCompileSimpleParameters(ImmutableUtils.makeImmutableList(new SimpleParameterOption[] {
				// /GL (Whole Program Optimization)		
				SimpleParameterOption.create("/GL"),
				// /Gy (Enable Function-Level Linking)
				SimpleParameterOption.create("/Gy"),
				// /O1, /O2 (Minimize Size, Maximize Speed)
				SimpleParameterOption.create("/O2"), // equivalent to: /Og /Oi /Ot /Oy /Ob2 /GF /Gy
				// /Oi (Generate Intrinsic Functions)
				SimpleParameterOption.create("/Oi"),

		}));
		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("NDEBUG", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));
		OPTIMIZE_RELEASE_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset });
	}

	public static Collection<? extends SimplePresetCOptions> getConsolePresets() {
		return CONSOLE_PRESETS;
	}

	public static Collection<SimplePresetCOptions> getWin32Presets() {
		return WIN32_PRESETS;
	}

	public static Collection<? extends SimplePresetCOptions> getDLLPresets() {
		return DLL_PRESETS;
	}

	public static Collection<? extends SimplePresetCOptions> getOptimizeRelease() {
		return OPTIMIZE_RELEASE_PRESETS;
	}

	public static Collection<? extends SimplePresetCOptions> getOptimizeDebug() {
		return OPTIMIZE_DEBUG_PRESETS;
	}

	public static Collection<? extends SimplePresetCOptions> getUap() {
		return UAP_PRESETS;
	}

	private COptionPresets() {
		throw new UnsupportedOperationException();
	}
}
