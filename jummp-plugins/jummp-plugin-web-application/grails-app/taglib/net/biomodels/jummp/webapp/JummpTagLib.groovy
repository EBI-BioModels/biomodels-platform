/**
* Copyright (C) 2010-2016 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
**/





package net.biomodels.jummp.webapp

import javax.xml.transform.stream.StreamSource
import javax.xml.transform.stream.StreamResult
import net.biomodels.jummp.core.model.RepositoryFileTransportCommand

class JummpTagLib {
    static namespace = "jummp"

    def grailsApplication

    private boolean isDDMoReDeployment() {
        String deploymentEnvironment = grailsApplication.config.jummp.branding.deployment
        return deploymentEnvironment.equalsIgnoreCase("ddmore")
    }

    def displayExistingMainFile = { attrs ->
        def result = new StringBuilder()
        String mainFileLabel = "submission.upload.mainFile.label"
        if (isDDMoReDeployment()) {
            mainFileLabel = "submission.upload.mainFile.ddmore.label"
        }
        if (!attrs.main) {
            result.append("<tr class='prop'>\n\t<td class='name'>\n\t\t<label for='mainFile'>\n\t\t\t")
            result.append(message(code: mainFileLabel))
            result.append("\n\t\t</label>\n\t</td>\n\t<td class='value'>\n\t\t")
            result.append("<input type='file' id='mainFile' name='mainFile'/>\n\t</td>\n</tr>")
            out << result.toString()
            return
        }
        attrs.main.each { m ->
            RepositoryFileTransportCommand command = m as RepositoryFileTransportCommand
            String name = new File(command.path).name

            result.append("<tr class='prop'>\n\t<td class='name'>\n\t\t<label for='mainFile'>\n\t\t\t")
            result.append(message(code: mainFileLabel))
            result.append("\n\t\t</label>\n\t</td>\n\t<td class='value'>\n\t\t")
            result.append("<span id='mainName_").append(name).append("'>").append(name).append("</span>\n\t\t")
            result.append("<input style='display:none;' type='file' id='mainFile' data-labelname='${name}' name='mainFile' class='mainFile'/>\n\t")
            result.append("<a href='#' class='replaceMain'>Replace</a> | <a href='#' class='removeMain'>Remove</a></td>\n</tr>\n")
        }
        out << result.toString()
    }

    def displayExistingAdditionalFiles = { attrs ->
        if (!attrs.additionals) {
            return
        }
        attrs.additionals.each { f ->
            RepositoryFileTransportCommand command = f as RepositoryFileTransportCommand
            String name = new File(command.path).name
            out << "<tr class='fileEntry'>\n\t<td class='name'>"
            out << name
            out << "</td>\n\t<td></td>\n\t<td>"
            out << "<a href='#' class='killer' title='Discard file'>Discard</a></td>\n</tr>\n"
        }
    }

    def renderAdditionalFilesLegend = {
        String additionalFilesLegend = "submission.upload.additionalFiles.legend"
        if (isDDMoReDeployment()) {
            additionalFilesLegend = "submission.upload.additionalFiles.ddmore.legend"
        }
        out << message(code: additionalFilesLegend)
    }

    def renderAdditionalFilesAddButton = {
        String deploymentEnvironment = grailsApplication.config.jummp.branding.deployment
        String additionalFilesAddButton = "submission.upload.additionalFiles.addButton"
        if (deploymentEnvironment.equalsIgnoreCase("ddmore")) {
            additionalFilesAddButton = "submission.upload.additionalFiles.ddmore.addButton"
        }
        out << message(code: additionalFilesAddButton)
    }

    def renderSubmitForPublicationConfirmDialogMessage = {
        String submitForPublicationConfirmDialogMessage = "model.toolbar.submit-for-publication.message"
        if (isDDMoReDeployment()) {
            submitForPublicationConfirmDialogMessage = "model.toolbar.submit-for-publication.ddmore.message"
        }
        out << message(code: submitForPublicationConfirmDialogMessage)
    }

