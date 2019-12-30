package saker.msvc.main.clink.options;

import saker.build.task.TaskContext;
import saker.msvc.api.ccompile.MSVCCompilerWorkerTaskOutput;

public class CompilerOutputLinkerInputOption
		implements CompilerOutputLinkerInputPass, LinkerInputPassTaskOption, LinkerInputPassOption {
	private MSVCCompilerWorkerTaskOutput compilerOutput;

	public CompilerOutputLinkerInputOption(MSVCCompilerWorkerTaskOutput compilerOutput) {
		this.compilerOutput = compilerOutput;
	}

	@Override
	public MSVCCompilerWorkerTaskOutput getCompilerOutput() {
		return compilerOutput;
	}

	@Override
	public LinkerInputPassTaskOption clone() {
		return this;
	}

	@Override
	public LinkerInputPassOption toLinkerInputPassOption(TaskContext taskcontext) {
		return this;
	}

	@Override
	public void accept(LinkerInputPassOptionVisitor visitor) {
		visitor.visit(this);
	}
}
