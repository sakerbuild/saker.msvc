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
package saker.msvc.main.coptions;

import saker.build.scripting.model.info.TypeInformationKind;
import saker.nest.scriptinfo.reflection.annot.NestFieldInformation;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeInformation;

@NestInformation("Enumeration containing possible C option preset types.\n" + "The values can be used with "
		+ COptionsPresetTaskFactory.TASK_NAME + "() for specifying the preset type. "
		+ "The preset types are interpreted in a case-insensitive manner.")
@NestTypeInformation(kind = TypeInformationKind.ENUM,
		enumValues = {

				@NestFieldInformation(value = COptionsPresetType.CONSOLE,
						info = @NestInformation("Preset type for console based applications.\n"
								+ "The preset includes the /SUBSYSTEM:CONSOLE simple linker parameter, and adds "
								+ "appropriate include directories and library paths to access the Windows SDK.\n"
								+ "The _CONSOLE preprocessor definition is added.")),
				@NestFieldInformation(value = COptionsPresetType.WIN32,
				info = @NestInformation("Preset type for Windows based applications.\n"
						+ "The preset includes the /SUBSYSTEM:WINDOWS simple linker parameter, and adds "
						+ "appropriate include directories and library paths to access the Windows SDK.\n"
						+ "The _WINDOWS preprocessor definition is added.")),
				@NestFieldInformation(value = COptionsPresetType.DLL,
						info = @NestInformation("Preset type for DLL (Dynamic Link Library) creation.\n"
								+ "The preset includes the /DLL /SUBSYSTEM:WINDOWS simple linker parameters and adds "
								+ "appropriate include directories and library paths to access the Windows SDK.\n"
								+ "The _WINDOWS, _WINDLL prepreocessor definitions are added.")),
				@NestFieldInformation(value = COptionsPresetType.UAP,
						info = @NestInformation("Preset type for developing against the Universal Application Platform (also known as UWP, Universal Windows Platofmr).\n")),
				@NestFieldInformation(value = COptionsPresetType.OPTIMIZE_RELEASE,
						info = @NestInformation("Preset that includes command line arguments for release optimization.\n"
								+ "The /GL /Gy /O2 /Oi simple parameters are added for compilation. Also defines the "
								+ "NDEBUG macro without any value (/DNDEBUG).\n"
								+ "The /LTCG /OPT:REF /OPT:ICF simple parameters are added for linking.")),
				@NestFieldInformation(value = COptionsPresetType.OPTIMIZE_DEBUG,
						info = @NestInformation("Preset that includes command line arguments for debug optimization.\n"
								+ "The /Od simple parameters are added for compilation. Also defines the "
								+ "_DEBUG macro without any value (/D_DEBUG).\n"
								+ "The preset doesn't include any linker parameters.")),

		})
public class COptionsPresetType {
	public static final String CONSOLE = "console";
	public static final String WIN32 = "win32";
	public static final String DLL = "dll";
	public static final String UAP = "uap";
	public static final String OPTIMIZE_RELEASE = "optimize-release";
	public static final String OPTIMIZE_DEBUG = "optimize-debug";
}
