<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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










<g:applyLayout name="main">
<%@ page import="java.text.DateFormat"%>
<%@ page import="net.biomodels.jummp.core.model.ModelState"%>
<%
    def loadedZips=new HashMap();
    def zipSupported=[:]
%>
<head>
        <title>${revision.name}</title>
        <link rel="stylesheet" href="<g:resource dir="css/jqueryui/smoothness" file="jquery-ui-1.10.3.custom.css"/>" />

    <script type="text/x-mathjax-config">
        MathJax.Hub.Config({
            tex2jax: { inlineMath: [['$','$'],['\\(','\\)']] }
        });
    </script>
    <script type='text/javascript'
            src='http://cdn.mathjax.org/mathjax/latest/MathJax.js?config=TeX-AMS-MML_HTMLorMML'>
    </script>
        <g:javascript src="jstree/jquery.jstree.js"/>
        <g:javascript src="equalize.js"/>
        <g:javascript src="syntax/shCore.js"/>
        <g:javascript src="syntax/shBrushMdl.js"/>
        <g:javascript src="syntax/shBrushXml.js"/>
        <g:javascript src="jquery.handsontable.full.js"></g:javascript>
        <style>
        	.ui-button {
        		border-left: none;
        		margin: 0;
        	}
        </style>
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jquery.handsontable.full.min.css')}"/>
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'jstree.css')}" /> 
        <link rel="stylesheet" href="${resource(dir: 'css', file: 'filegrid.css')}" /> 
        <link rel="stylesheet" href="${resource(dir: 'css/syntax', file: 'shCore.css')}" /> 
        <link rel="stylesheet" href="${resource(dir: 'css/syntax', file: 'shThemeDefault.css')}" />
        
        <Ziphandler:outputFileInfoAsJS repFiles="${revision.files.findAll{!it.hidden}}" loadedZips="${loadedZips}" zipSupported="${zipSupported}"/>
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
                    openPage(anchor);
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
                        height:250,
                        modal: true,
                        buttons: {
                            "Confirm Delete": function() {
                                openPage('${g.createLink(controller: 'model', action: 'delete', id: (revision.model.publicationId) ?: (revision.model.submissionId))}');
                            	$( this ).dialog( "close" );
                            },
                            Cancel: function() {
                                $( this ).dialog( "close" );
                       }
                    }
            });
			
		});
		
		function getCSVData(data) {
			var lines=data.match(/[^\r\n]+/g);
			/*var content=[];
			content.push("<table>");*/
			var data = [];
			for (var line in lines) {
				var fields=lines[line].split(",");
				data.push(fields);
			}
			return data;
		}
		
		function addPreviewNotification(showNotification, fileProps) {
			if (showNotification) {
					$("#notificationgoeshere").html("As this is a large file, only a part of it is loaded below. <a id='loadFileCompletely' href=''>Click here</a> to load the file completely. Please be warned that this may be slow.");
					$("#loadFileCompletely").click( function(event) {
							event.preventDefault();
							fileProps.showPreview = false;
							updateFileDetailsPanel(fileProps);
					});
			}
			else {
				$("#notificationgoeshere").hide();
			}
		}
		
		function updateFileDetailsPanel(fileProps) {
			if (typeof(fileProps) != "undefined") {
				var formats=["text","txt","xml","pdf", "jpg","jpeg", "gif", "png", "bmp"];
				var mimeType=fileProps["mime"];
				var content=[];
				var makeAjaxCall=false;
				var imageType=false;
				var mdlType=false;
				var xmlType=false;
				var csvType=false;
				content.push("<div class='ui-widget-content ui-corner-all'><div class='padleft padright padtop'><h3>")
				content.push(fileProps["Name"])
				var fileLink="${g.createLink(controller: 'model', action: 'download', id: revision.identifier())}"
										+"?filename="+encodeURIComponent(fileProps.Name)
				content.push("<a title='Download ",fileProps.Name, "'","href='",fileLink);
				fileLink=fileLink+"&inline=true";
				content.push("'><img style='width:20px;margin-left:10px;float:none' alt='Download' src='http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/download.png'/></a></h3></div>");
				if (mimeType!=null) {
					for (var format in formats) {
						var matching=formats[format];
						if (mimeType.indexOf(matching) !=-1) {
							makeAjaxCall=true
							if (matching=="jpg" || matching=="jpeg" || matching=="gif" || matching=="png" || matching=="bmp") {
								imageType=true;
							}
							if (matching=="txt" || matching=="text" || matching=="xml") {
								if (fileProps.Name.indexOf('.mdl') !=-1) {
									mdlType=true;
								}
								if (fileProps.Name.indexOf('.xml')!=-1) {
									xmlType=true;
								}
								if (fileProps.Name.indexOf('.csv')!=-1) {
									csvType=true;
								}
							}
							content.push("<div id='notificationgoeshere' class='padleft padbottom'></div><div id='filegoeshere' class='padright padbottom")
							if (!mdlType && !xmlType) {
								content.push(" padleft") 
							}
							content.push("'>")
							if (matching=="pdf") {
								content.push("<iframe width='100%' height='500' src='")
								content.push(fileLink)
								content.push("'/>")
							}
							content.push("</div>")
						}
					}
				}
				content.push("<div class='metapanel'><div id='tableGoesHere' class='padleft padright padbottom'>")
				if (!fileProps.isInternal) {
					var detailsURL = "${g.createLink(controller: 'model', action: 'getFileDetails', id: revision.identifier())}"
											+"?filename="+encodeURIComponent(fileProps.Name)
					console.log(detailsURL);
					$.ajax({
						url: detailsURL,
						dataType: "text",
						success: function(data) {
							console.log(data);
							data=JSON.parse(data);
							var tcontent=[];						
							tcontent.push("<table cellpadding='2' cellspacing='5'>")
							for (var prop in fileProps) {
								if (prop!="isInternal" && prop!="Name" && fileProps[prop] && fileProps[prop]!="null" && prop!="mime" && prop!="showPreview") {
									tcontent.push("<tr><td><b>",prop.replace("_"," "),"</b></td><td>",fileProps[prop])
									tcontent.push("</td></tr>");
								}
							}
							tcontent.push("<tr><td><b>Submitted</b></td><td>",new Date(data[0].commit))
							tcontent.push("</td></tr>")
							tcontent.push("<tr><td><b>Last Modified</b></td><td>",new Date(data[data.length-1].commit))
							tcontent.push("</td></tr>")
									
							tcontent.push("</table>");
							$("#tableGoesHere").html(tcontent.join(""));
							$("#Files").equalize({reset: true});
						},
						error: function(jq, status, errorThrown) {
							alert(status+".."+errorThrown);
						}
					});
				}
				else {
					content.push("<table cellpadding='2' cellspacing='5'>")
					for (var prop in fileProps) {
						if (prop!="isInternal" && prop!="Name" && fileProps[prop] && fileProps[prop]!="null" && prop!="mime") {
							content.push("<tr><td><b>",prop.replace("_"," "),"</b></td><td>",fileProps[prop])
							content.push("</td></tr>");
						}
					}
					content.push("</table>");
				}
				content.push("</div></div></div>");
				$("#Files #detailsBox").html(content.join(""));
				if (makeAjaxCall) {
					if (mdlType) {
						$.ajax({
							url : fileLink+"&preview="+encodeURIComponent(fileProps.showPreview),
							dataType: "text",
							success : function (data) {
								var brush=new SyntaxHighlighter.brushes.mdl()
								brush.init({ toolbar: false });
								var html=brush.getHtml(data)
								$("#filegoeshere").html(html);
								addPreviewNotification(fileProps.showPreview, fileProps);
								$("#Files").equalize({reset: true});
								$(".syntaxhighlighter").css({'max-height': (screen.height * 0.45)+'px'});
							}
						});
					}
					else if (xmlType) {
						$.ajax({
							url : fileLink+"&preview="+encodeURIComponent(fileProps.showPreview),
							dataType: "text",
							success : function (data) {
								var brush=new SyntaxHighlighter.brushes.Xml()
								brush.init({ toolbar: false });
								var html=brush.getHtml(data)
								$("#filegoeshere").html(html);
								addPreviewNotification(fileProps.showPreview, fileProps);
								$("#Files").equalize({reset: true});
								$(".syntaxhighlighter").css({'max-height': (screen.height * 0.45)+'px'});
							}
						});
					}
					else if (csvType) {
						$.ajax({
							url : fileLink+"&preview="+encodeURIComponent(fileProps.showPreview),
							dataType: "text",
							success : function (data) {
								var plottingData=getCSVData(data)
								//$("#filegoeshere").html(plottingData);
								$("#filegoeshere").handsontable({
														data: plottingData,
														width: 625,
														height: 300,
														stretchH: 'all',
														readOnly: true,
														colHeaders: true,
								});
								addPreviewNotification(fileProps.showPreview, fileProps);
								$("#Files").equalize({reset: true});
							}
						});
					}
					else if (mimeType.indexOf("txt") != -1 || mimeType.indexOf("text") != -1) {
						$.ajax({
							url : fileLink+"&preview="+encodeURIComponent(fileProps.showPreview),
							dataType: "text",
							success : function (data) {
								$("#filegoeshere").text(data);
								$("#filegoeshere").html($("#filegoeshere").html().replace(/(\r\n|\n|\r)/gm,'<br/>'));
								addPreviewNotification(fileProps.showPreview, fileProps);
								$("#Files").equalize({reset: true});
							}
						});
					}
					else if (imageType) {
						var img = $("<img style='width:100%;' />").attr('src', fileLink)
									.load(function() {
										if (!this.complete || typeof this.naturalWidth == "undefined" || this.naturalWidth == 0) {
											$("#filegoeshere").text("Image could not be loaded")
										} else {
											$("#filegoeshere").append(img);
											$("#Files").equalize({reset: true});
										}
						});
					}
				}
			}
			else {
				$("#Files #detailsBox").html("");
			}
			$("#Files").equalize({reset: true});
		}
		
		$(document).ready(function() {
			// Handler for .ready() called.
			$("#Files #treeView").bind("select_node.jstree", function(event, data) {
					var clickedOn=$(data.args[0]).attr('title')
					if (clickedOn!=null) {
						clickedOn = clickedOn.replace(/^\s+|\s+$/g,'')
						var fileProps=fileData[clickedOn]
						updateFileDetailsPanel(fileProps)
					}
					else {
						$("#Files #detailsBox").html("");
					}
				}).jstree({
				"ui" : {
					"select_limit" : 1
				},
				"themes" : {
					    "theme" : "classic",
					    "icons" : false
				},
				"plugins" : [ "themes", "html_data", "ui"]
			});
			var tree = $("#Files #treeView");
			tree.bind("loaded.jstree", function (event, data) {
				tree.jstree("open_all");
			});
			$('#confirm-model-publish').dialog({
                resizable: false,
                autoOpen: false,
                height: 300,
                modal: true,
                buttons: {
                    Confirm: function() {
                        openPage("${g.createLink(controller: 'model', action: 'publish', id: revision.identifier() )}");
                        $( this ).dialog( "close" );
                    },
                    Cancel: function() {
                        $( this ).dialog( "close" );
                    }
                }
            });
            $("body").append("<div id='modelToolbar' class='collapsibleContainer' title='Model Toolbar'><button title='Expand Toolbar' data-showing='0' id='panelToggle'>Expand</button></div>	");
            $("#buttonContainer").prependTo("#modelToolbar");
            $("#panelToggle").click(function (evt){
            		displayToolbar($("#panelToggle").data("showing") == '0', true);
            });
         	$( "#download" ).button({
					text:false,
					icons: {
						primary:"ui-icon-arrowthickstop-1-s"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });;
			$( "#update" ).button({
					text:false,
					icons: {
						primary:"ui-icon-refresh"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });;
			$( "#delete" ).button({
					text:false,
					icons: {
						primary:"ui-icon-trash"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });;
			$( "#publish" ).button({
					text:false,
					icons: {
						primary:"ui-icon-unlocked"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });;
			$( "#share" ).button({
					text:false,
					icons: {
						primary:"ui-icon-person"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });;
			$("#panelToggle").button({
					text:false,
					icons: {
						primary: "ui-icon-circle-arrow-e"
					}
			}).removeClass('ui-corner-all').css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px', 'float':'right'  });;
		});
		displayToolbar(false, false);
            
		function displayToolbar(show, firstTime) {
			if (show) {
					$("#panelToggle").data("showing", '1');
            		$(".pagecontent").width("95%");
	            	$(".pagecontent").css('margin-left', '80px');
	            	$(".buttonLabel").show();
	            	$("#modelToolbar").width("120px");
	            	$( "#panelToggle" ).button("option", {
			    			icons: { primary: "ui-icon-circle-arrow-w" }
			    	});
			    	$( ".toolbutton" ).button("option", "text", true);
			    	$( ".toolbutton" ).css({ width: '120px', 'padding-top': '10px', 'padding-bottom': '10px' });
			}
			else {
					$("#panelToggle").data("showing", '0');
            		$(".pagecontent").width("100%");
	            	$(".pagecontent").css('margin-left', '5px');
	            	$(".buttonLabel").hide();
			    	$("#modelToolbar").width("45px");
			    	$( ".toolbutton" ).button("option", "text", false);
			    	if (firstTime) {
			    		$( "#panelToggle" ).button("option", {
			    			icons: { primary: "ui-icon-circle-arrow-e" }
			    		});
			    	}
			    	$( ".toolbutton" ).css({ width: '45px', 'padding-top': '10px', 'padding-bottom': '10px' });
			}
		}
		
        function openPage(loc) {
            window.location.href = loc;
        }
    </script>
    <g:layoutHead/>
    </head>
    <body>
    	<div id="buttonContainer" style="display:inline"<%--class="ui-widget-header ui-corner-all"--%>>
				<ul id='toolbarList'><li>
    			<button class='toolbutton' id="download" onclick="return openPage('${g.createLink(controller: 'model', action: 'download', id: revision.identifier())}')">Download</button>
				<g:if test="${canUpdate}">
					<li>
					<button class='toolbutton' id="update" onclick="return openPage('${g.createLink(controller: 'model', action: 'update', id: (revision.model.publicationId) ?: (revision.model.submissionId))}')">Update</button>
					</li>
				</g:if>
				<g:if test="${canDelete}">
					<div id="dialog-confirm" title="Confirm Delete">
						<p>Are you sure you want to delete the model?</p>
					</div>
					<li>
					<button class='toolbutton' id="delete" onclick='return $( "#dialog-confirm" ).dialog( "open" );'>Delete</button>
					</li>
				</g:if>
				<g:if test="${showPublishOption}">
					<div id="confirm-model-publish" title="You are about to publish this model version">
						<p>Make this version of the model visible to anyone without logging in?</p>
					</div>
					<li>
					<button class='toolbutton' id="publish" onclick="return $( '#confirm-model-publish' ).dialog( 'open' );">Publish</button>
					</li>
				</g:if>
				<g:if test="${canShare}">
					<li>
					<button class='toolbutton' id="share" onclick="return openPage('${g.createLink(controller: 'model', action: 'share', id: revision.identifier())}')">Share</button>
					</li>
				</g:if>
				</ul>
         </div>
        <div class="ebiLayout_reduceWidth">
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
                    (revision.model.publicationId) ?: (revision.model.submissionId))}">here</a>.
    		</div>
    	</g:if>
    	<div id="topBar">
    		    <div style="float:left;width:75%;">
    				<h1>${revision.name}</h1>
    			</div>
    			<div style="float:right;margin-top:10px;">
    				<g:if test="${revision.state==ModelState.PUBLISHED}">
						<img style="float:right;margin-top:0;" title="This version of the model is public" alt="public model" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/unlock.png"/>
					</g:if>
					<g:else>
						<img style="float:right;margin-top:0;" title="This version of the model is unpublished" alt="unpublished model" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/lock.png"/>
					</g:else>                                
				</div>
   
    	        <%--        <a class="submit" title="Update Model" href="${g.createLink(controller: 'model', action: 'update', id: (revision.model.publicationId) ?: (revision.model.submissionId))}">Update</a>
        	<a class="submit" title="Download Model" href="${g.createLink(controller: 'model', action: 'download', id: revision.identifier())}">Download</a>
	 --%></div>
    	<div id="tablewrapper">
    	<div id="tabs">
	  <ul class='modelTabs'>
	    <li><a href="#Overview">Overview</a></li>
	    <li><a href="#Files">Files</a></li>
	    <li><a href="#History">History</a></li>
	    <g:pageProperty name="page.modelspecifictabs" />
	  </ul>
	  <div id="Overview">
	  	<label>Model Description:</label>
	  		<ul style="list-style-type: none;">
	  			<li>${revision.description}</li>
	  		</ul>
	    <table style="margin-top:30px">
		<tr>
		    <td><label><g:message code="model.model.format"/></label></td>
		    <td><div class='spaced'>${revision.format.name} ${revision.format.formatVersion!="*"?"(${revision.format.formatVersion})":""}</div></td>
		</tr>
        <%
            model = revision.model
        %>
        <g:if test="${model.publication}">
            <tr>
                <td><label><g:message code="model.model.publication"/>:</label></td>
                <td>
                    <div class='spaced'>
                        <g:render  model="[model:model]" template="/templates/showPublication" />
                    </div>
                </td>
            </tr>
        </g:if>
		<tr>
		    <td><label><g:message code="model.model.authors"/></label></td>
		    <td>
		    	<div class='spaced'>
			    	<%
			    		StringBuilder authorString=new StringBuilder()
			    		authors.eachWithIndex() { author, i -> 
			    			if (i!=0) {
			    				authorString.append(", ")
			    			}
			    			authorString.append(author)
			    		};
			    	%>
		    		${authorString.toString()}
		    	</div>
		    </td>
		</tr>
	    </table>
	
	  </div>
	  <div id="Files" class="filegrid">
	  	<div class="filecol-1-3">
	  		<div id="treeView">
	  			<ul>
	  				<li rel="folder"><a>Main Files</a>
	  				<ul>
	  					<Ziphandler:outputFileInfoAsHtml repFiles="${revision.files}" loadedZips="${loadedZips}" zipSupported="${zipSupported}" mainFile="${true}"/>
	  				</ul>
	  				</li>
	  			</ul>
	  			<ul>
	  				<g:if test="${revision.files.find{!it.hidden && !it.mainFile}}">
	  				<li><a>Additional Files</a>
	  					<ul>
	  						<Ziphandler:outputFileInfoAsHtml repFiles="${revision.files.findAll{!it.hidden}}" loadedZips="${loadedZips}" zipSupported="${zipSupported}" mainFile="${false}"/>
	  				   </ul>
	  			    </li>
	  			    </g:if>
	  		</ul>
  		</div>
  		</div>
  		<div class="filecol-2-3">
  			<div id="detailsBox" class="detailsBox"></div>
  		</div>
  	  </div>
	  <div id="History">
	  	<% DateFormat dateFormat = DateFormat.getDateTimeInstance(); %>
	  	<ul>
	  		<li>Model owner: ${revision.model.submitter}</li>
	  		<li>Submitted: ${dateFormat.format(allRevs.first().uploadDate)}</li>
	  		<li>Last Modified: ${dateFormat.format(allRevs.last().uploadDate)}</li>
	  	</ul>
	  	<h5>Revisions</h5>
	  	<ul>
	  	     <g:each status="i" var="rv" in="${allRevs.sort{a,b -> a.revisionNumber > b.revisionNumber ? -1 : 1}}">
	  	     	<li style="${revision.id == rv.id ?"background-color:#FFFFCC;":""}margin-top:5px">
	  	     		Version: ${rv.revisionNumber}
	  	     		<g:if test="${rv.state==ModelState.PUBLISHED}">
                           	<img style="width:12px;margin:2px;float:none;" title="This version of the model is public" alt="public model" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/unlock.png"/>
                    </g:if>
					<g:else>
							<img style="width:12px;margin:2px;float:none;" title="This version of the model is unpublished" alt="unpublished model" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/lock.png"/>
					</g:else>
	  	     		<g:if test="${revision.id!=rv.id}">
	  	     			<a class="versionDownload" title="go to version ${rv.revisionNumber}" href="${g.createLink(controller: 'model', action: 'show', id: rv.identifier())}">
	  	     				<img style="width:12px;margin:2px;float:none" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Generic/Generic%20icons/external_link.png"/> 
	  	     			</a>
	  	     		</g:if>
	  	     				<a class="versionDownload" title="download" href="${g.createLink(controller: 'model', action: 'download', id: rv.identifier())}">
	  	     					<img alt="Download this version" style="width:15px;float:none" src="http://www.ebi.ac.uk/web_guidelines/images/icons/EBI-Functional/Functional%20icons/download.png"/>
	  	     				</a>
	  	     			<ul>
	  	     				<li>Submitted on: ${dateFormat.format(rv.uploadDate)}</li>
	  	     				<li>Submitted by: ${rv.owner}</li>
	  	     				<li>With comment: ${rv.comment}</li>
	  	     			</ul>
	  	     	</li>
	  	     </g:each>
  		</ul>
	  </div>
	  <g:pageProperty name="page.modelspecifictabscontent" />
	</div>
	</div>
    </div>

</body>
<content tag="contexthelp">
		display
</content>
</g:applyLayout>
