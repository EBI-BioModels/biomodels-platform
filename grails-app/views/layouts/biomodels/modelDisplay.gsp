<%--
 Copyright (C) 2010-2021 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.

 Additional permission under GNU Affero GPL version 3 section 7

 If you modify Jummp, or any covered work, by linking or combining it with
 Apache Commons (or a modified version of that library), containing parts
 covered by the terms of Apache License v2.0, the licensors of this
 Program grant you additional permission to convey the resulting work.
 {Corresponding Source for a non-source form of such a combination shall include
 the source code for the parts of Apache Commons used as well as that of
 the covered work.}
--%>

<g:applyLayout name="biomodels/main">
<%@ page import="net.biomodels.jummp.core.constants.BioModels"%>
<%@ page import="grails.converters.JSON; java.text.DateFormat"%>
<%@ page import="net.biomodels.jummp.core.model.ModelState"%>
<%@ page import="net.biomodels.jummp.qcinfo.*"%>
<% JSON tagsJSON = bmTags as JSON %>
<head xmlns="http://www.w3.org/1999/html">
    <title>${revision.name} | BioModels</title>
    <script type="text/javascript">
        $(document).ready(function() {
            $('.model-tags-select2').select2({
                placeholder: "Type here to search a tag",
                tags: false,
                multiple: true
            });
        });
    </script>
    <script type="text/x-mathjax-config">
        MathJax.Hub.Config({
            tex2jax: { inlineMath: [['$','$'],['\\(','\\)']] }
        });
    </script>
    <script type='text/javascript'
            src="${serverURL}/js/MathJax-2.6.1/MathJax.js?config=TeX-AMS-MML_HTMLorMML">
    </script>
    <g:javascript>
        let canUpdate = ${canUpdate};
        // initialTags is the list of tags associated with the model
        // as the page is completely loaded
        let initialTags = [];
        Object.values = function(object) {
            let values = [];
            for(let property in object) {
                values.push(object[property]);
            }
            return values;
        }
        let tagsJSON = Object.values(${tagsJSON});
        if (tagsJSON.length !== 0) {
            $.each(tagsJSON, function (index, value) {
                initialTags.push(value);
            });
        }
    </g:javascript>
    <g:javascript src="syntax/shCore.js"/>
    <g:javascript src="syntax/shBrushMdl.js"/>
    <g:javascript src="syntax/shBrushXml.js"/>
    <g:javascript src="toastr.min.js"/>
    <g:javascript src="jquery.handsontable.full.js"/>
    <script type="text/javascript" src="https://d3js.org/d3.v4.min.js"></script>
    <g:javascript src="biomodels/omicsdi.service.js"/>
    <link rel="alternate" href="https://identifiers.org/biomodels.db/${revision.modelIdentifier()}"/>
    <link rel="alternate" href="https://www.ebi.ac.uk/biomodels-main/${revision.modelIdentifier()}"/>
    <link rel="alternate" href="https://www.ebi.ac.uk/biomodels-main/${revision.modelIdentifier()}"/>
    <link rel="canonical" href="${BioModels.BM_ROOT_URL}/${revision.modelIdentifier()}"/>
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'jquery.handsontable.full.min.css')}"/>
    <link rel="stylesheet" href="${resource(dir: 'css/syntax', file: 'shCore.css')}"/>
    <link rel="stylesheet" href="${resource(dir: 'css/syntax', file: 'shThemeDefault.css')}"/>
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'toastr.min.css')}"/>
    <link rel="stylesheet" href="${resource(dir: 'css', file: 'model-display.css')}"/>
    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/css/select2.min.css"/>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/js/select2.min.js"></script>
    <script  defer="defer" type="text/javascript" language="javascript" src="${reactomeUrl}"></script>
    <script>
        $(function() {
            $( "#tabs" ).tabs({
                fx: { opacity: 'toggle' },
                select: function(event, ui) {
                    jQuery(this).css('height', jQuery(this).height());
                    jQuery(this).css('overflow', 'hidden');
                },
                show: function(event, ui) {
                    jQuery(this).css('height', 'auto');
                    jQuery(this).css('overflow', 'visible');
                    }
            });
            $("#tabs ul li a").on("click", function (e) {
                const anchor = $(this).attr('href');
                const anchorClass = $(this).attr('class');
                const anchorId = $(this).attr('id');
                if (anchorClass === "ui-tabs-anchor") {
                    e.preventDefault();
                    location.hash = anchor;
                    let toggleHelp = 0;
                    if (helpHidden !== 1) {
                        toggleHelp=1;
                    }
                    if (toggleHelp === 1) {
                        hideHelp();
                    }
                    window.scrollTo(0, 0);
                    if (toggleHelp === 1) {
                        showHelp();
                    }
                    if (anchor.startsWith("#mdl") && anchor.length === 14) {
                        hideQuestionMark();
                    }
                }
            });
        });
    </script>
    <g:layoutHead/>
