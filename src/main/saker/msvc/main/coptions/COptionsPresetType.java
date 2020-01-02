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
						info = @NestInformation("Preset type for console based applications.")),
				@NestFieldInformation(value = COptionsPresetType.DLL,
						info = @NestInformation("Preset type for DLL (Dynamic Link Library) creation.")),

		})
public class COptionsPresetType {
	public static final String CONSOLE = "console";
	public static final String DLL = "dll";
}
