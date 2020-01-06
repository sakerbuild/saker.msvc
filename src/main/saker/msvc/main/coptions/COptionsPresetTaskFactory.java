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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.thirdparty.saker.util.ObjectUtils;
import saker.compiler.utils.api.CompilationIdentifier;
import saker.compiler.utils.main.CompilationIdentifierTaskOption;
import saker.msvc.impl.coptions.preset.COptionPresets;
import saker.msvc.impl.coptions.preset.PresetCOptions;
import saker.msvc.impl.coptions.preset.SimpleCOptionsPresetTaskOutput;
import saker.msvc.impl.coptions.preset.SimplePresetCOptions;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.msvc.main.doc.TaskDocs.DocCOptionsPresetTaskOutput;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;

@NestTaskInformation(returnType = @NestTypeUsage(DocCOptionsPresetTaskOutput.class))
@NestInformation("Creates a C options configuration for a specific preset type.\n"
		+ "Presets are pre-made C configurations that configure the compiler and linker tasks to "
		+ "work in a specific way. Presets contain the necessary options to compile and link sources "
		+ "for the specified target.\n" + "The returned presets can be directly passed to the "
		+ MSVCCCompileTaskFactory.TASK_NAME + "() and " + MSVCCLinkTaskFactory.TASK_NAME
		+ "() compiler options parameters.")
@NestParameterInformation(value = "Preset",
		aliases = { "", "Presets" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = { COptionsPresetType.class }),
		required = true,
		info = @NestInformation("Specifies one or more preset names for which the presets should be created.\n"
				+ "The specified presets will be assigned with the Identifier parameter that specifies as a qualifier "
				+ "for options merging."))
@NestParameterInformation(value = "Identifier",
		type = @NestTypeUsage(CompilationIdentifierTaskOption.class),
		info = @NestInformation("Specifies the identifier that should be assigned to the returned presets for options merging.\n"
				+ "If specified, the presets will only be merged to task inputs which appropriately contain the given identifier."))
public class COptionsPresetTaskFactory extends FrontendTaskFactory<Object> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.msvc.coptions.preset";

	@Override
	public ParameterizableTask<? extends Object> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<Object>() {

			@SakerInput(value = { "", "Preset", "Presets" }, required = true)
			public Collection<String> presetNames;

			@SakerInput(value = "Identifier")
			public CompilationIdentifierTaskOption identifierOption;

			@Override
			public Object run(TaskContext taskcontext) throws Exception {
				if (ObjectUtils.isNullOrEmpty(presetNames)) {
					taskcontext.abortExecution(new NullPointerException(
							(presetNames == null ? "null" : "empty") + " preset names argument specified."));
					return null;
				}

				CompilationIdentifierTaskOption identifieropt = ObjectUtils.clone(identifierOption,
						CompilationIdentifierTaskOption::clone);
				CompilationIdentifier optionidentifier = CompilationIdentifierTaskOption.getIdentifier(identifieropt);

				Collection<SimplePresetCOptions> presets = new LinkedHashSet<>();
				for (String presetid : presetNames) {
					switch (presetid.toLowerCase(Locale.ENGLISH)) {
						case COptionsPresetType.CONSOLE: {
							presets.addAll(COptionPresets.getConsolePresets());
							break;
						}
						case COptionsPresetType.DLL: {
							presets.addAll(COptionPresets.getDLLPresets());
							break;
						}
//						case "debug": {
//							presets.addAll(COptionPresets.getDebugPresets());
//							break;
//						}
						default: {
							taskcontext.abortExecution(
									new IllegalArgumentException("Unrecognized C options preset type: " + presetNames));
							return null;
						}
					}
				}

				Collection<? extends PresetCOptions> resultpresets;
				if (optionidentifier != null) {
					Set<PresetCOptions> resultset = new LinkedHashSet<>();
					resultpresets = resultset;
					for (SimplePresetCOptions preset : presets) {
						SimplePresetCOptions cloned = preset.clone();
						cloned.setIdentifier(optionidentifier);
						resultset.add(cloned);
					}
				} else {
					resultpresets = presets;
				}
				return new SimpleCOptionsPresetTaskOutput(resultpresets);
			}
		};
	}

}
