/*******************************************************************************
 * Copyright (c) 2010, 2018 AGETO Service GmbH and others.
 * All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which accompanies this distribution,
t https://www.eclipse.org/legal/epl-2.0/
t
t SPDX-License-Identifier: EPL-2.0.
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation of CVS adapter
 *     Gunnar Wagenknecht - initial API and implementation of Git adapter
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.releng.tools.git;

import java.io.IOException;
import java.util.Calendar;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.jgit.diff.DiffConfig;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.FollowFilter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.releng.tools.RepositoryProviderCopyrightAdapter;

public class GitCopyrightAdapter extends RepositoryProviderCopyrightAdapter {

	/* filter files if the most recent commit contains this string */
	private static String filterString = "copyright"; // lowercase //$NON-NLS-1$

	/* continue to the next commit if the current one starts with one of the following strings */
	private static String[] skipOnMessages = new String[] {"move bundles"}; //$NON-NLS-1$

	public GitCopyrightAdapter(IResource[] resources) {
		super(resources);
	}

	private boolean startsWithAnyOf(String test, String[] candidates) {
		for (int i = 0; i < candidates.length; i++) {
			if (test.startsWith(candidates[i])) return true;
		}
		return false;
	}

	@Override
	public int getLastModifiedYear(IFile file, IProgressMonitor monitor)
			throws CoreException {
		try {
			monitor.beginTask("Fetching logs from Git", 100); //$NON-NLS-1$
			final RepositoryMapping mapping = RepositoryMapping
					.getMapping(file);
			if (mapping != null) {
				final Repository repo = mapping.getRepository();
				if (repo != null) {
					try (RevWalk walk = new RevWalk(repo)) {
						ObjectId start = repo.resolve(Constants.HEAD);
						walk.setTreeFilter(FollowFilter.create(mapping.getRepoRelativePath(file),
								repo.getConfig().get(DiffConfig.KEY)));
						walk.markStart(walk.lookupCommit(start));
						RevCommit commit = walk.next();
						if (commit != null) {
							if (commit.getFullMessage().toLowerCase().contains(filterString)) {
								commit = walk.next();
								if (commit == null) {
									return 0;
								}
							}
							while (startsWithAnyOf(commit.getFullMessage().toLowerCase(), skipOnMessages)) {
								commit = walk.next();
								if (commit == null) {
									return 0;
								}
							}

							boolean isSWT= file.getProject().getName().startsWith("org.eclipse.swt"); //$NON-NLS-1$
							String logComment= commit.getFullMessage();
							if (isSWT && (logComment.indexOf("restore HEAD after accidental deletion") != -1 || logComment.indexOf("fix permission of files") != -1)) { //$NON-NLS-1$ //$NON-NLS-2$
								// ignore commits with above comments
								return 0;
							}

							boolean isPlatform= file.getProject().getName().equals("eclipse.platform"); //$NON-NLS-1$
							if (isPlatform && (logComment.indexOf("Merge in ant and update from origin/master") != -1 || logComment.indexOf("Fixed bug 381684: Remove update from repository and map files") != -1)) { //$NON-NLS-1$ //$NON-NLS-2$
								// ignore commits with above comments
								return 0;
							}


							final Calendar calendar = Calendar.getInstance();
							calendar.setTimeInMillis(0);
							calendar.add(Calendar.SECOND,
									commit.getCommitTime());
							return calendar.get(Calendar.YEAR);
						}
					} catch (final IOException e) {
						throw new CoreException(new Status(IStatus.ERROR,
								RelEngPlugin.ID, 0, NLS.bind(
										"An error occured when processing {0}",
										file.getName()), e));
					} finally {
					}
				}
			}
		} finally {
			monitor.done();
		}
		return -1;
	}

	@Override
	public void initialize(IProgressMonitor monitor) throws CoreException {
		// TODO We should perform a bulk "log" command to get the last modified
		// year
	}

}
