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

import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.ccompile.option.IncludeDirectoryOption;
import saker.msvc.impl.clink.option.LibraryPathOption;
import saker.msvc.impl.sdk.WindowsKitsSDKReference;
import saker.msvc.impl.sdk.option.CommonSDKPathReferenceOption;

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
				MSVCUtils.DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION, MSVCUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(
				ImmutableUtils.makeImmutableNavigableSet(new String[] { "/SUBSYSTEM:CONSOLE" }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableLinkedHashSet(new IncludeDirectoryOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_INCLUDE),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UM),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.INCLUDE_SHARED), }));

		x86.setPresetIdentifier("console" + "-x86");
		x64.setPresetIdentifier("console" + "-x64");
		basepreset.setPresetIdentifier("console");

		CONSOLE_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	private static final Collection<SimplePresetCOptions> DEBUG_PRESETS;
	static {
		SimplePresetCOptions debugpreset = new SimplePresetCOptions();
		debugpreset.setPresetIdentifier("debug");
		debugpreset.setLinkSimpleParameters(ImmutableUtils.makeImmutableNavigableSet(new String[] { "/DEBUG" }));
		DEBUG_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { debugpreset });
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
				MSVCUtils.DEFAULT_WINDOWS_KITS_SDK_DESCRIPTION, MSVCUtils.getSDKNameComparator()));
		basepreset.setLinkSimpleParameters(
				ImmutableUtils.makeImmutableNavigableSet(new String[] { "/DLL", "/SUBSYSTEM:WINDOWS" }));
		basepreset.setIncludeDirectories(ImmutableUtils.makeImmutableLinkedHashSet(new IncludeDirectoryOption[] {
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_MSVC, MSVCUtils.MSVC_IDENTIFIER_INCLUDE),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UCRT),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS, WindowsKitsSDKReference.INCLUDE_UM),
				new CommonSDKPathReferenceOption(MSVCUtils.SDK_NAME_WINDOWS_KITS,
						WindowsKitsSDKReference.INCLUDE_SHARED), }));

		x86.setPresetIdentifier("dll" + "-x86");
		x64.setPresetIdentifier("dll" + "-x64");
		basepreset.setPresetIdentifier("dll");

		DLL_PRESETS = ImmutableUtils.makeImmutableHashSet(new SimplePresetCOptions[] { basepreset, x64, x86 });
	}

	public static Collection<? extends SimplePresetCOptions> getConsolePresets() {
		return CONSOLE_PRESETS;
	}

	public static Collection<? extends SimplePresetCOptions> getDLLPresets() {
		return DLL_PRESETS;
	}

	//XXX disabled until proper implementation
//	public static Collection<? extends SimplePresetCOptions> getDebugPresets() {
//		return DEBUG_PRESETS;
//	}

	private COptionPresets() {
		throw new UnsupportedOperationException();
	}
}
