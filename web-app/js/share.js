var template;
var toJSON = function(form) {
    return form.serializeArray().reduce(function(json,kv) {
      json[kv.name] = kv.value;
      return json;
    },{});
  };
var Collaborator = Backbone.Model.extend({
});

var Collaborators = Backbone.Collection.extend({
    model: Collaborator,
    add: function(newCollab) {
		try {
			newCollab.id = encodeURIComponent(newCollab.id);
		}
		catch(ignoreException) {
		}
     	Backbone.Collection.prototype.add.call(this, newCollab);
    }

});
function makeBoolean(value) {
    value=value||false;
    if (value==="on") {
        value=true;
    }
    return value;
}
var lookupURL="";
var submitURL="";
var autoURL="";
var teamLookup="";
var selectedItem;
collaborators = new Collaborators;
CollaboratorTable = Backbone.View.extend({
    events: {
        "click .remove": "remove",
        "click .updateCollab": "updateCollab"
    },
    initialize: function() {
        _.bindAll(this,"render","error");
        this.listenTo(this.collection,"add", this.render);
        this.listenTo(this.collection,"error",this.error);
        this.listenTo(this.collection,"update",this.render);
        var source = $('#collaborator-list-template').html();
        template = Handlebars.compile(source);
        this.render();
    },
    error: function(model,msg){
        alert(msg);
    },
    submitData: function(evt) {
        evt.preventDefault();
    },
    performSubmission:function() {
        var collabString=JSON.stringify(this.collection);
        collabString = decodeURIComponent(collabString);
        $(":input").prop('disabled', true);
        $.post(submitURL, { collabMap: collabString }, function(returnedData) {
            if (returnedData.success) {
                $(":input").prop('disabled', false);
            }
            else {
                showNotification(returnedData.message);
                scheduleHide();
                $(":input").prop('disabled', false);
            }
        });
    },
    create: function(evt) {
        evt.preventDefault();
        var objectCreated=toJSON($("#collaboratorAddForm"));
        objectCreated.read=makeBoolean(objectCreated.read);
        objectCreated.write=makeBoolean(objectCreated.write);
        if (objectCreated.write) {
            objectCreated.read=true;
        }
        objectCreated.disabledEdit=false;
        objectCreated.show=true;
        var that = this;
        if (selectedItem && selectedItem[2]==objectCreated.name) {
            objectCreated.id = selectedItem[1];
            collaborators.add(objectCreated);
            this.performSubmission();
        } else {
            $.post(lookupURL, { name: objectCreated.name }, function(returnedData) {
                if (returnedData.found) {
                    objectCreated.id=returnedData.username;
                    collaborators.add(objectCreated);
                    that.performSubmission();
                } else {
                    showNotification("Could not find a user by that name");
                    scheduleHide();
                }
            });
        }
    },
    createTeam: function(evt) {
    	evt.preventDefault();
        var objectCreated=toJSON($("#collaboratorAddForm"));
        objectCreated.read=makeBoolean(objectCreated.teamRead);
        objectCreated.write=makeBoolean(objectCreated.teamWrite);
        if (objectCreated.write) {
            objectCreated.read=true;
        }
        objectCreated.disabledEdit=false;
        objectCreated.show=true;
        var that = this;
        $.post(teamLookup, { teamID: objectCreated.team }, function(returnedData) {
                _.each(returnedData, function(teamMember) {
                		var collaborator = {};
                		collaborator.id = teamMember.username;
                		collaborator.name = teamMember.userRealName;
                		collaborator.read = objectCreated.read;
                		collaborator.write = objectCreated.write;
                		collaborator.disabledEdit=false;
                		collaborator.show=true;
                		that.collection.add(collaborator);
                });
                that.performSubmission();
        });
    },
    updateCollab: function(evt) {
        //evt.preventDefault();
        //evt.stopPropagation();
        var buttonElement=$("#"+evt.currentTarget.id);
        var id=buttonElement.data("person");
        var field=buttonElement.data("field");
        var collab=this.collection.get(id);
        collab.set(field, buttonElement.is(':checked'));
        if (field==="write") {
            if (collab.get("write")) {
                collab.set("read", true);
            }
        } else {
            if (collab.get("read")) {
                collab.set("write", false)
            }
        }
      //this.render();
      this.performSubmission();
    },
    update: function(evt) {
        evt.preventDefault();
        evt.stopPropagation();
        var form = $(evt.currentTarget);
        var id = form.attr("data-id");
        this.collection.get(id).save(toJSON(form),{
          error:this.error
        });
    },
    remove: function(evt) {
        evt.preventDefault();
        var buttonElement=$("#"+evt.currentTarget.id);
        var id=buttonElement.data("person");
        var nameRemoved=buttonElement.data("name");
        var collaborator=buttonElement.parent().parent();
        var removed=this.collection.remove(id);
        this.render();
        this.performSubmission();
    },
    render: function() {
        willBeRendered=this.collection.length;
        this.collection.each(function(collab) {
            if (!collab.get("show")) {
            willBeRendered--;
            }
        });
        var renderThis = {collabsList: this.collection.toJSON(), hasCollabs: willBeRendered>0};
        var html = template(renderThis);
        this.$el.html(html);
        addButtonEvents();
    }
});

