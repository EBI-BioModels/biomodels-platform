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
<%@ page import="grails.converters.JSON; java.text.DateFormat"%>
<%@ page import="net.biomodels.jummp.core.model.ModelState"%>
<%@ page import="net.biomodels.jummp.qcinfo.*"%>
<%
    JSON tagsJSON = bmTags as grails.converters.JSON
%>
<head xmlns="http://www.w3.org/1999/html">
    <title>${revision.name} | BioModels</title>
    <script type="text/javascript">
        $(document).ready(function() {
            $('.model-tags-select2').select2({
                placeholder: "Type here to search a tag",
                tags: false,
                multiple: true
            });
            $(".header").first().next().slideDown(500);

            $("#expand-all").click(function(){
                $(".header").next().slideDown(500);
            });
            $("#collapse-all").click(function(){
                $(".header").next().slideUp(500);
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
    <link rel="canonical" href="https://www.ebi.ac.uk/biomodels/${revision.modelIdentifier()}"/>
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
            $("#tabs ul li a").click(function (e) {
                var anchor=$(this).attr('href');
                var anchorClass = $(this).attr('class');
                var anchorId = $(this).attr('id');
                if (anchorClass=="versionDownload" || anchorClass=="publicationLink") {
                    $.jummp.openPage(anchor);
                } else if (anchorClass == "ui-tabs-anchor") {
                    e.preventDefault();
                    location.hash = anchor;
                    var toggleHelp=0;
                    if (helpHidden!=1) {
                        toggleHelp=1;
                    }
                    if (toggleHelp==1) {
                        hideHelp();
                    }
                    window.scrollTo(0, 0);
                    if (toggleHelp==1) {
                        showHelp();
                    }
                    if (anchor.startsWith("#mdl") && anchor.length == 14) {
                        hideQuestionMark();
                    }
                }
            });
            $( "#dialog-confirm" ).dialog({
                    resizable: false,
                    autoOpen: false,
                    height: 200,
                    width: 440,
                    modal: true,
                    buttons: {
                        "Confirm Delete": function() {
                            $.jummp.openPage('${g.createLink(controller: 'model', action: 'delete',
                            id: revision.modelIdentifier())}');
                            $( this ).dialog( "close" );
                        },
                        Cancel: function() {
                            $( this ).dialog( "close" );
                        }
                    }
            });

        });

        $(document).ready(function() {
            // OmicsDI Service for showing rosette
            let isAvailable = checkModelAvailability("${revision.modelIdentifier()}");
            if (isAvailable) {
                createRosette("${revision.modelIdentifier()}");
                $('.ext-rsc-text').attr('style', 'display:inline-block;');
            }

            // Handler for .ready() called.
            $('#confirm-model-consistency-check').dialog({
                resizable: false,
                autoOpen: false,
                height: 250,
                width: 500,
                modal: true,
                buttons: {
                    Confirm: function() {
                        var url = "${g.createLink(controller: 'sbml',
                                        action: 'checkConsistency',
                                        id: revision.identifier())}";
                        $.jummp.openPage(url);
                        $(this).dialog("close");
                    },
                    Cancel: function() {
                        $(this).dialog("close");
                    }
                }
            });
            $('#confirm-model-conversion').dialog({
                resizable: false,
                autoOpen: false,
                height: 250,
                width: 500,
                modal: true,
                buttons: {
                    Confirm: function() {
                        var url = "${g.createLink(controller: 'conversion', action: 'convert')}";
                        url += "?id=${revision.model.submissionId}&revisionId=${revision.revisionNumber}"
                        $.jummp.openPage(url);
                        $(this).dialog("close");
                    },
                    Cancel: function() {
                        $(this).dialog("close");
                    }
                }
            });

            $('#confirm-model-publish').dialog({
                resizable: false,
                autoOpen: false,
                height: 250,
                width: 500,
                modal: true,
                buttons: {
                    Confirm: function() {
                        $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'publish', id: revision.identifier() )}");
                        $( this ).dialog( "close" );
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
            $('#confirm-model-notify').dialog({
                resizable: false,
                autoOpen: false,
                height: 250,
                width: 525,
                modal: true,
                buttons: {
                    Confirm: function() {
                        $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'submitForPublication', id: revision.identifier() )}");
                        $( this ).dialog( "close" );
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                    }
                }
            });

            $('#warn-publication-details').dialog({
                resizable: false,
                autoOpen: false,
                height: 540,
                width: 895,
                modal: true,
                buttons: [
                    {
                        id: "btnWarningDialogAction",
                        text: "Close",
                        click: function() {
                            doProceedOrLeave($(this));
                        }
                    }
                ]
            });

            $("body").append("<div id='modelToolbar' class='collapsibleContainer' title='Model Toolbar'>" +
                "<button title='Expand Toolbar' data-showing='0' id='panelToggle'>Expand</button></div>	");
            $("#buttonContainer").prependTo("#modelToolbar");
            var panelToggle = $("#panelToggle");
            panelToggle.click(function (evt){
                displayToolbar(panelToggle.data("showing") === '0', true);
            });
            $( "#download" ).button({
                    text:false,
                    icons: {
                        primary:"ui-icon-arrowthickstop-1-s"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $("#ask-reviewer-account").button({
                    text:false,
                    icons: {
                        primary:"ui-icon-circle-check"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $("#manage-contributors").button({
                    text:false,
                    icons: {
                        primary:"ui-icon-contact"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $( "#peer-review" ).button({
                text:false,
                icons: {
                    primary:"ui-icon-signal-diag"
                }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $( "#update" ).button({
                    text:false,
                    icons: {
                        primary:"ui-icon-refresh"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $( "#delete" ).button({
                    text:false,
                    icons: {
                        primary:"ui-icon-trash"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $( "#publish" ).button({
                    text:false,
                    icons: {
                        primary:"ui-icon-unlocked"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $( "#share" ).button({
                    text:false,
                    icons: {
                        primary:"ui-icon-person"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            /*$("#annotate").button({
                text: false,
                icons: {
                    primary: "ui-icon-tag"
                }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });*/
            $("#checkConsistency").button({
                text: false,
                icons: {
                    primary: "ui-icon-check"
                }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $("#certify").button({
                text: false,
                icons: {
                    primary: "ui-icon-star"
                }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            $("#convert").button({
                text: false,
                icons: {
                    primary: "ui-icon-transferthick-e-w"
                }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });

            panelToggle.button({
                    text:false,
                    icons: {
                        primary: "ui-icon-circle-arrow-e"
                    }
            }).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px', 'float':'right'  });

            $("#curation_state_change").on('change', function () {
                var curationState = this.value;
                $.ajax({
                    type: "PUT",
                    url: $.jummp.createLink("model", "updateCurationState"),
                    cache: false,
                    dataType: 'json',
                    headers: {
                        "Content-Type": "application/json"
                    },
                    data: JSON.stringify({
                        curationState: curationState,
                        modelId: "${revision.model.submissionId}",
                        revisionNumber: "${revision.revisionNumber}"
                    }),
                    beforeSend: function() {
                        toastr.info('Updating curation status...');
                    },
                    error: function(jqXHR) {
                        toastr.clear();
                        toastr.error(jqXHR.responseText.message);
                    },
                    success: function(response) {
                        toastr.clear();
                        toastr.success(response.message);
                        // Updating the curation state feature allows us to change
                        // back and forth non-curated and curated without any problem. On top of that,
                        // the update procedure considers the situation where the curation state of
                        // the public model has been changed to curated. We need to generate
                        // the publication identifier for such a model. Hence the page is only refreshed if
                        // we are changing the curation state of the non-curated public model from non-curated to
                        // curated. The page will be redirected to itself with the newly-created publication
                        // identifier that has been assigned to the model.
                        var publicationId = response.publicationId;
                        var criteria = publicationId !== null;
                        if (criteria) {
                            var message = response.message;
                            message += ". Please wait a few seconds while the web page is being refreshed.";
                            $('.flashNotificationDiv').html(message).show();
                            var modelDisplayPage = $.jummp.createURI(publicationId);
                            <%-- force a redirect if a publication identifier was generated  --%>
                            <%-- Using setTimeout to fresh the web page with the newly-created publication identifier
                             after 5 seconds. --%>
                            setTimeout(function () {
                                $.jummp.openPage(modelDisplayPage);
                            }, 5000);
                        }
                    }
                });
            });
        });
        function setReactomeId(reactomeId) {
            this.reactomeId = reactomeId;
        }
        function openDialogBox() {
            $("#dialog").dialog("open");
        }
        function displayToolbar(show, firstTime) {
            if (show) {
                $("#panelToggle").data("showing", '1');
                $(".buttonLabel").show();
                $("#modelToolbar").width("110px");
                $("#panelToggle").button("option", {
                    icons: { primary: "ui-icon-circle-arrow-w" }
                });
                $(".toolbutton").button("option", "text", true);
                $(".toolbutton").css({ width: '110px', 'padding-top': '10px', 'padding-bottom': '10px' });
            } else {
                $("#panelToggle").data("showing", '0');
                $(".buttonLabel").hide();
                $("#modelToolbar").width("45px");
                $(".toolbutton").button("option", "text", false);
                if (firstTime) {
                    $("#panelToggle").button("option", {
                        icons: { primary: "ui-icon-circle-arrow-e" }
                    });
                }
                $(".toolbutton").css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
            }
        }

        $(function () {
            <sec:ifLoggedIn>
                displayToolbar(true, true);
            </sec:ifLoggedIn>
            <sec:ifNotLoggedIn>
                displayToolbar(false, true);
            </sec:ifNotLoggedIn>
            // displayToolbar(true, true);
        });
        var size = {
            width: window.innerWidth || document.body.clientWidth,
            height: window.innerHeight || document.body.clientHeight
        };

            var global_diagram;
            var reactomeId ="";
            var base_height = Math.floor(size.height/2);
            var base_width = Math.floor(size.width/2);
            var REACTOME_HEIGHT = base_height;
            var REACTOME_WEIGHT = base_width;
            var DIALOG_WEIGHT = base_width+100;
            var DIALOG_HEIGHT = base_height+150;

            $(document).ready(function () {
                $("#dialog").dialog({
                    width: DIALOG_WEIGHT,
                    height: DIALOG_HEIGHT,
                    modal: true,
                    open: function( event, ui ) {
                        global_diagram.resize(REACTOME_WEIGHT,REACTOME_HEIGHT);
                        global_diagram.resetSelection();
                        global_diagram.selectItem(reactomeId);
                    },
                    autoOpen: false
                });

                $("#opener").on("change", function () {
                    setReactomeId(this.value);
                    if(reactomeId) {
                        loadDiagram();
                        openDialogBox();
                    }
                });
            });

            //Creating the Reactome Diagram widget
            //Take into account a proxy needs to be set up in your server side pointing to www.reactome.org
            function loadDiagram() {
                var diagram = Reactome.Diagram.create({
                    "placeHolder": "diagramHolder",
                    "width": REACTOME_WEIGHT,
                    "height": REACTOME_HEIGHT
                });
                diagram.loadDiagram(reactomeId);

                // store this in a global variable so we can call resetSelection() from
                // the callback for opening the Reactome popup. Calling it here results
                // in a popup window with an invisible pathway, even though the widget
                // control buttons are rendered just fine.
                //Adding different listeners
                global_diagram = diagram;

                diagram.onObjectHovered(function (hovered) {
                    console.info("Hovered ", hovered);
                });

                diagram.onObjectSelected(function (selected) {
                    console.info("Selected ", selected);
                });
            }
    </script>
    <g:layoutHead/>
</head>
<body>
        <!-- Render the model toolbox -->
        <div id="buttonContainer" style="display:inline">
            <ul id='toolbarList'>
                <li>
                <button class='toolbutton' id="download"
                    onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                    action: 'download', id: revision.identifier())}')">Download</button></li>
                <g:if test="${canUpdate}">
                    <li>
                    <button class='toolbutton' id="update"
                            onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                            action: 'update',
                            id: revision.modelIdentifier())}')">Update</button>
                    </li>
                </g:if>
                <g:if test="${canDelete}">
                    <div id="dialog-confirm" title="Confirm Delete" style="display:none;">
                        <p>Are you sure you want to delete the model?</p>
                    </div>
                    <li>
                    <button class='toolbutton' id="delete"
                            onclick='return $( "#dialog-confirm" ).dialog( "open" );'>Delete</button>
                    </li>
                </g:if>
                <g:if test="${canSubmitForPublication}">
                    <% def dialog_id %>
                    <g:if test="${revision.model.publication}">
                        <div id="confirm-model-notify"
                             title="<jummp:renderSubmitForPublicationConfirmDialogTitle/>"
                             style="display:none;">
                            <p><jummp:renderSubmitForPublicationConfirmDialogMessage/></p>
                        </div>
                        <% dialog_id = "confirm-model-notify" %>
                    </g:if>
                    <g:else>
                        <div id="warn-publication-details"
                             title="<jummp:renderSubmitForPublicationWarningDialogTitle/>" style="display: none">
                            <p><jummp:renderSubmitForPublicationWarningDialogMessage/></p>
                        </div>
                        <% dialog_id = "warn-publication-details" %>
                    </g:else>
                    <li>
                        <button class='toolbutton' id="peer-review"
                                title="Submit for publication"
                                onclick='return $("#${dialog_id}").dialog("open");'>
                            Publish</button></li>
                </g:if>
                <g:if test="${showPublishOption}">
                    <div id="confirm-model-publish" title="You are about to publish this model version"
                         style="display:none;">
                        <p>Make this version of the model visible to anyone without logging in?</p>
                    </div>
                    <li>
                    <button class='toolbutton' id="publish"
                            onclick="return $( '#confirm-model-publish' ).dialog( 'open' );">Publish</button>
                    </li>
                </g:if>
                <g:if test="${canShare}">
                    <li>
                    <button class='toolbutton' id="share"
                            onclick="return $.jummp.openPage('${g.createLink(controller: 'model',
                            action: 'share', id: revision.identifier())}')">Share</button>
                    </li>
                </g:if>
                %{--<g:if test="${canUpdate}">
                    <li>
                        <button class='toolbutton' id='annotate'
                                onclick="return $.jummp.openPage('${g.createLink(controller: 'annotation',
                                action: 'edit',
                                id: revision.modelIdentifier())}')">Annotate</button>
                    </li>
                </g:if>--}%
                <g:if test="${canCertify}">
                    <li>
                        <button class='toolbutton' id="certify"
                                onclick="return $.jummp.openPage('${g.createLink(controller: 'qcInfo',
                                action: 'edit',
                        id: revision.modelIdentifier())}')">Certify</button>
                    </li>
                </g:if>
                <g:if test="${canCheckConsistency}">
                    <div id="confirm-model-consistency-check" title="Model consistency check" style="display:none;">
                        <p>Checking model consistency uses an online validator. This might take time for uploading and validating the model. Do you want to proceed the validation?</p>
                    </div>
                    <li>
                        <button id="checkConsistency"
                                class="toolbutton"
                                title="Check consistency"
                                onclick="return $('#confirm-model-consistency-check').dialog('open');">
                            Check
                        </button>
                    </li>
                </g:if>
                <g:if test="${hasCuratorRole && supportedForConversion}">
                    <div id="confirm-model-conversion" title="Model Conversion" style="display:none;">
                        <p>Exporting this model to other formats uses an online service. This might take time for
                        uploading and exporting the model. Do you want to proceed the model conversion?</p>
                    </div>
                    <li>
                        <button id="convert"
                                class="toolbutton"
                                title="Convert This Model To The Other Formats"
                                onclick="return $('#confirm-model-conversion').dialog('open');">
                            Convert
                        </button>
                    </li>
                </g:if>
                <g:if test="${canAskReviewerAccount}">
                    <li>
                        <button class='toolbutton' id="ask-reviewer-account"
                                title="Click on this button to open a reviewer account for this model"
                                onclick="return $.jummp.openPage('${g.createLink(controller: 'jummp',
                            action: 'createReviewerAccount',
                            id: revision.modelIdentifier())}')">Reviewer</button>
                    </li>
                </g:if>
                <g:if test="${canAskReviewerAccount}"> <!-- canAddContributor is the same canAskReviewerAccount -->
                    <li>
                        <button class='toolbutton' id="manage-contributors"
                                title="Click on this button to manage the list of contributors of your model"
                                onclick="return $.jummp.openPage('${g.createLink(controller: 'jummp',
                            action: 'contributors',
                            id: revision.identifier())}')">Contributors</button>
                    </li>
                </g:if>
            </ul>
        </div>

        <!-- Render the main content of the model display page -->
        <div class="ebiLayout_reduceWidth">
            <!-- Show warning messages: archived models, old versions, etc. -->
            <g:if test="${revision.model.deleted}">
                <div class='PermanentMessage'>
                    This is an archived model.
                </div>
            </g:if>
            <g:if test="${oldVersion}">
                <div class='PermanentMessage'>
                    You are viewing a version of a model that has been updated.
                    To access the latest version, and a more detailed display please
                    go <a href="${createLink(controller: "model", action: "show", id:
                        revision.modelIdentifier())}">here</a>.
                </div>
            </g:if>

            <!-- Show model revision name, model of the month, icons, flags, Reactome connected pathways, etc. -->
            <div id="topBar">
                <div class="message" style="display: block"></div>

                <div style="float:left;width:75%;">
                    <h2>${revision.name}</h2>
                    <biomd:renderModelOfMonth modelId="${revision.model.id}" />
                </div>

                <div style="float:right;margin-top:10px;">
                    <g:if test="${!flags.empty}">
                        <biomd:renderModelFlags flags="${flags}"/>
                    </g:if>
                    <g:if test="${revision.qcInfo != null}">
                        <jummp:renderStarLevels flag="${revision.qcInfo.flag}" />
                    </g:if>
                    <span>&nbsp;</span>
                    <g:if test="${revision.state==ModelState.PUBLISHED}">
                        <img style="float:right;margin-top:0;" title="This version of the model is public"
                             alt="public model"
                             src="${serverURL}/images/unlock.png"/>
                    </g:if>
                    <g:else>
                        <img style="float:right;margin-top:0;" title="This version of the model is unpublished"
                             alt="unpublished model"
                             src="${serverURL}/images/lock.png"/>
                    </g:else>
                </div>

                <g:if test="${reactomeIds}">
                <div style="margin-right: 50%;">
                    <g:select name="reactome_pathways"
                              id="opener"
                              onchange="setReactomeId(this.value);"
                              from="${reactomeIds}"
                              noSelection="['':'Choose Reactome Pathway']"
                              optionKey = "${{null != it && !((String)it).isEmpty()?((String)it).split('\\|')[1]:((String)it).split('\\|')[0]}}"
                              optionValue="${{((String)it).split('\\|')[0]}}" />
                </div>
                </g:if>
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
		            <g:if test="${curationNotes != null || hasCuratorRole}">
                    <li><a href='#Curation'>Curation</a></li></g:if>
                    </ul>

                    <!-- Overview tab -->
                    <div id="Overview" class="row">
                        <div class="small-12 medium-8 large-8 columns">
                            <div class="row">
                                <div class="small-12 medium-2 large-2 columns">
                                    <span class="overview-tab-attribute">Model Identifier</span>
                                </div>
                                <div class="small-12 medium-10 large-10 columns">
                                    ${revision.modelIdentifier()}
                                </div>
                            </div>
                            <div class="row">
                                <div class="small-12 medium-2 large-2 columns">
                                    <jummp:displayModelDescriptionLabel>
                                        <span class="overview-tab-attribute">${description}</span>
                                    </jummp:displayModelDescriptionLabel>
                                </div>
                                <div class="small-12 medium-10 large-10 columns">
                                    %{--<a class="descriptionToggle" title="Click to see more">
                                        <span>Click here to collapse/expand the description
                                            <img style="width:12px;margin:2px;float:none"
                                                 src="${serverURL}/images/expand.png"/>
                                        </span>
                                    </a>--}%
                                    <div id="description">
                                        ${raw(revision.description)}
                                    </div>
                                </div>
                                <g:javascript>
                                    /*$('.descriptionToggle').click(function() {
                                        $('#description').slideToggle('fast');
                                    });*/
                                </g:javascript>
                            </div>
                            <div class="row">
                                <div class="small-12 medium-2 large-2 columns">
                                    <span class="overview-tab-attribute"><g:message code="model.model.format"/></span>
                                </div>
                                <div class="small-12 medium-10 large-10 columns">
                                    ${revision.format.name}
                                        ${revision.format.formatVersion!="*"?"(${revision.format.formatVersion})":""}
                                </div>
                            </div>
                            <g:render  model="[revision: revision]" template="/templates/renderPublication" />
                        </div>

                        <div class="small-12 medium-4 large-4 columns">
                            <div class="rounded-header"><h4 style="color: #ffffee">Metadata information</h4></div>
                            <g:pageProperty name="page.genericAnnotations"/>
                            <g:if test="${curationState}">
                            <biomd:insertSeparator/>
                            <div class='row'>
                                <div class="small-12 medium-6 large-4 columns">Curation status</div>
                                <div class="small-12 medium-6 large-8 columns">
                                <g:if test="${canUpdate && hasCuratorRole && curationNotes != null}">
                                    <select id="curation_state_change">
                                        <g:each in="${possibleCurationStates}" var="possibleCurationState">
                                            <g:if test="${possibleCurationState.equals(curationState)}">
                                                <option value="${possibleCurationState}" selected>
                                                    <jummp:camelCase message="${possibleCurationState}" />
                                                </option>
                                            </g:if>
                                            <g:else>
                                                <option value="${possibleCurationState}">
                                                    <jummp:camelCase message="${possibleCurationState}" />
                                                </option>
                                            </g:else>
                                        </g:each>
                                    </select>
                                </g:if>
                                <g:else>
                                    <jummp:camelCase message="${curationState}" />
                                </g:else>
                                </div>
                            </div></g:if>
                            <g:if test="${modellingApproaches}">
                            <biomd:insertSeparator/>
                            <div class='row'>
                                <div class="small-12 medium-6 large-4 columns">Modelling approach(es)</div>
                                <div class="small-12 medium-6 large-8 columns">
                                    <biomd:renderModellingApproaches modellingApproaches="${modellingApproaches}"/>
                                </div>
                            </div></g:if>
                            <g:if test="${originalModels}">
                            <biomd:insertSeparator/>
                            <div class='row'>
                                <div class="small-12 medium-6 large-4 columns">Original model(s)</div>
                                <div class="small-12 medium-6 large-8 columns">
                                    <biomd:renderOriginalModels sources="${originalModels}"/></div>
                            </div></g:if>
                            <!-- Show all tags assigned to the model -->
                            <biomd:insertSeparator/>
                            <g:if test="${canUpdate && hasCuratorRole}">
                                <biomd:showEditableTags bmTags="${bmTags}"/>
                            </g:if>
                            <g:else>
                                <biomd:showTags bmTags="${bmTags}"/>
                            </g:else>
                            <!-- Render a disclaimer if the model has been published without a publicly available manuscript -->
                            <biomd:displayDisclaimer revision="${revision}"/>
                            <biomd:insertSectionSeparator/>
                            <div class="rounded-header"><h4 style="color: #ffffee">Connected external resources</h4></div>
                            <div class='row'>
                                <div class="small-12 medium-3 large-3 columns" id="rosette-holder">
                                    <!-- This empty holder is used to show the model rosette rendered
                                    automatically in omicsdi.service.js via the function createRosette() called
                                    from the ready block of this page -->
                                </div>
                                <div class="small-12 medium-9 large-9 columns">
                                    <p class="ext-rsc-text" style="display: none">OmicsDI Impact Metrics</p>
                                </div>
                            </div>
                            <g:if test="${hrefLinkToNewtEditor}">
                            <biomd:renderLinkToNewtEditor serverURL="${serverURL}"
                                                          hrefLinkToNewtEditor="${hrefLinkToNewtEditor}"/>
                            </g:if>

                            %{--<div class='row'>
                                <div class="medium-3 columns">Validation Status</div>
                                <div class="medium-9 columns">${validationLevel}</div>
                            </div>
                            <div class='row'>
                                <div class="medium-3 columns">Certification Comment</div>
                                <div class="medium-9 columns">${certComment}</div>
                            </div>--}%
                        </div>
                    </div>

                    <!-- Files tab -->
                    <div id="Files" class="row">
                        <% Map model = ["repoFiles": repoFiles] %>
                        <g:render template="/templates/biomodels/modelDisplay/tabFiles"
                                  model="${model}" />
                    </div>

                    <!-- History tab -->
                    <div id="History">
                        <% DateFormat dateFormat = DateFormat.getDateTimeInstance(); %>
                        <ul>
                            <li>Model originally submitted by : ${revision.model.submitter}</li>
                            <li>Submitted: ${dateFormat.format(allRevs.first().uploadDate)}</li>
                            <li>Last Modified: ${dateFormat.format(allRevs.last().uploadDate)}</li>
                        </ul>
                        <h5>Revisions</h5>
                        <ul>
                            <g:each status="i" var="rv" in="${allRevs.sort{a,b -> a.revisionNumber > b.revisionNumber ? -1 : 1}}">
                                <li style="${revision.id == rv.id ?"background-color:#FFFFCC;":""}margin-top:5px">
                                    Version: ${rv.revisionNumber}
                                    <g:if test="${rv.state==ModelState.PUBLISHED}">
                                            <img style="width:12px;margin:2px;float:none;"
                                                 title="This version of the model is public" alt="public model"
                                                 src="${serverURL}/images/unlock.png"/>
                                    </g:if>
                                    <g:else>
                                            <img style="width:12px;margin:2px;float:none;"
                                                 title="This version of the model is unpublished" alt="unpublished model"
                                                 src="${serverURL}/images/lock.png"/>
                                    </g:else>
                                    <g:if test="${revision.id!=rv.id}">
                                        <a class="versionDownload" title="go to version ${rv.revisionNumber}"
                                           href="${g.createLink(controller: 'model', action: 'show', id: rv.identifier())}">
                                            <img style="width:12px;margin:2px;float:none"
                                                 src="${serverURL}/images/external_link.png"/>
                                        </a>
                                    </g:if>
                                            <a class="versionDownload" title="download"
                                               href="${g.createLink(controller: 'model', action: 'download', id: rv.identifier())}">
                                                <img alt="Download this version" style="width:15px;float:none"
                                                     src="${serverURL}/images/download.png"/>
                                            </a>
                                        <ul>
                                            <li>Submitted on: ${dateFormat.format(rv.uploadDate)}</li>
                                            <li>Submitted by: ${rv.owner}</li>
                                            <li>With comment: ${rv.comment}</li>
                                        </ul>
                                </li>
                            </g:each>
                        </ul>
                        <g:if test="${allRevs.size() > 1}">
                            <p style="font-style: italic; font-size: smaller">(*) You might be seeing discontinuous
                                revisions as only public revisions are displayed here. Any private revisions
                                <img title="unpublished model revision" alt="unpublished model revision"
                                     src="${serverURL}/images/lock.png"/>
                                 of this model will only be shown to the submitter and their collaborators.</p>
                        </g:if>
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
                    <g:if test="${curationNotes != null || hasCuratorRole}">
                        <biomd:renderCurationNotesTab curationNotes="${curationNotes}"
                                                      model="${revision.modelIdentifier()}"
                                                      modelName="${revision.name}"
                                                      hasCuratorRole="${hasCuratorRole}"/>
                    </g:if>
                </div>
            </div>
        </div>
    <script>
        $('#btnSaveTags').on("click", function (event) {
            "use strict";
            event.preventDefault();
            let updatedTags = getDataFromSelect2();
            if (initialTags.length === updatedTags.length && !initialTags.length) {
                toastr.clear();
                toastr.warning("No label applied to the model. Alternatively, select at least one label from the list.");
            } else {
                $.ajax({
                    type: "POST",
                    url: $.jummp.createLink("modelTag", "saveModelTag"),
                    cache: true,
                    async: true,
                    processData: true,
                    dataType: "json",
                    data: {
                        modelId: "${revision.model.submissionId}",
                        tags: buildTagSet()
                    },
                    beforeSend: function () {
                        let msg = "";
                        if (updatedTags.length === 0) {
                            msg = "No tags applied to the model.";
                        } else {
                            msg = "The labels applied to the model are being saved into our database. Please wait...";
                        }
                        toastr.clear();
                        toastr.info(msg);
                    }
                }).done(function (data, txtStatus, jqXHR) {
                        var msg = data.message;
                        var statusCode = data.status
                        toastr.clear();
                        if (statusCode === 200) {
                            toastr.success(msg);
                            // update select2 data

                        } else if (statusCode === 400) {
                            toastr.error(msg);
                        } else if (statusCode === 422) {
                            toastr.warn(msg);
                        } else {
                            toastr.error("Cannot determine the reason for the unexpected error");
                        }
                        initialTags = updatedTags;

                }).fail(function (jqXHR, status, errorThrown) {
                        var msg = jqXHR.statusText;
                        toastr.clear();
                        toastr.error(msg);
                });
            }
        });

        function buildTagSet() {
            let data = $('.model-tags-select2').select2('data');
            let updatedTags = [];
            $.each(data, function (index, value) {
                updatedTags.push({"id": value.id, "name": value.text});
            });
            var jsonStr = JSON.stringify(updatedTags, ['id', 'name']);
            return jsonStr;
        }

        function getDataFromSelect2() {
            let data = $('.model-tags-select2').select2('data');
            let updatedTags = [];
            $.each(data, function (index, value) {
                updatedTags.push(value.text);
            });
            return updatedTags;
        }
        $('#chkPublishWithoutPublication').change(function () {
            let whichButton = '';
            if (this.checked) {
                whichButton = '<span class="ui-button-text">Proceed</span>';
            } else {
                whichButton = '<span class="ui-button-text">Close</span>';
            }
            $('#btnWarningDialogAction').html(whichButton);
        });

        function doProceedOrLeave(pointer) {
            let actionButton = $('#btnWarningDialogAction').text();
            if (actionButton === "Proceed") {
                $.jummp.openPage("${g.createLink(controller: 'model',
                        action: 'submitForPublication', id: revision.identifier())}");
            }
            pointer.dialog("close");
        }
    </script>
</body>
<content tag="contexthelp">
        display
</content>
</g:applyLayout>
<div id="dialog" title="Reactome pathway">
    <div id="diagramHolder"></div>
</div>
