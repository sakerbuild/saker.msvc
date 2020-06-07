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
import java.util.Set;

import saker.build.runtime.execution.ExecutionContext;
import saker.build.task.ParameterizableTask;
import saker.build.task.TaskContext;
import saker.build.task.utils.annot.SakerInput;
import saker.build.thirdparty.saker.util.ImmutableUtils;
import saker.build.trace.BuildTrace;
import saker.msvc.main.ccompile.MSVCCCompileTaskFactory;
import saker.msvc.main.clink.MSVCCLinkTaskFactory;
import saker.nest.scriptinfo.reflection.annot.NestInformation;
import saker.nest.scriptinfo.reflection.annot.NestParameterInformation;
import saker.nest.scriptinfo.reflection.annot.NestTaskInformation;
import saker.nest.scriptinfo.reflection.annot.NestTypeUsage;
import saker.nest.utils.FrontendTaskFactory;
import saker.sdk.support.api.SDKDescription;
import saker.sdk.support.main.TaskDocs.DocSDKDescription;
import saker.windows.api.SakerWindowsUtils;

@NestTaskInformation(returnType = @NestTypeUsage(DocSDKDescription.class))
@NestInformation("Depretaced. User saker.windows.sdk.windowskits().\n"
		+ "Gets an SDK description for the Windows Kits that matches the given versions.\n"
		+ "This task will create an SDK description that references the Windows operating system SDK with "
		+ "any of the specified versions.\n"
		+ "The returned SDK can be specified as an input for the SDKs parameter to the "
		+ MSVCCCompileTaskFactory.TASK_NAME + "() and " + MSVCCLinkTaskFactory.TASK_NAME + "() tasks.")
@NestParameterInformation(value = "Versions",
		aliases = { "", "Version" },
		type = @NestTypeUsage(value = Collection.class, elementTypes = String.class),
		info = @NestInformation("Specifies the suitable Windows Kits versions that can be used for this SDK.\n"
				+ "The version numbers are expected to have the same format as they are under the "
				+ "Windows Kits\\<OS>\\bin directory if they are installed in the Program Files "
				+ "of the system. E.g.: 10.0.18362.0"))
@Deprecated
public class WindowsKitsSDKTaskFactory extends FrontendTaskFactory<SDKDescription> {
	private static final long serialVersionUID = 1L;

	public static final String TASK_NAME = "saker.msvc.sdk.windowskits";

	@Override
	public ParameterizableTask<? extends SDKDescription> createTask(ExecutionContext executioncontext) {
		return new ParameterizableTask<SDKDescription>() {

			@SakerInput(value = { "", "Version", "Versions" })
			public Collection<String> versionsOption;

			@Override
			public SDKDescription run(TaskContext taskcontext) throws Exception {
				if (saker.build.meta.Versions.VERSION_FULL_COMPOUND >= 8_006) {
					BuildTrace.classifyTask(BuildTrace.CLASSIFICATION_CONFIGURATION);
				}

				Set<String> versions = ImmutableUtils.makeImmutableNavigableSet(versionsOption);
				return SakerWindowsUtils.getWindowsKitsSDKForVersions(versions);
			}
		};
	}

}