function addButtonEvents() {
    if (collaborators.length < 3) {
        $("#collabCreate").removeClass('hideRightBorder');
        $("#currentCollabs").addClass('hideLeftBorder');
        $("#collabUI").height($("#collabCreate").height());
    }
    else {
        $("#collabCreate").addClass('hideRightBorder');
        $("#currentCollabs").removeClass('hideLeftBorder');
        $("#collabUI").height($("#currentCollabs").height());
    }
 }

function switchRadio(me, opposite) {
    if ($(me).is(':checked')) {
        $(opposite).prop("checked", false);
    }
}

Handlebars.registerHelper('setChecked', function(mode) {
    if (mode=="read") {
        if (this.read && !this.write) {
            return "checked";
        }
    }
    else {
        if (this.write) {
            return "checked";
        }
    }
});

var searchTerm="";
var obscured={};

function getHighlighted(text) {
    return text.replace(new RegExp(searchTerm, 'gi'), "<SPAN style='BACKGROUND-COLOR: #ffff00'>"+searchTerm+"</SPAN>");
}

function obscure(text) {
    retval=[];
    for (var i=0; i<text.length; i++) {
        if (Math.random() > 0.75) {
            retval.push('_');
        }
        else {
            retval.push(text[i]);
        }
    }
    text=retval.join('');
    if (text.indexOf("_")==-1) {
        text=obscure(text);
    }
    return text;
}

function obscureEmail(text) {
    existing = obscured[text];
    if (typeof existing === "undefined") {
        parts=text.split("@");
        obscured[text]=obscure(parts[0])+"@"+obscure(parts[1]);
    }
    return obscured[text]
}

function main(existing, contURL, submit, autoComp, teamURL) {
    collaboratorList = new CollaboratorTable({
        collection: collaborators
    });
    lookupURL=contURL;
    submitURL=submit;
    autoURL=autoComp;
    teamLookup = teamURL;
    $('.containUI').html(collaboratorList.$el);
    _.each(existing, function(collab) {
        if (collab.write) {
            collab.read=true;
        }
        collaborators.add(collab);
    });
    $("#radioWriter").click(function() {
        switchRadio("#radioWriter", "#radioReader");
    });
    $("#radioReader").click(function() {
        switchRadio("#radioReader", "#radioWriter");
    });
    $("#teamRadioReader").click(function() {
        switchRadio("#teamRadioReader", "#teamRadioWriter");
    });
    $("#teamRadioWriter").click(function() {
        switchRadio("#teamRadioWriter", "#teamRadioReader");
    });
    autoComplete(collaborators, autoURL);
    addButtonEvents();
    $( "#SaveCollabs" ).click(function(event){
        collaboratorList.submitData(event);
    });
    $( "#AddButton" ).click(function(event){
        collaboratorList.create(event);
    });
    $( "#TeamAddButton" ).click(function(event) {
        collaboratorList.createTeam(event);
    });
    $("#teamSearch").width($("#nameSearch").width()*0.94);
}

function autoComplete(collabs, url) {
    $( "#nameSearch" ).autocomplete({
        minLength: 2, source: function( request, response ) {
            var term = request.term;
            searchTerm=request.term;
            $.getJSON( url, request, function( data, status, xhr ) {
                if (collabs.length == 0) {
                    response(data);
                } else {
                    var filtered=[];
                    $.each(data, function(result) {
                        if (collabs.findWhere({name: data[result][2], id: data[result][1]})) {
                        }
                        else {
                            filtered.push(data[result]);
                        }
                    });
                    response( filtered );
                }
            });
        },
        select: function( event, ui ) {
            $( "#nameSearch" ).val( ui.item[2] );
            selectedItem=ui.item;
            return false;
        }
    }).data( "ui-autocomplete" )._renderItem = function( ul, item ) {
        return $( "<li>" )
            .append( "<a>" + getHighlighted(item[2]) + " ("+ getHighlighted(item[1])+")<br/>" + getHighlighted(obscureEmail(item[0])) + "<hr style='margin: 0.5em 0 !important;'/></a>" )
            .appendTo( ul );
    };
}
