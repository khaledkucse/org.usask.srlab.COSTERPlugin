package org.usask.srlab.coster.core;

import java.io.*;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.core.ClasspathEntry;

public class AddLibrary {
	public static void addLibrary(String projectName, ArrayList<File> jarFiles) throws MalformedURLException, IOException, URISyntaxException, CoreException {
		IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
		IJavaProject jProject = JavaCore.create(project);
	
		for(File file : jarFiles){
		    if(file.isFile() && file.getName().endsWith(".jar")){
		        addProjectLibrary(jProject, file);
		    }
		}
	}

	private static void addProjectLibrary(IJavaProject jProject, File jarLibrary) throws IOException, URISyntaxException, MalformedURLException, CoreException {
	    // copy the jar file into the project
//	    InputStream jarLibraryInputStream = new BufferedInputStream(new FileInputStream(jarLibrary));
//	    IFile libFile = jProject.getProject().getFile(jarLibrary.getName());
//	    libFile.create(jarLibraryInputStream, false, null);
		
		IPath ipath = new Path(jarLibrary.getAbsolutePath());
	    // create a classpath entry for the library
	    IClasspathEntry relativeLibraryEntry = new org.eclipse.jdt.internal.core.ClasspathEntry(
	        IPackageFragmentRoot.K_BINARY,
	        IClasspathEntry.CPE_LIBRARY, 
	        ipath,
	        ClasspathEntry.INCLUDE_ALL, // inclusion patterns
	        ClasspathEntry.EXCLUDE_NONE, // exclusion patterns
	        null, null, null, // specific output folder
	        false, // exported
	        ClasspathEntry.NO_ACCESS_RULES, false, // no access rules to combine
	        ClasspathEntry.NO_EXTRA_ATTRIBUTES);

	    // add the new classpath entry to the project's existing entries
	    IClasspathEntry[] oldEntries = jProject.getRawClasspath();
	    IClasspathEntry[] newEntries = new IClasspathEntry[oldEntries.length + 1];
	    System.arraycopy(oldEntries, 0, newEntries, 0, oldEntries.length);
	    newEntries[oldEntries.length] = relativeLibraryEntry;
	    jProject.setRawClasspath(newEntries, null);
	}

}
