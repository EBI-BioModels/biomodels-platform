<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
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
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>

<%
    def contextHelpLocation=g.pageProperty(name:'page.contexthelp')
    if (contextHelpLocation) {
        contextHelpLocation=contextHelpLocation.trim()
    }
    else {
        contextHelpLocation="manual"
    }
    int helpWidth = 800;

    def styleName = grailsApplication.config.jummp.branding.style

    response.setHeader("Cache-Control","no-cache");
    response.setHeader("Cache-Control","no-store");
    response.setDateHeader("Expires", 0);
    response.setHeader("Pragma","no-cache");
%>

<!doctype html>
<g:render template="/templates/${styleName}/precursor" />
<html lang="en">
<head>
    <g:render template="/templates/${styleName}/head" />
    <g:javascript>
        $.appName = "${grailsApplication.metadata["app.name"]}";
        $.serverUrl = "${grailsApplication.config.grails.serverURL}";
        var helpHidden=1;

        <g:if test="${contextHelpLocation}">
            var helpWidth=-1;
            var maxWidth=-1;
            var maxHelpWidth=800;
            var minHelpWidth=50;
            var stepWidth=30;
            var syncResize=0;
            function adjustWidth(newWidth) {
                if (syncResize==1) {
                    var delta= helpWidth - newWidth;
                    var mainWidth=$("#mainframe" ).width();
                    if (maxWidth==-1) {
                        maxWidth=mainWidth;
                    }
                    if (maxWidth<(mainWidth+delta)) {
                        mainWidth=maxWidth;
                    }
                    else {
                        mainWidth+=delta;
                    }
                    $( "#mainframe" ).width(mainWidth);
                    helpWidth=newWidth;
                }
            }

            function hideHelp() {
                $( "#helpPanel" ).removeClass("helpHeightWorkaround")
                adjustWidth(0);
                $( "#helpPanel" ).hide();
                helpHidden=1;
                helpWidth=-1;
                $('#toggleHelp').text("More about this page");
                $('#toggleHelp').attr("title", "Access help for this page");
            }

            function showHelp() {
                $( "#helpPanel" ).addClass("helpHeightWorkaround")
                helpWidth=-1;
                $("html, body").animate({ scrollTop: 0 }, "medium");
                $( "#helpPanel" ).width(${helpWidth});
                    $( "#helpPanel" ).show();
                    adjustWidth(${helpWidth});
                    $( "#helpPanel" ).position({
                        my: "right-1 bottom",
                        at: "right-1 bottom",
                        of: ".main-menu"
                    });
                    helpHidden=0;
                    $('#toggleHelp').text("Hide help");
                    $('#toggleHelp').attr("title", "Hide help");
                }
                var isDragged=false;
        </g:if>
        $(function() {
        <bmsec:whenLoggedIn>
            pollForNotifications('<g:createLink controller="notification" action="unreadNotificationCount"/>')
        </bmsec:whenLoggedIn>
        <g:if test="${contextHelpLocation}">
            $("#helpPanel").resizable({
                        handles: 'n,e,s,w',
                        maxWidth: maxHelpWidth,
                        animate: true,
                        resize: function( event, ui ) {
                            if (helpWidth==-1) {
                                helpWidth=ui.originalSize.width;
                            }
                            adjustWidth(ui.size.width);
                        }
            });
            $( "#helpPanel").draggable({ cursor: "move",
                                          revert: false,
                                          containment: "body",
                                          start: function() {
                                            isDragged=true;
                                          },
                                          stop: function( event, ui ) {
                                            isDragged=false;
                                          }
                                        });
            $( "#helpPanel" ).hide();

            $("#toolbar").mouseleave(function() {
                if (isDragged) {
                    $( "#helpPanel" ).draggable( "disable" );
                    $( "#helpPanel" ).draggable( "enable" );
                }
            })


            $( "#expand" ).button({
                text: false,
                icons: {
                    primary: "ui-icon-circle-plus"
                }
            }).click(function() {
                 helpWidth=$("#helpPanel" ).width();
                if (maxHelpWidth<(helpWidth+stepWidth)) {
                    helpWidth=maxHelpWidth;
                }
                else {
                    helpWidth+=stepWidth;
                }
                var windowRight = document.body.getBoundingClientRect ().right
                console.log(windowRight);
                $( "#helpPanel" ).width(helpWidth);
                var el= document.getElementById ("helpPanel");
                var helpRight = el.getBoundingClientRect ().right
                if (helpRight > windowRight) {
                    var offset = helpRight - windowRight + 10
                    $('#helpPanel').animate({
                        'marginLeft' : "-=" + offset + "px"
                    });
                }

            });
            $( "#contract" ).button({
                text: false,
                icons: {
                    primary: "ui-icon-circle-minus"
                }
            }).click(function() {
                 helpWidth=$("#helpPanel" ).width();
                if (minHelpWidth>helpWidth-stepWidth) {
                    helpWidth=minHelpWidth
                }
                else {
                    helpWidth-=stepWidth;
                }
                $( "#helpPanel" ).width(helpWidth);
                var addedPercentage = (helpWidth - 400) / 400
                var basic = 99+addedPercentage;
                $( "#helpFrame" ).width(basic+"%");
            });
            $( "#close" ).button({
                text: false,
                icons: {
                    primary: "ui-icon-circle-close"
                }
            }).click(function() {
                hideHelp();
            });

            $( "#snap" ).button({
                text: false,
                icons: {
                    primary: "ui-icon-arrowrefresh-1-e"
                }
            }).click(function() {
                hideHelp();
                showHelp();
            });
            $( "#outlink" ).button({
                text: false,
                icons: {
                    primary: "ui-icon-extlink"
                }
            }).click(function() {
                hideHelp();
                window.open('<ContextHelp:getURL location="${contextHelpLocation}"/>','_blank');
                    });
                    $('#toggleHelp').click(function(event) {
                        event.preventDefault();
                        if (helpHidden==1) {
                            showHelp();
                        }
                        else {
                            hideHelp();
                        }
                    });
        </g:if>
        });
    </g:javascript>
    <g:javascript src="jummp.js"/>
    <g:javascript src="notification.js"/>

    <link rel="stylesheet" href="<g:resource dir="css" file="notification.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/${styleName}" file="layout.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/${styleName}" file="${styleName}.css"/>" />
    <link rel="stylesheet" href="<g:resource dir="css/jqueryui/smoothness" file="jquery-ui-1.10.3.custom.min.css"/>" />
    <g:layoutHead/>
