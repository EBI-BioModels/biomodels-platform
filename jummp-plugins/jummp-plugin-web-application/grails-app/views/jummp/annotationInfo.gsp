<%--
 Copyright (C) 2010-2025 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <https://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="${titleCode}" default="Annotation Information"/> | BioModels</title>
</head>

<body>
<p class="back-to-curation-docs">
    &laquo; <a href="${createLink(controller: 'jummp', action: 'curatorZone')}">Curation documentation</a>
</p>

<h1>BioModels Database Annotation Information</h1>


<p>
  This document contains a general introduction to the annotation of models performed by the curators of BioModels Database.
</p>


<h2>What is annotation?</h2>

<p>
  The process of annotating a model consists in identifying the model components. This is achieved by linking model components with terms from controlled vocabularies and entries in data resources. This is similar to attaching cross-references to the model with the addition of qualifiers, in order to explicitly define the relationship between the model component and the target element.
</p>


<h2>Why annotating models?</h2>

<p>
  In publications describing models, the names of different model elements such as genes, proteins and metabolites, or the organisms from which the model is derived, are often denoted by biologically non-meaningful names. The entire paper describing the model should be read to know the relationships between each model element and their biological meaning. To make this easier and to allow users to directly relate the model as well as the model elements to the relevant biological concepts, efforts are being put to enrich models with annotations.
</p>
<p>
  Annotated models provide numerous advantages. Their understanding by both users and software tools is highly enhanced, which therefore lower the barriers of their reuse. Models can more accurately be searched in a repository. Tasks such as model comparison, clustering of similar models and integration with other models are made possible with annotated models. Annotation also facilitates the conversion of models to other formats.
</p>


<h2>How to encode annotation?</h2>

<p>
  Annotation in the SBML model files provided by BioModels Database is encoded using the SBML controlled annotation scheme. Please refer to the <a href="http://sbml.org/Documents/Specifications" title="SBML specifications">SBML specifications</a> (specially the section entitled "A standard format for the annotation element") for more detailed information. This scheme makes use of <a href="http://www.ebi.ac.uk/miriam/">MIRIAM URIs</a> (<a href="http://identifiers.org/pubmed/22140103?resource=MIR:00100032" title="Access to publication: Identifiers.org and MIRIAM Registry: community resources to provide persistent identification" class="external">Juty et al. 2012</a>), <a href="http://biomodels.net/qualifiers/" title="BioModels.net qualifiers">BioModels.net qualifiers</a>, but also <a href="http://dublincore.org/" title="The Dublin Core&reg; Metadata Initiative" class="external">Dublin Core</a>, <a href="http://en.wikipedia.org/wiki/VCard" title="vCard on Wikipedia" class="external">vCard</a> and <a href="http://www.w3.org/RDF/" title="Resource Description Framework (RDF)" class="external">RDF</a>.
</p>

<div class="doc_img">
  <img src="https://www.biomodels.org/static-assets/GRAPHICS/SBML_annotation_example.png" alt="Example of annotation in SBML" title="Example of annotation in SBML" />
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 1</span> Example of one annotation block from an SBML file.
  </div>
</div>

<p>
  The <span class="mom_figure_ref">Figure 1</span> shows that the SBML <tt>species</tt> L_EGFR is actually a complex (usage of the qualifier <b>hasPart</b>) composed of two proteins (presence of 2 URIs pointing to UniProt nested under the qualifier). This was shown for information purposes only, as many tools allow the creation and edition of annotation without the need for the user to understand how the information is ultimately stored in the file.
</p>
<p>
  Details about MIRIAM URIs can be found from the <a href="http://www.ebi.ac.uk/miriam/" title="MIRIAM Registry">Registry website</a> (which provides services to generate those URIs).
</p>


<h2>How to identify relevant resources for annotation?</h2>

<p>
  The choice of which resource to use depends on the kind of model component which needs to be annotated.
</p>
<p>
  To date, model elements in BioModels Database are annotated using more than 45 different external resources. Some of the predominantly used external resources for model annotations in BioModels Database are:
</p>
<ul>
  <li><a href="http://geneontology.org/" title="Gene Ontology (GO)">Gene Ontology</a></li>
  <li><a href="http://www.ebi.ac.uk/chebi/" title="Chemical Entities of Biological Interest (ChEBI)">ChEBI ontology</a></li>
  <li><a href="http://www.brenda-enzymes.org/ontology/" title="BRENDA Tissue Ontology (BTO)">Brenda Tissue Ontology</a></li>
  <li><a href="https://www.ebi.ac.uk/sbo/" title="Systems Biology Ontology (SBO)">Systems Biology Ontology</a></li>
  <li><a href="http://www.uniprot.org/taxonomy/" title="Taxonomy">Taxonomy</a></li>
  <li><a href="http://www.reactome.org/" title="Reactome">Reactome</a></li>
  <li><a href="http://www.genome.jp/kegg/" title="Kyoto Encyclopedia of Genes and Genomes (KEGG)">KEGG</a></li>
  <li><a href="https://www.uniprot.org/" title="UniProt">UniProt</a></li>
  <li>...</li>
</ul>


<h2>Qualifiers in the annotation</h2>

<p>
  The qualifiers in the annotation of models, represent the relationship between the model element and its annotation. The list of all existing qualifiers, their definition, and how they can be used within a SBML model file, can be found on the <a href="http://biomodels.net/qualifiers/" title="BioModels.net qualifiers">BioModels.net website</a>.
</p>


<h2>How are annotations created and edited?</h2>

<p>
  The search for the biological entities which are represented by the model components is performed manually. The information is coming from the model, the associated publication, and any notes or correspondence from the modeller(s). Once the relevant biological entities have been found, their formal identification is obtained from key word searches in the appropriate external resources.
