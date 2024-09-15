<%--
  Created by IntelliJ IDEA.
  User: tnguyen
  Date: 13/09/2024
  Time: 22:15
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<%@ page import="net.biomodels.jummp.core.constants.BioModels"%>
<%@ page import="net.biomodels.jummp.core.model.ModelState"%>
<meta name="layout" content="${session['branding.style']}/newmain"/>
<html>
<head xmlns="http://www.w3.org/1999/html">
    <title>${revision.name} | BioModels</title>
    <link rel="stylesheet"
          href="${resource(contextPath: serverURL, dir: 'css/biomodels', file: 'model-display.css')}">
    <g:render template="/templates/model/show/loadjs" plugin="jummp-plugin-web-application"/>
    <g:javascript>
        const serverURL = "${serverURL}";
        const titlePage = "${revision.name}" + " | BioModels";
        const rootModelURL = "${revision.url()}"
    </g:javascript>
</head>
<body>
<!-- Show warning messages: archived models, old versions, etc. -->
<div class="warnings">
    <g:render template="/templates/model/show/warning" plugin="jummp-plugin-web-application"/>
</div>
<div id="top-bar">
    <g:render template="/templates/model/show/topbar" plugin="jummp-plugin-web-application"/>
</div>

<div class="row">
<div class="columns large-12 medium-12 small-12">
    <g:render template="/templates/model/show/model-toolbox" plugin="jummp-plugin-web-application"/>
    <div class="w3-bar w3-black">
        <button class="w3-bar-item w3-button tablink w3-red" id="btn-Overview"
                onclick="openTab('Overview')">Overview</button>
        <button class="w3-bar-item w3-button tablink" id="btn-Files"
                onclick="openTab('Files')">Files</button>
        <button class="w3-bar-item w3-button tablink" id="btn-History"
                onclick="openTab('History')">History</button>
        <g:if test="${convertedFilesTC}">
        <button class="w3-bar-item w3-button tablink" id="btn-Exports"
                onclick="openTab('Exports')">Exports</button>
        </g:if>
        <div id="specific-tabs"></div>
        <g:if test="${specificTabs}">
            <g:each in="${specificTabs}" var="tab">
                <button class="w3-bar-item w3-button tablink" id="btn-${tab.key}"
                        onclick='openTab("${tab.key}")'>${tab.key}</button>
            </g:each>
        </g:if>
        <g:if test="${curationNotes != null || hasCuratorRole || canSeeCurationTab }">
        <button class="w3-bar-item w3-button tablink" id="btn-Curation"
                onclick="openTab('Curation')">Curation</button>
        </g:if>
    </div>
    <!-- Overview tab -->
    <div id="Overview" class="w3-container w3-border display-tab">
        <g:render template="/templates/model/show/overview" plugin="jummp-plugin-web-application"/>
    </div>
    <!-- Files tab -->
    <div id="Files" class="w3-container w3-border display-tab" style="display:none">
        <% Map model = ["repoFiles": repoFiles] %>
        <g:render template="/templates/biomodels/modelDisplay/tabFiles" model="${model}" />
    </div>
    <!-- History tab -->
    <div id="History" class="w3-container w3-border display-tab" style="display:none">
        <g:render template="/templates/model/show/history" plugin="jummp-plugin-web-application"/>
    </div>
    <!-- Exports tab -->
    <g:if test="${convertedFilesTC}">
    <div id="Exports" class="w3-container w3-border display-tab" style="display:none">
        <h3>Below are the converted model files where you could download</h3>
        <biomd:renderConvertedFiles convertedFilesTC="${convertedFilesTC}"/>
    </div>
    </g:if>
    <!-- Rendering the contents of the specific tabs -->
    <div id="specific-tabs-content" style="display: none"></div>
    <g:if test="${specificTabs}">
    <g:each in="${specificTabs}" var="tabContent">
        <div id="${tabContent.key}" class="w3-container w3-border display-tab" style="display:none">
            ${tabContent.value}
        </div>
    </g:each></g:if>
    <!-- Curation tab -->
    <g:if test="${curationNotes != null || hasCuratorRole || canSeeCurationTab }">
    <div id="Curation" class="w3-container w3-border display-tab" style="display:none">
        <biomd:renderCurationNotesTab curationNotes="${curationNotes}"
                                      model="${revision.modelIdentifier()}"
                                      modelName="${revision.name}"
                                      canSeeCurationTab="${canSeeCurationTab}"
                                      hasCuratorRole="${hasCuratorRole}"
                                      hasAdminRole="${hasAdminRole}"/>
    </div></g:if>
</div>
</div>

<script>
    $(function() {
        const currentHref = window.location.href;
        let tabName;
        if (currentHref.indexOf("#") > 0) {
            tabName = currentHref.substring(currentHref.indexOf("#") + 1);
            openTab(tabName);
        }
    });

    function openTab(tabName) {
        let i, x, tabLinks;
        x = $(".display-tab");
        for (i = 0; i < x.length; i++) {
            $(x[i]).css({"display": "none"});
        }
        tabLinks = $(".tablink");
        for (i = 0; i < x.length; i++) {
            $(tabLinks[i]).removeClass("w3-red");
        }
        $("#"+tabName).css({"display": "block"})
        $("#btn-"+tabName).addClass("w3-red");

        const currentHref = window.location.href;
        let rootURL = rootModelURL;
        let url;
        if (currentHref.indexOf("#") > 0) {
            rootURL = currentHref.substring(0, currentHref.indexOf("#"));
            url = rootURL + "#" + tabName;
        } else {
            url = window.location.href + "#" + tabName;
        }
        const stateData = "Accessing " + url;
        if (window.history.replaceState) {
            // prevents browser from storing history with each change:
            window.history.replaceState(stateData, titlePage, url);
        }
    }
</script>
</body>
</html>
