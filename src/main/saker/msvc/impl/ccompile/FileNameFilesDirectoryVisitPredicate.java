package saker.msvc.impl.ccompile;

import java.util.NavigableSet;

import saker.build.file.DirectoryVisitPredicate;
import saker.build.file.SakerDirectory;
import saker.build.file.SakerFile;

class FileNameFilesDirectoryVisitPredicate implements DirectoryVisitPredicate {
	private NavigableSet<String> fileNames;

	public FileNameFilesDirectoryVisitPredicate(NavigableSet<String> fileNames) {
		this.fileNames = fileNames;
	}

	@Override
	public boolean visitFile(String name, SakerFile file) {
		return fileNames.contains(name);
	}

	@Override
	public boolean visitDirectory(String name, SakerDirectory directory) {
		return false;
	}
}