</p>
<p>
  Once the relevant biological entities have been identified, several tools are available to store this information in the SBML model files. A few of them are listed below, but this is far from exhaustive. Please refer to the <a href="http://sbml.org/SBML_Software_Guide" title="SBML Software Guide" class="external">SBML Software Guide</a> for a more complete list.
</p>
<p>
  <a href="https://www.biomodels.org/" title="BioModels">BioModels</a> provides an annotation interface (<span class="mom_figure_ref">Figure 2</span>). Access to this feature is currently restricted to its team of curators. The appropriate qualifiers and external data resource names are selected from a pull down menu. The entity identifier has to be typed in. The interface makes use of Ajax technologies in order to provide a dynamical way to annotate large models. The system automatically and transparently creates the annotation blocks (including the qualifiers and MIRIAM URIs, of the form shown in <span class="mom_figure_ref">Figure 1</span>) and includes it in the SBML file.
</p>

<div class="doc_img">
  <a href="https://www.biomodels.org/static-assets/GRAPHICS/BioModels_Database_annotation-interface.png" title="Click to view the full size image...">
    <img src="https://www.biomodels.org/static-assets/GRAPHICS/BioModels_Database_annotation-interface_small.png" alt="Annotation interface of BioModels Database" title="Annotation interface of BioModels Database (Click to view the full size image)" />
  </a>
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 2</span> Annotation Interface of BioModels Database.
  </div>
</div>

<p>
  Model annotation can also be done using <a href="http://www.ebi.ac.uk/compneur-srv/SBMLeditor.html" title="SBMLeditor">SBMLeditor</a> (<span class="mom_figure_ref">Figure 3</span>). Similarly to BioModels Database's annotation interface, the software takes care of generating the whole annotation blocks based on the information the user entered in the provided fields.
</p>

<div class="doc_img">
  <a href="https://www.biomodels.org/static-assets/GRAPHICS/SBMLeditor_annotation-interface.png" title="Click to view the full size image...">
    <img src="https://www.biomodels.org/static-assets/GRAPHICS/SBMLeditor_annotation-interface_small.png" alt="Annotation interface of SBMLeditor" title="Annotation interface of SBMLeditor (Click to view the full size image)" />
  </a>
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 3</span> Annotation interface of SBMLeditor.
  </div>
</div>

<p>
  <a href="http://www.copasi.org/" title="COPASI: biochemical network simulator" class="external">COPASI</a> also provides some annotation features (<span class="mom_figure_ref">Figure 4</span>). It allows users to create and edit all possible annotation. Further information can be found from the <a href="http://www.copasi.org/tiki-index.php?page=OD.Annotating.Models.and.Model.Elements&amp;structure=OD" title="Annotating Models and Model Elements with COPASI" class="external">COPASI documentation</a>.
</p>

<div class="doc_img">
  <a href="https://www.biomodels.org/static-assets/GRAPHICS/COPASI_annotation-interface.png" title="Click to view the full size image...">
    <img src="https://www.biomodels.org/static-assets/GRAPHICS/COPASI_annotation-interface_small.png" alt="Annotation interface of COPASI" title="Annotation interface of COPASI (Click to view the full size image)" />
  </a>
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 4</span> Annotation interface of COPASI.
  </div>
</div>

<p>
  <a href="http://celldesigner.org/" title="CellDesigner">CellDesigner</a> provides features where notes and MIRIAM annotations can be added and edited (<span class="mom_figure_ref">Figure 5</span>). Further information can be found from the <a href="http://celldesigner.org/documents.html" title="CellDesigner Documentation">CellDesigner documentation</a>.
</p>

<div class="doc_img">
  <a href="https://www.biomodels.org/static-assets/GRAPHICS/CellDesigner_annotation-interface.png" title="Click to view the full size image...">
    <img src="https://www.biomodels.org/static-assets/GRAPHICS/CellDesigner_annotation-interface_small.png" alt="Annotation interface of CellDesigner" title="Annotation interface of CellDesigner (Click to view the full size image)" />
  </a>
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 5</span> Annotation interface of CellDesigner.
  </div>
</div>

<p>
  <a href="http://semanticsbml.org/" title="semanticSBML" class="external">semanticSBML</a> provides a very easy-to-use annotation interface. The interface includes a search feature to find appropriate resource identifiers to annotate model elements (<span class="mom_figure_ref">Figure 6</span>).
</p>

<div class="doc_img">
  <a href="https://www.biomodels.org/static-assets/GRAPHICS/SemanticSBML_annotation-interface.png" title="Click to view the full size image...">
    <img src="https://www.biomodels.org/static-assets/GRAPHICS/SemanticSBML_annotation-interface_small.png" alt="Annotation interface of semanticSBML" title="Annotation interface of semanticSBML (Click to view the full size image)" />
  </a>
  <div class="doc_img_legend">
    <span class="mom_figure_ref">Figure 6</span> Annotation interface of semanticSBML.
  </div>
</div>

<p>
  One another tool worth mentioning is <a href="http://saint.ncl.ac.uk/" title="SBML Model Annotator: Annotation via Data Integration" class="external">Saint</a>, which provides an automated SBML annotation environment. Further information can be found on its <a href="http://saint-annotate.sourceforge.net/" title="About Saint: Annotation of Computational Biological Models" class="external">SourceForge project</a>.
</p>


<h2>Annotation guidelines (for curators)</h2>

<p>
  More <a href="${createLink(controller: 'jummp', action: 'annotationTips')}" title="Annotation guidelines">detailed guidelines for model annotation</a>, with specific examples, are available; although those have been primarily designed for our curators.
</p>

<br />

</body>
<content tag="curator-zone">
    selected
</content>
</html>
