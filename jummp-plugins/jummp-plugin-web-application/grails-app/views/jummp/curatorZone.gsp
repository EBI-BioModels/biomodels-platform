<%--
  Created by IntelliJ IDEA.
  User: tnguyen@ebi.ac.uk
  Date: 02/09/2020
  Time: 15:22
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="${session['branding.style']}/main" />
    <title><g:message code="${titleCode}" default="Curator's Zone"/> | BioModels</title>
</head>

<body>
<h2>Documentation for BioModels' Curators</h2>

<p>
    This is the central place where BioModels' curators can find the guidelines, references and
    procedures used when curating and annotating models.
</p>

<h3>Model annotation</h3>
<ul>
    <li>
        <a href="${createLink(controller: 'jummp', action: 'annotationInfo')}">Annotation Information</a><br/>
        A general introduction to what model annotation is, why models are annotated, how annotation
        is encoded in SBML, and the tools available to create and edit it.
    </li>
    <li>
        <a href="${createLink(controller: 'jummp', action: 'annotationTips')}">Annotation Guidelines</a><br/>
        Detailed guidelines, with worked examples, for selecting accession numbers, choosing qualifiers,
        annotating with SBO terms, and annotating the <tt>model</tt> element, species and reactions.
        Primarily designed for our curators.
    </li>
</ul>

<a name="general-introduction-annotation"></a>
<h3>Further reading</h3>
<p>Additional curation notes are available here:
    <a href="https://drive.google.com/file/d/1JqjcH0T0UTWMuBj-scIMwsyt2z38A0vp/view?usp=sharing">
        General introduction to the annotation of models (Google Drive)</a>.
</p>
</body>
<content tag="curator-zone">
    selected
</content>
</html>