</head>
<body>
    <!-- Render the model toolbox -->
    <div id="buttonContainer" style="display:inline">
        <g:render template="/templates/model/show/model-toolbox-built-with-jqueryui"
                  plugin="jummp-plugin-web-application"/>
    </div>

        <!-- Render the main content of the model display page -->
        <div class="ebiLayout_reduceWidth">
            <!-- Show warning messages: archived models, old versions, etc. -->
            <g:render template="/templates/model/show/warning" plugin="jummp-plugin-web-application"/>

            <!-- Show model revision name, model of the month, icons, flags, Reactome connected pathways, etc. -->
            <div id="topBar">
            <g:render template="/templates/model/show/topbar" plugin="jummp-plugin-web-application"/>
            </div>

            <!-- Render the model tabs -->
            <div id="tablewrapper">
                <div id="tabs">
                    <ul class='modelTabs'>
                    <li><a href="#Overview">Overview</a></li>
                    <li><a href="#Files">Files</a></li>
                    <li><a href="#History">History</a></li>
                    <g:if test="${convertedFilesTC}">
                    <li><a href="#Exports">Exports</a></li></g:if>
                    <!--
                        These specific tabs would be shown based on specific model format. Every tab is deliberately designed
                        for each part/section in the content of model file.
                        For example:
                        SBML: needs to have tabs such as Math Definition, Physical Entities, Parameters, etc.
                        PharmML: needs to have tabs such as Model Definition, Trial Design, Estimation Steps, etc.
                    -->
                    <g:pageProperty name="page.modelspecifictabs" />
                    <!--
                        These specific tabs would be shown based on the presence of data. For example,
                        curation notes do not be included at all the time.
                    -->
		            <g:if test="${ curationNotes != null || hasCuratorRole || canSeeCurationTab }">
                    <li><a href="#Curation">Curation</a></li></g:if>
                    </ul>

                    <!-- Overview tab -->
                    <div id="Overview" >
                    <g:render template="/templates/model/show/overview" plugin="jummp-plugin-web-application"/>
                    </div>

                    <!-- Files tab -->
                    <div id="Files" class="row">
                    <% Map model = ["repoFiles": repoFiles] %>
                    <g:render template="/templates/biomodels/modelDisplay/tabFiles" model="${model}" />
                    </div>

                    <!-- History tab -->
                    <div id="History">
                    <g:render template="/templates/model/show/history" plugin="jummp-plugin-web-application"/>
                    </div>

                    <!-- Exports tab -->
                    <g:if test="${convertedFilesTC}">
                    <div id="Exports">
                        <h3>Below are the converted model files where you could download</h3>
                        <biomd:renderConvertedFiles convertedFilesTC="${convertedFilesTC}"/>
                    </div>
                    </g:if>

                    <!-- Some specific tabs -->
                    <g:pageProperty name="page.modelspecifictabscontent" />

                    <!-- Curation tab -->
                    <g:if test="${curationNotes != null || hasCuratorRole || canSeeCurationTab }">
                    <div id="Curation">
                        <biomd:renderCurationNotesTab curationNotes="${curationNotes}"
                                                      model="${revision.modelIdentifier()}"
                                                      modelName="${revision.name}"
                                                      canSeeCurationTab="${canSeeCurationTab}"
                                                      hasCuratorRole="${hasCuratorRole}"
                                                      hasAdminRole="${hasAdminRole}"/>
                    </div>
                    </g:if>
                </div>
            </div>
        </div>

    <script>
        $(function() {
            <sec:ifLoggedIn>
            displayToolbar(true, true);
            </sec:ifLoggedIn>
            <sec:ifNotLoggedIn>
            displayToolbar(false, true);
            </sec:ifNotLoggedIn>
            // displayToolbar(true, true);
        });
    </script>
    <!-- loading the script to create a link to Reactome's DiagramJs widget when the model is eligible -->
    <g:if test="${reactomeIds}">
    <script src="${resource(dir: 'js/biomodels', file: 'reactome.diagram.viewer.js')}"></script>
    </g:if>
</body>
<content tag="contexthelp">
display
</content>
</g:applyLayout>

