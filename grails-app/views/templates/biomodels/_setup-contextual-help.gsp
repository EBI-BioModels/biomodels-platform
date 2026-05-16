<g:javascript>
$.appName = "${grailsApplication.metadata["app.name"]}";
$.serverUrl = "${grailsApplication.config.grails.serverURL}";
let helpHidden = 1;

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
