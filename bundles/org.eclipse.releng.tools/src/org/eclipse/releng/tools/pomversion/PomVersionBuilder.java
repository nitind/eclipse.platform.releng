/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.pomversion;

import java.util.Map;
import java.util.jar.JarFile;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;

/**
 * Builder for creating POM version problems
 */
public class PomVersionBuilder extends IncrementalProjectBuilder {
	
	/**
	 * Project relative path to the pom.xml file
	 */
	public static final IPath POM_PATH = new Path("pom.xml"); //$NON-NLS-1$

	/**
	 * Project relative path to the manifest file.
	 */
	public static final IPath MANIFEST_PATH = new Path(JarFile.MANIFEST_NAME);

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#build(int, java.util.Map, org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IProject[] build(int kind, Map args, IProgressMonitor monitor) throws CoreException {
		if (!getProject().isAccessible()) {
			return null;
		}
		
		if (kind == FULL_BUILD) {
			fullBuild(monitor);
		} else {
			IResourceDelta delta = getDelta(getProject());
			if (delta == null) {
				fullBuild(monitor);
			} else {
				incrementalBuild(delta, monitor);
			}
		}
		return null;
	}	
	
	private void incrementalBuild(IResourceDelta delta, IProgressMonitor monitor) {
		PomResourceDeltaVisitor visitor = new PomResourceDeltaVisitor();
		try {
			delta.accept(visitor);
		} catch (CoreException e) {
			RelEngPlugin.log(e);
		}
		if (visitor.doBuild()){
			fullBuild(monitor);
		}
	}

	private void fullBuild(IProgressMonitor monitor) {
		IProject project = getProject();
		SubMonitor localMonitor = SubMonitor.convert(monitor, NLS.bind("Comparing POM and plug-in version in {0}", project.getName()), 10);
		IFile pomFile = project.getFile(POM_PATH);
		IFile manifestFile = project.getFile(MANIFEST_PATH);
		if (pomFile.exists() && manifestFile.exists()){
			PomVersionErrorReporter reporter = new PomVersionErrorReporter(manifestFile, pomFile);
			reporter.validate(localMonitor);
		}
		localMonitor.done();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.resources.IncrementalProjectBuilder#clean(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected void clean(IProgressMonitor monitor) throws CoreException {
		getProject().deleteMarkers(IPomVersionConstants.PROBLEM_MARKER_TYPE, false, IResource.DEPTH_INFINITE);
	}
	
	private class PomResourceDeltaVisitor implements IResourceDeltaVisitor {
		private boolean build;

		public boolean visit(IResourceDelta delta) {
			build = false;
			if (delta != null) {
				int kind = delta.getKind();
				// A file being removed doesn't affect the pom version check (markers are already cleaned)
				if (kind == IResourceDelta.REMOVED) {
					return false;
				}
				
				IResource resource = delta.getResource();
				// by ignoring derived resources we should scale a bit better.
				if (resource.isDerived())
					return false;
				if (resource.getType() == IResource.FILE) {
					IFile file = (IFile) resource;
					IFile pomFile = file.getProject().getFile(POM_PATH);
					if (file.equals(pomFile)){
						build = true;
						return false;
					}
					IFile manifestFile = file.getProject().getFile(MANIFEST_PATH);
					if (file.equals(manifestFile)){
						build = true;
						return false;
					}
					return false;
				}
			}
			return true;
		}

		/**
		 * @return whether the delta contained a file requiring a build
		 */
		public boolean doBuild() {
			return build;
		}
	}

}
