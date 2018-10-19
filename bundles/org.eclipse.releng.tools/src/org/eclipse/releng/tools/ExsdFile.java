/*******************************************************************************
 * Copyright (c) 2008, 2014 Gunnar Wagenknecht and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Gunnar Wagenknecht - initial API and implementation
 *     Leo Ufimtsev lufimtse@redhat.com - fixed xml header issues.
 *      https://bugs.eclipse.org/381147
 *      https://bugs.eclipse.org/bugs/show_bug.cgi?id=276257  //used.
 *
 *******************************************************************************/
package org.eclipse.releng.tools;

import java.io.IOException;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;

/*
 * Test notes:
 * [x] Document with XML content, "documentation" element.
 */

/**
 * <h2> EXSD File handler. </h2>
 *
 * <p> This class deals with another special case of 'xml' files, where the only
 * "comment" to consider lies inside of a "documentation" element.</p>
 */
public class ExsdFile extends SourceFile {

	public ExsdFile(IFile file) {
		super(file);
	}


    @Override
	public boolean isCommentStart(String aLine) {
        return aLine.trim().contains(getCommentStart());
    }

    @Override
	public boolean isCommentEnd(String aLine, String commentStartString) {
        return aLine.trim().contains(getCommentEnd());
    }
	
	@Override
	public String getCommentStart() {
         return "<documentation>"; //$NON-NLS-1$
	}

	@Override
	public String getCommentEnd() {
		return "</documentation>"; //$NON-NLS-1$
	}
	
	@Override
	public int getFileType() {
		return CopyrightComment.XML_COMMENT;
	}
	
	/**
	 * Given the new constructed copyright comment, it inserts it into the the document.
	 *
	 * <p> Note, this is only called if inserting an actual comment.<br>
	 *  If only updating the year, this method is not called. </p>
	 * @see org.eclipse.releng.tools.SourceFile#doInsert(java.lang.String, org.eclipse.jface.text.IDocument)
	 */
        @Override
		protected void doInsert(final String comment, IDocument document) throws BadLocationException, IOException {

            //----------------- XML COMMENT CLEAN UP
            // XML comments need extra-tidy up because we need to consider the existance of an XML header
            String tidyComment = comment.trim();

            //Append new-line at the end for cleaner look.
            tidyComment += "\n";

            // check for existance of an xml header (<?xml)
            // If so, put the comment 'below' it.
            // example:
            //<?xml .... ?>
            //<--
            //    comment start....
            if (containsXmlEncoding(document)) {
                // If encoding is present, pre-append a new line.
                tidyComment = "\n" + tidyComment; //$NON-NLS-1$
            }

            //------------------ COMMENT INSERT
            // find insert offset (we must skip instructions)
            int insertOffset = findInsertOffset(document);

            // insert comment
            document.replace(insertOffset, 0, tidyComment);
        }

	/**
	 * Given the document, find the place after the xml header to insert the comment.
	 * @param document
	 * @return
	 * @throws BadLocationException
	 */
	private int findInsertOffset(IDocument document) throws BadLocationException {
		boolean inInstruction = false;
		int insertOffset = 0;
		
		for (int offset = 0; offset < document.getLength(); offset++) {
			char c = document.getChar(offset);
			
			// ignore whitespace and new lines
			if(Character.isWhitespace(c)) {
				// we update the offset to ignore whitespaces
				// after instruction ends
				insertOffset = offset;
				continue;
			}

			// look at next char
			char c2 = ((offset+1) < document.getLength()) ? document.getChar(offset+1) : 0;
			
			// look for instruction ending
			if(inInstruction) {
				if(c == '?' && c2 == '>') {

				        //Offset is '+2' not '+1' because of '?' in '?>'
					insertOffset = offset + 2;
					inInstruction = false;
					offset++; // don't need to analyse c2 again
					// we continue in case there are more instructions
					continue;
				} else {
					// look for ending
					continue;
				}
			}
			
			// next chars must start an instruction
			if(c == '<' && c2 =='?') {
				inInstruction = true;
				offset++; // don't need to analyse c2 again
				continue;
			} else {
				// if it's something else, we can stop seeking
				break;
			}
		}
		return insertOffset;
	}

	/**
	 * Find out if an XML document contains an XML meta header.
	 *
	 * <p> XML documents <b> sometimes </b> contain a header specifying various attributes such as  <br>
	 * version, encoding etc... </p>
	 *
	 * <p> Examples include: <br>
	 * {@literal <?xml version="1.0" encoding="UTF-8"?> }<br>
	 * {@literal<?xml version="1.0" encoding="UTF-8" standalone="no"?> } <br>
	 * {@literal <?xml version="1.0" ?> } </p>
	 *
	 * @param xmlDoc
	 * @return             True if it contains a header.
	 * @throws BadLocationException
	 */
	public boolean containsXmlEncoding(IDocument xmlDoc) throws BadLocationException {

	    //XML attribute headers *must* reside on the first line.
	    //We identify if the xml document contains a header by checking the first tag and see if it starts with: <?xml

	    //-- Check to see if the document is long enough to contain the minimum '<?xml?>' tag
	    if (xmlDoc.getLength() < 7) {
	        return false;
	    }

	    for (int offset = 0; offset < xmlDoc.getLength(); offset++) {

	        //Read Char.
	        char c = xmlDoc.getChar(offset);

                // ignore whitespace and new lines
                if(Character.isWhitespace(c)) {
                        continue;
                }

                //Once we've found the first '<', check that it's a '<?xml'
                if (c == '<') {

                    //check that document is long enough to close a header if we are to read it: '<?xml
                    if ((offset + 4) < xmlDoc.getLength()) {

                        //Read "<?xml" equivalent.
                        String xmlTag = "" + c + xmlDoc.getChar(offset+1) + xmlDoc.getChar(offset+2) + //$NON-NLS-1$
                                                 xmlDoc.getChar(offset+3) + xmlDoc.getChar(offset+4);

                        if ( xmlTag.compareToIgnoreCase("<?xml") == 0) { //$NON-NLS-1$
                            return true;
                        } else {
                            return false;
                        }
                    }
                }
	    }

	    //if parsing an empty xml document, return false.
	    return false;
	}

}
