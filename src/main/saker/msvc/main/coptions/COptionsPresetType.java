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
