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
package saker.msvc.main.sdk;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.trace.BuildTrace;
import saker.msvc.impl.MSVCUtils;
import saker.msvc.impl.sdk.VersionsMSVCSDKDescription;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;

@NestTaskInformation(returnType = @NestTypeUsage(DocSDKDescription.class))
@NestInformation("Gets an SDK description for the Microsoft Visual C++ toolchain that matches the given versions.\n"
		+ "This task will create an SDK description that references the MSVC toolchain with any of the specified versions.\n"
		+ "The returned SDK can be specified as an input for the SDKs parameter to the "
		+ MSVCCCompileTaskFactory.TASK_NAME + "() and " + MSVCCLinkTaskFactory.TASK_NAME + "() tasks.")
@NestParameterInformation(value = "Versions",
		aliases = { "", "Version" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Specifies the suitable MSVC toolchain versions that can be used for this SDK.\n"
				+ "The versions are associated with toolchain installations since Visual Studio 2017. For earlier "
				+ "toolchains use the LegacyVersions parameter.\n"
				+ "The version numbers are expected to have the same format as they are under the "
				+ "Microsoft Visual Studio\\2019\\Community\\VC\\Tools\\MSVC directory if they are installed in the Program Files "
				+ "of the system. E.g.: 14.22.27905"))
@NestParameterInformation(value = "LegacyVersions",
		aliases = { "LegacyVersion" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Specifies the suitable legacy MSVC toolchain versions that can be used for this SDK.\n"
				+ "The versions are associated with toolchain installations before Visual Studio 2017.\n"
				+ "The version numbers are expected to have the same format as they are in the Microsoft Visual Studio 14.0 "
				+ "directory if they are installed in the Program Files of the system. E.g.: 14.0"))
public class MSVCSDKTaskFactory extends FrontendTaskFactory<SDKDescription> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.msvc.sdk";

	@Override
	public ParameterizableTask<? extends SDKDescription> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<SDKDescription>() {
			@SakerInput(value = { "", "Version", "Versions" })
			public Optional<Collection<String>> regularVersionsOption;

			@SakerInput(value = { "LegacyVersion", "LegacyVersions" })
			public Optional<Collection<String>> legacyVersionsOption;

			@Override
			public SDKDescription run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_CONFIGURATION);
				}

				if (regularVersionsOption == null && legacyVersionsOption == null) {
					//no args specified. use the default
					return MSVCUtils.DEFAULT_MSVC_SDK_DESCRIPTION;
				}
				Set<String> regularversions = ImmutableUtils
						.makeImmutableNavigableSet(regularVersionsOption.orElse(null));
				Set<String> legacyversions = ImmutableUtils
						.makeImmutableNavigableSet(legacyVersionsOption.orElse(null));
				//if a version set wasnt specified, then defaultize to an empty set to not use any by default
				if (regularVersionsOption == null && regularversions == null) {
					regularversions = Collections.emptySet();
				}
				if (legacyVersionsOption == null && legacyversions == null) {
					legacyversions = Collections.emptySet();
				}
				return VersionsMSVCSDKDescription.create(regularversions, legacyversions);
			}
		};
	}

}