    def renderSubmitForPublicationConfirmDialogTitle = {
        String submitForPublicationConfirmDialogMessage = "model.toolbar.submit-for-publication.title"
        if (isDDMoReDeployment()) {
            submitForPublicationConfirmDialogMessage = "model.toolbar.submit-for-publication.ddmore.title"
        }
        out << message(code: submitForPublicationConfirmDialogMessage)
    }

    /**
     * Renders the HTML code for a JUMMP styled button.
     * That is including the active glow.
     * @attr id The HTML id (optional)
     * @attr class The HTML class (optional)
     **/
    def button = { attrs, body ->
        out << render(template: "/templates/buttonTemplate", model: [attrs: attrs, body: body()], plugin: "jummp-plugin-web-application")
    }

    /**
    * Renders a compact title of a publication.
    * Format: Journal Year Month; Volume (Issue) Pages
    * All fields are optional.
    * @attr publication REQUIRED The PublicationTransportCommand
    */
   def compactPublicationTitle = { attrs ->
       def publication = attrs.publication
       String publicationSummary = publication.journal
       if (publication.year) {
           publicationSummary += " " + publication.year
           if (publication.month) {
               publicationSummary += " " + publication.month
           }
       }
       if (publication.volume) {
           publicationSummary += "; " + publication.volume
       }
       if (publication.issue) {
           publicationSummary += "(" + publication.issue + ")"
       }
       if (publication.pages) {
           publicationSummary += ": " + publication.pages
       }
       out << publicationSummary
   }

   /**
    * Renders a span element with an alert icon and initially hidden.
    */
   def errorField = { attrs ->
       out << "<span class=\"ui-icon ui-icon-alert\" style=\"display: none\"></span>"
   }

   /**
    * Renders a list of Annotations. An annotation is a map consisting of following key/value pairs:
    * @li biologicalQualifier
    * @li modelQualifier
    * @li qualifier
    * @li resources
    * A description of the keys can be found in ISbmlService.getAnnotations().
    * @attr annotations List of Annotation maps (required)
    * @attr biological Boolean attribute whether to include annotations with biological qualifier (optional, default @c true)
    * @attr model Boolean attribute whether to include annotations with model qualifier (optional, decault @c true)
    * @todo move into an SBML taglib
    */
   def annotations = { attrs ->
       boolean biologicalAnnotations = true
       boolean modelAnnotations = true
       if (attrs.containsKey("biological")) {
           biologicalAnnotations = Boolean.parseBoolean(attrs.biological)
       }
       if (attrs.containsKey("model")) {
           modelAnnotations = Boolean.parseBoolean(attrs.model)
       }
       List<String> renderedAnnotations = []
       attrs.annotations.each { annotation ->
           if (annotation.qualifier == "BQB_UNKNOWN" || annotation.qualifier == "BQM_UNKNOWN") {
               return
           }
           if (annotation.biologicalQualifier && !biologicalAnnotations) {
               return
           }
           if (annotation.modelQualifier && !modelAnnotations) {
               return
           }
           renderedAnnotations << renderAnnotation(annotation)
       }
       if (renderedAnnotations.size() == 1) {
           out << renderedAnnotations.first()
       } else {
           out << "<ul>"
           renderedAnnotations.each {
               out << "<li>${it}</li>"
           }
           out << "</ul>"
       }
   }

   /**
    * Renders one Annotations Map.
    * @see annotations
    * @todo move into an SBML taglib
    */
   def renderAnnotation = { annotation ->
       renderQualifer(annotation.qualifier)
       if (annotation.resources.size() == 1) {
           out << "&nbsp;"
           out << renderURN(annotation.resources[0])
       } else {
           out << "<ul>"
           annotation.resources.each {
               out << "<li>"
               out << renderURN(it)
               out << "</li>"
           }
           out << "</ul>"
       }
   }

   /**
    * Renders a single MIRIAM URN from an annotation resource.
    * @todo move into an SBML taglib
    */
   def renderURN = { attrs ->
       Map miriam = attrs
       if (attrs.resource) {
           miriam = attrs.resource
       }
       if (miriam.containsKey("dataTypeLocation") && miriam.containsKey("dataTypeName") && miriam.containsKey("name") && miriam.containsKey("url")) {
           out << "<a target=\"_blank\" href=\"${miriam["dataTypeLocation"]}\">${miriam["dataTypeName"]}</a>"
           out << "&nbsp;"
           out << "<a target=\"_blank\" href=\"${miriam["url"]}\">${miriam.name}</a>"
       } else if (miriam.containsKey("name")) {
           out << miriam["name"]
       } else if (miriam.containsKey("urn")) {
           out << miriam["urn"]
       } else {
           out << miriam
       }
   }

