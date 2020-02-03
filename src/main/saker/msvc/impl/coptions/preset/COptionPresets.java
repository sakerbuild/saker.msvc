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
import java.util.TreeMap;

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.option.IncludePathOption;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.sdk.WindowsKitsSDKReference;
import saker.msvc.impl.sdk.option.CommonSDKPathReferenceOption;
import saker.msvc.main.coptions.COptionsPresetType;
import saker.sdk.support.api.SDKSupportUtils;

public class COptionPresets {
	private static final Collection<SimplePresetCOptions> CONSOLE_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableLinkedHashSet(new LibraryPathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_LIB_X64),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.LIB_X64_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.LIB_X64_UM), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableLinkedHashSet(new LibraryPathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_LIB_X86),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.LIB_X86_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.LIB_X86_UM), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setSdks(ImmutableUtils.singletonNavigableMap(MSVCUtils.SDK_NAME_WINDOWS_KITS,
				MSVCUtils.DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION, SDKSupportUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(
				ImmutableUtils.makeImmutableNavigableSet(new String[] { "/SUBSYSTEM:CONSOLE" }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableLinkedHashSet(new IncludePathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_INCLUDE),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UM),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.INCLUDE_SHARED), }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_CONSOLE", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));
		
		x86.setPresetIdentifier(COptionsPresetType.CONSOLE + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.CONSOLE + "-x64");
		basepreset.setPresetIdentifier(COptionsPresetType.CONSOLE);

		CONSOLE_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	private static final Collection<SimplePresetCOptions> DLL_PRESETS;
	static {
		SimplePresetCOptions x64 = new SimplePresetCOptions();
		x64.setArchitecture("x64");
		x64.setLibraryPaths(ImmutableUtils.makeImmutableLinkedHashSet(new LibraryPathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_LIB_X64),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.LIB_X64_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.LIB_X64_UM), }));

		SimplePresetCOptions x86 = new SimplePresetCOptions();
		x86.setArchitecture("x86");
		x86.setLibraryPaths(ImmutableUtils.makeImmutableLinkedHashSet(new LibraryPathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_LIB_X86),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.LIB_X86_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.LIB_X86_UM), }));

		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setSdks(ImmutableUtils.singletonNavigableMap(MSVCUtils.SDK_NAME_WINDOWS_KITS,
				MSVCUtils.DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION, SDKSupportUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(
				ImmutableUtils.makeImmutableNavigableSet(new String[] { "/DLL", "/SUBSYSTEM:WINDOWS" }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableLinkedHashSet(new IncludePathOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_INCLUDE),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UM),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.INCLUDE_SHARED), }));

		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_WINDOWS", "");
		macrodefs.put("_WINDLL", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));

		x86.setPresetIdentifier(COptionsPresetType.DLL + "-x86");
		x64.setPresetIdentifier(COptionsPresetType.DLL + "-x64");
		basepreset.setPresetIdentifier(COptionsPresetType.DLL);

		DLL_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	//based on Visual Studio default command line for console apps
	private static final Collection<SimplePresetCOptions> OPTIMIZE_DEBUG_PRESETS;
	static {
		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setPresetIdentifier("optimize-debug");
		basepreset.setCompileSimpleParameters(ImmutableUtils.makeImmutableNavigableSet(new String[] {
				// /Od (Disable (Debug)) : Turns off all optimizations in the program and speeds compilation.
				"/Od",

		}));
		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("_DEBUG", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));
		OPTIMIZE_DEBUG_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset });
	}

	//based on Visual Studio default command line for console apps
	private static final Collection<SimplePresetCOptions> OPTIMIZE_RELEASE_PRESETS;
	static {
		SimplePresetCOptions basepreset = new SimplePresetCOptions();
		basepreset.setPresetIdentifier(COptionsPresetType.OPTIMIZE_RELEASE);
		basepreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableNavigableSet(new String[] {
				// /LTCG (Link-time Code Generation)
				"/LTCG",
				// /OPT:REF eliminates functions and data that are never referenced
				"/OPT:REF",
				// /OPT:ICF Use ICF[=iterations] to perform identical COMDAT folding.
				"/OPT:ICF"

		}));
		basepreset.setCompileSimpleParameters(ImmutableUtils.makeImmutableNavigableSet(new String[] {
				// /GL (Whole Program Optimization)		
				"/GL",
				// /Gy (Enable Function-Level Linking)
				"/Gy",
				// /O1, /O2 (Minimize Size, Maximize Speed)
				"/O2", // equivalent to: /Og /Oi /Ot /Oy /Ob2 /GF /Gy
				// /Oi (Generate Intrinsic Functions)
				"/Oi",

		}));
		TreeMap<String, String> macrodefs = new TreeMap<>();
		macrodefs.put("NDEBUG", "");
		basepreset.setMacroDefinitions(ImmutableUtils.unmodifiableNavigableMap(macrodefs));
		OPTIMIZE_RELEASE_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset });
	}

	public static Collection<? extends SimplePresetCOptions> getConsolePresets() {
		return CONSOLE_PRESETS;
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

	private COptionPresets() {
		throw new UnsupportedOperationException();
	}
}
