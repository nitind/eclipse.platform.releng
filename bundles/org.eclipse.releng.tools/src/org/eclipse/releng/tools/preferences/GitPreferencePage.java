/*******************************************************************************
 * Copyright (c) 2018 IBM Corporation and others.
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.preferences;

import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.releng.tools.RelEngPlugin;
import org.eclipse.releng.tools.git.IGitCommitConstants;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

public class GitPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	public GitPreferencePage() {
		super();
		setDescription(Messages.GitPreferencePage_0);
	}

	@Override
	protected void createFieldEditors() {
		addField(new StringFieldEditor(IGitCommitConstants.FILTER_STRINGS_KEY, Messages.GitPreferencePage_1,
				getFieldEditorParent()));
		addField(new StringFieldEditor(IGitCommitConstants.FILTER_STRINGSTARTS_KEY, Messages.GitPreferencePage_2,
				getFieldEditorParent()));
	}

	@Override
	protected IPreferenceStore doGetPreferenceStore() {
		return RelEngPlugin.getDefault().getPreferenceStore();
	}

	@Override
	public void init(IWorkbench workbench) {
	}
}