</head>
<!-- open body tag -->
<%
    def sidebarContent = g.pageProperty(name:'page.sidebar')
    if (sidebarContent) {
        sidebarContent = sidebarContent.trim()
    }
%>
<body class="level2 full-width">
<g:if test="${session.enabled2FA || (session.showEnrollmentNotice && !(controllerName == 'usermanagement' && actionName == 'show'))}">
    <div style="position:fixed;top:0;left:0;width:100%;background:#f0ad4e;color:#333;text-align:center;padding:10px;font-weight:bold;z-index:9999;">
        <g:if test="${session.enabled2FA}">
            You are not fully logged in &mdash; please check your email for the One-Time Passcode and
            <a href="${createLink(controller:'auth', action:'load2fa')}" style="color:#333;text-decoration:underline;">complete your verification</a>.
        </g:if>
        <g:else>
            Two-factor authentication will soon be required for all accounts.
            <a href="${createLink(controller:'usermanagement', action:'show')}" style="color:#333;text-decoration:underline;">Set up 2FA now</a>
            to ensure uninterrupted access.
        </g:else>
    </div>
    <div style="height:41px;"></div>
</g:if>
<div id="mainframe">
    <g:render template="/templates/${styleName}/header"/>
    <g:render template="/templates/${styleName}/mainbody"/>
    <g:render template="/templates/${styleName}/footer"/>

    <g:if test="${contextHelpLocation}">
        <div id="helpbutton">
            <a id="toggleHelp" title="Access help for this page" href="#">More about this page</a>
        </div>
        <div id="helpPanel">
            <div id="toolbar" class="ui-widget-header ui-corner-all">
                <button id="expand">Increase help size</button>
                <button id="contract">Decrease help size</button>
                <button id="snap">Reset help</button>
                <button id="outlink">Open in a new tab</button>
                <button id="close">Close</button>
            </div>
            <ContextHelp:getLink location="${contextHelpLocation}" width="${helpWidth}"/>
        </div>
    </g:if>
</div>
<g:render template="/templates/${styleName}/loadjs"/>
</body>
</html>
