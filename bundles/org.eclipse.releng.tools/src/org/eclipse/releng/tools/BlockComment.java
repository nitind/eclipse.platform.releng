/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
 *
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
package org.eclipse.releng.tools;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;


/**
 * @author droberts
 */
public class BlockComment {

	int start;
	int end;
	private String contents;
	private static String newLine = System.getProperty("line.separator"); //$NON-NLS-1$
	private String copyrightHolder;
	private List<String> nonIBMContributors = new ArrayList<>();
	private String commentEnd;

    /** A regex mattern to match years in the range {@code 19** to 23** } */
    private static final String YEAR_REGEX = "(19|20|21|22|23)\\d{2}"; //$NON-NLS-1$

	
	/**
	 * @param commentStart
	 * @param commentEnd
	 * @param comment
	 */
	public BlockComment(int commentStartLine, int commentEndLine, String comment, String commentStartString, String commentEndString) {
		start = commentStartLine;
		end = commentEndLine;
		commentEnd = commentEndString;
		contents = comment;
	}

	public String getContents() {
	    return contents;
	}

	/**
	 * @return boolean
	 */
	public boolean isCopyright() {
		String lowercase = contents.toLowerCase();
		if (lowercase.indexOf("copyright") != -1) { //$NON-NLS-1$
			// look more thoroughly
			String[] lines = lowercase.split("\r|\n"); //$NON-NLS-1$
			for (int i = 0; i < lines.length; i++) {
				if (lines[i].matches(".*opyright.*" + YEAR_REGEX + ".*")) { //$NON-NLS-1$ //$NON-NLS-2$
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * @return boolean
	 */
	public boolean atTop() {
		return start == 0;
	}

	/**
	 * @return boolean
	 */
	public boolean notIBM() {
		
		String lowerCaseContents = contents.toLowerCase();
		if (copyrightHolder == null) {
			int start = lowerCaseContents.indexOf("copyright");
			if (start == -1) {
				return false;
			}
			
			int end = lowerCaseContents.indexOf(newLine, start);
			
			copyrightHolder = contents.substring(start + "copyright".length(), end);
		}
		
		String lowercaseCopyrightHolder = copyrightHolder.toLowerCase();
		
		int result = lowercaseCopyrightHolder.indexOf("ibm");
		if (result != -1) {
			return false;
		}
		
		result = lowercaseCopyrightHolder.indexOf("international business machine");
		if (result != -1) {
			return false;
		}
		
		return true;
	}

	/**
	 * @return String
	 */
	public String getCopyrightHolder() {
		return copyrightHolder;
	}

	/**
	 * 
	 */
	public List<String> nonIBMContributors() {

		String lowerCaseContents = contents.toLowerCase();
		int start = lowerCaseContents.indexOf("contributors");
		if (start == -1) {
			return nonIBMContributors;
		}
		
		start = lowerCaseContents.indexOf(newLine, start);
		if (start == -1) {
			return nonIBMContributors;
		}

		start = start + newLine.length();
		BufferedReader aReader = new BufferedReader(new StringReader(lowerCaseContents.substring(start)));
		
		String aLine;
		try {
			aLine = aReader.readLine();
			while (aLine != null) {
				aLine = aLine.trim();
				if ((aLine.length() > 0) && (aLine.indexOf(commentEnd) == -1)) {
					if ((aLine.indexOf("ibm") == -1) && (aLine.indexOf("international business machine") == -1)) {
						nonIBMContributors.add(aLine);
					}
				}
				aLine = aReader.readLine();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		return nonIBMContributors;
	}

}
