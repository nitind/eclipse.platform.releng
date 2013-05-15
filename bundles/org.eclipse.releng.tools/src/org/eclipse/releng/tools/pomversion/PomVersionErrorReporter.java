/*******************************************************************************
 *  Copyright (c) 2013 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.releng.tools.pomversion;

import java.util.Stack;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.filebuffers.FileBuffers;
import org.eclipse.core.filebuffers.ITextFileBuffer;
import org.eclipse.core.filebuffers.ITextFileBufferManager;
import org.eclipse.core.filebuffers.LocationKind;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.osgi.util.NLS;
import org.eclipse.releng.tools.RelEngPlugin;
import org.osgi.framework.Version;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Validates the content of the pom.xml.  Currently the only check is that the
 * version specified in pom.xml matches the bundle version.
 *
 */
public class PomVersionErrorReporter {

	private static final String ELEMENT_PROJECT = "project"; //$NON-NLS-1$
	private static final String ELEMENT_VERSION = "version"; //$NON-NLS-1$
	private static final String SNAPSHOT_SUFFIX = "-SNAPSHOT"; //$NON-NLS-1$

	private SAXParserFactory parserFactory = SAXParserFactory.newInstance();
	private String pomVersionSeverity;
	
	private IFile manifestFile;
	private IFile pomFile;

	public PomVersionErrorReporter(IFile manifest, IFile pom) {
		manifestFile = manifest;
		pomFile = pom;
	}

	protected void validate(IProgressMonitor monitor) {
		SubMonitor subMon = SubMonitor.convert(monitor, 10);
		try {
			if (subMon.isCanceled()) {
				return;
			}
			if (!manifestFile.exists() || !pomFile.exists()){
				return;
			}
			
			IPreferencesService service = Platform.getPreferencesService();
			pomVersionSeverity = service.getString(RelEngPlugin.ID, IPomVersionConstants.POM_VERSION_ERROR_LEVEL, IPomVersionConstants.VALUE_IGNORE, new IScopeContext[] {new ProjectScope(manifestFile.getProject())});
			if (pomVersionSeverity == IPomVersionConstants.VALUE_IGNORE) {
				return;
			}
			
			// Get the manifest version
			Version bundleVersion = Version.emptyVersion;

			// Compare it to the pom file version
			try {
				SAXParser parser = parserFactory.newSAXParser();
				PomVersionHandler handler = new PomVersionHandler(pomFile, bundleVersion);
				parser.parse(pomFile.getContents(), handler);
			} catch (Exception e1) {
				// Ignored, if there is a problem with the pom file don't create a marker
			}

		} finally {
			subMon.done();
			if (monitor != null) {
				monitor.done();
			}
		}

	}

	private void reportMarker(String message, int lineNumber, int charStart, int charEnd, String correctedVersion) {
		try {
			IMarker marker = pomFile.createMarker(IPomVersionConstants.PROBLEM_MARKER_TYPE);
			marker.setAttribute(IMarker.MESSAGE, message);
			if (pomVersionSeverity != null && pomVersionSeverity.equals(IPomVersionConstants.VALUE_WARNING)){
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			} else {
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
			}
			if (lineNumber == -1)
				lineNumber = 1;
			marker.setAttribute(IMarker.LINE_NUMBER, lineNumber);
			marker.setAttribute(IMarker.CHAR_START, charStart);
			marker.setAttribute(IMarker.CHAR_END, charEnd);
			marker.setAttribute(IPomVersionConstants.POM_CORRECT_VERSION, correctedVersion);
		} catch (CoreException e){
			RelEngPlugin.log(e);
		}
	}

	class PomVersionHandler extends DefaultHandler {
		private Version bundleVersion;
		private Stack elements = new Stack();
		private boolean checkVersion = false;
		private Locator locator;

		public PomVersionHandler(IFile file, Version bundleVersion) {
			this.bundleVersion = bundleVersion;
		}

		public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		}

		public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
			if (ELEMENT_VERSION.equals(qName)) {
				if (!elements.isEmpty() && ELEMENT_PROJECT.equals(elements.peek())) {
					checkVersion = true;
				}
			}
			elements.push(qName);
		}

		public void endElement(String uri, String localName, String qName) throws SAXException {
			elements.pop();
		}

		public void characters(char[] ch, int start, int length) throws SAXException {
			if (checkVersion) {
				checkVersion = false;
				// Compare the versions
				String versionString = new String(ch, start, length);
				try {
					// Remove snapshot suffix
					int index = versionString.indexOf(SNAPSHOT_SUFFIX);
					if (index >= 0) {
						versionString = versionString.substring(0, index);
					}
					Version pomVersion = Version.parseVersion(versionString);
					// Remove qualifiers and snapshot
					Version bundleVersion2 = new Version(bundleVersion.getMajor(), bundleVersion.getMinor(), bundleVersion.getMicro());
					Version pomVersion2 = new Version(pomVersion.getMajor(), pomVersion.getMinor(), pomVersion.getMicro());

					if (!bundleVersion2.equals(pomVersion2)) {
						String correctedVersion = bundleVersion2.toString();
						if (index >= 0) {
							correctedVersion = correctedVersion.concat(SNAPSHOT_SUFFIX);
						}

						try {
							// Need to create a document to calculate the markers charstart and charend
							IDocument doc = createDocument(pomFile);
							int offset = doc.getLineOffset(locator.getLineNumber() - 1); // locator lines start at 1
							int charEnd = offset + locator.getColumnNumber() - 1; // returns column at end of character string, columns start at 1
							int charStart = charEnd - length;
							reportMarker(NLS.bind("POM artifact version {0} does not match bundle version {1}", pomVersion2.toString(), bundleVersion2.toString()), locator.getLineNumber(), charStart, charEnd, correctedVersion);
						} catch (BadLocationException e) {
							RelEngPlugin.log(e);
						}
					}
				} catch (IllegalArgumentException e) {
					// Do nothing, user has a bad version
				}
			}
		}
	}
	
	protected IDocument createDocument(IFile file) {
		if (!file.exists()) {
			return null;
		}
		ITextFileBufferManager manager = FileBuffers.getTextFileBufferManager();
		if (manager == null) {
			return null;
		}
		try {
			manager.connect(file.getFullPath(), LocationKind.NORMALIZE, null);
			ITextFileBuffer textBuf = manager.getTextFileBuffer(file.getFullPath(), LocationKind.NORMALIZE);
			IDocument document = textBuf.getDocument();
			manager.disconnect(file.getFullPath(), LocationKind.NORMALIZE, null);
			return document;
		} catch (CoreException e) {
			RelEngPlugin.log(e);
		}
		return null;
	}
}
