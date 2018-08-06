/**
* Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
* Deutsches Krebsforschungszentrum (DKFZ)
*
* This file is part of Jummp.
*
* Jummp is free software; you can redistribute it and/or modify it under the
* terms of the GNU Affero General Public License as published by the Free
* Software Foundation; either version 3 of the License, or (at your option) any
* later version.
*
* Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
* WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
* A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
* details.
*
* You should have received a copy of the GNU Affero General Public License along
* with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
*
* Additional permission under GNU Affero GPL version 3 section 7
*
* If you modify Jummp, or any covered work, by linking or combining it with
* Apache Commons (or a modified version of that library), containing parts
* covered by the terms of Apache License v2.0, the licensors of this
* Program grant you additional permission to convey the resulting work.
* {Corresponding Source for a non-source form of such a combination shall
* include the source code for the parts of Apache Commons used as well as
* that of the covered work.}
**/





package net.biomodels.jummp.core.util

import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException
import javax.xml.stream.XMLStreamReader
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory

public class JummpXmlUtils {
    private static final Log log = LogFactory.getLog(this)
    private static final boolean IS_INFO_ENABLED = log.isInfoEnabled()

    public static String findModelElement(final File model, final String elementName) {
        if (!model || !model.canRead() || !elementName) {
            log.error("Refusing to find element ${elementName} in file ${model?.properties}.")
            return ""
        }
        String elem = parseXmlFile.curry(model)({ XMLStreamReader r ->
            if (elementName.equals(r.getLocalName())) {
                return r.getElementText()
            }
            return false
        })
        return elem ? elem : ""
    }

    public static def parseXmlFile = { File f, Closure action ->
        // deal with sanitisation elsewhere
        assert f && f.canRead()
        def fileReader = new FileReader(f)
        String theResult
        XMLInputFactory factory = XMLInputFactory.newInstance()
        factory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.FALSE)
        XMLStreamReader xmlReader
        try {
            xmlReader = factory.createXMLStreamReader(fileReader)
            boolean found = false
            while (xmlReader.hasNext() && !found) {
                xmlReader.next()
                if (xmlReader.startElement) {
                    def result = action(xmlReader)
                    if (result) {
                        theResult = result
                        found = true
                    } else {
                        xmlReader.next()
                    }
                }
            }
        } catch (XMLStreamException e) {
            log.error("Error while parsing XML file ${model.properties}: ${e.message}.", e)
        } finally {
            xmlReader?.close()
            fileReader?.close()
            return theResult
        }
    }

    public static String findModelAttribute(final File model, String elementName, String attributeName) {
        if (!model || !model.canRead()) {
            def errMsg = new StringBuilder("Cannot find ").append(elementName).append(".").append(attributeName)
            errMsg.append(" in file ").append(model?.properties)
            log.error errMsg.toString()
            return ""
        }
        if (null == elementName) {
            elementName = ""
        }
        if (null == attributeName) {
            attributeName = ""
        }
        if (IS_INFO_ENABLED) {
            def info = new StringBuilder("Extracting attribute ").append(attributeName).
                        append(" of element ").append(elementName).append(" from ").append(model.properties)
            log.info(info.toString())
        }
        String attr = parseXmlFile.curry(model)({ XMLStreamReader reader ->
            if (elementName.equals(reader.getLocalName())) {
                if (attributeName.startsWith("xmlns")) {
                    if (attributeName.contains(":")) {
                        // Can't use substring() as the underlying array reference has been dropped
                        String ns = attributeName.dropWhile{it != ':'}.drop(1)
                        return reader.getNamespaceURI(ns)
                    }
                    return reader.getNamespaceURI()
                }
                return reader.getAttributeValue(null, attributeName)
            }
            return ""
       })
    }
}
