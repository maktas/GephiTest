/*
Copyright 2008 WebAtlas
Authors : Mathieu Bastian, Mathieu Jacomy, Julian Bilcke
Website : http://www.gephi.org

This file is part of Gephi.

Gephi is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Gephi is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Gephi.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.gephi.io.importer.spi;

import org.gephi.io.importer.api.ContainerLoader;
import org.gephi.io.importer.api.Report;
import org.w3c.dom.Document;

/**
 * Importers interface for XML document source.
 *
 * @author Mathieu Bastian
 */
public interface XMLImporter extends FileFormatImporter {

    /**
     * Import data from XML <code>document</code> and push it to <code>container</code>. Informations,
     * logs and issues are pushed to <code>report</code> for further analysis and verification.
     * @param document the XML document
     * @param container container loading interface
     * @param report the import report for logging informations and issues
     * @return <code>true</code> if the import is successfull and can be processed or <code>false</code> otherwise
     * @throws java.lang.Exception for catching eventual exceptions
     */
    public boolean importData(Document document, ContainerLoader container, Report report) throws Exception;
}