package saker.msvc.main.util;

import saker.std.api.file.location.ExecutionFileLocation;
import saker.std.api.file.location.FileLocationVisitor;
import saker.std.api.file.location.LocalFileLocation;

public class FileLocationFileNameVisitor implements FileLocationVisitor {
	private String result;

	public String getFileName() {
		return result;
	}

	@Override
	public void visit(LocalFileLocation loc) {
		result = loc.getLocalPath().getFileName();
	}

	@Override
	public void visit(ExecutionFileLocation loc) {
		result = loc.getPath().getFileName();
	}

}