   /**
    * Renders a Biomodels.net annotation qualifier.
    * The qualifier has to be one of org.sbml.jsbml.CVTerm.Qualifier
    * @todo move into an SBML taglib
    */
   def renderQualifer = { qualifier ->
       switch (qualifier) {
       case "BQB_ENCODES":
           out << g.message(code: "sbml.qualifier.bqb.encodes")
           break
       case "BQB_HAS_PART":
           out << g.message(code: "sbml.qualifier.bqb.hasPart")
           break
       case "BQB_HAS_PROPERTY":
           out << g.message(code: "sbml.qualifier.bqb.hasProperty")
           break
       case "BQB_HAS_VERSION":
           out << g.message(code: "sbml.qualifier.bqb.hasVersion")
           break
       case "BQB_IS":
           out << g.message(code: "sbml.qualifier.bqb.is")
           break
       case "BQB_IS_DESCRIBED_BY":
           out << g.message(code: "sbml.qualifier.bqb.isDescribedBy")
           break
       case "BQB_IS_ENCODED_BY":
           out << g.message(code: "sbml.qualifier.bqb.isEncodedBy")
           break
       case "BQB_IS_HOMOLOG_TO":
           out << g.message(code: "sbml.qualifier.bqb.isHomologTo")
           break
       case "BQB_IS_PART_OF":
           out << g.message(code: "sbml.qualifier.bqb.isPartOf")
           break
       case "BQB_IS_PROPERTY_OF":
           out << g.message(code: "sbml.qualifier.bqb.isPropertyOf")
           break
       case "BQB_IS_VERSION_OF":
           out << g.message(code: "sbml.qualifier.bqb.isVersionOf")
           break
       case "BQB_OCCURS_IN":
           out << g.message(code: "sbml.qualifier.bqb.occursIn")
           break
       case "BQM_IS":
           out << g.message(code: "sbml.qualifier.bqm.is")
           break
       case "BQM_IS_DERIVED_FROM":
           out << g.message(code: "sbml.qualifier.bqm.isDerivedFrom")
           break
       case "BQM_IS_DESCRIBED_BY":
           out << g.message(code: "sbml.qualifier.bqm.isDescribedBy")
           break
       default:
           out << ""
           break
       }
   }

   def contentMathML = { attrs ->
       if (!transformer) {
           def factory = TransformerFactory.newInstance()
           transformer = factory.newTransformer(new StreamSource(new File(servletContext.getRealPath("/xsl/mathmlc2p.xsl"))))
       }
       transformer.transform(new StreamSource(new StringReader(attrs.mathML)), new StreamResult(out))
   }

   /**
    * Renders a table row with the resolved SBO term.
    * The primary use for this tag is inside of the tooltips for various SBML elements
    * The tag expects an attribute sbo containing the integer value of the sbo term.
    * In case the tag is not set or an empty string the table row is not rendered.
    * @attr sbo REQUIRED the numerical SBO term without the urn header
    */
   def sboTableRow = { attrs ->
       Map sbo = attrs
       if (attrs.sbo instanceof Map) {
           sbo = attrs.sbo
       }
       if (!sbo || sbo.isEmpty()) {
           return
       }
       out << render(template: "/templates/sboTableRow", model: [urn: sbo])
   }

   /**
    * Renders a table row with the given annotations.
    * The primary use for this tag is inside of the tooltips for various SBML elements.
    * The tag expects an attribute annotations which is a list of the annotations to be rendered.
    * In case the list is empty the table row is not rendered.
    * @attr annotations REQUIRED The list of annotations
    */
   def annotationsTableRow = { attrs ->
       if (!attrs.annotations) {
           return;
       }
       out << render(template: "/templates/annotationsTableRow", model: [annotations: attrs.annotations])
   }
}
