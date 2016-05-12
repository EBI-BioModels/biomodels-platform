<head>
    <meta name="layout" content="modelAnnotation"/>
    <script>
        var Jummp = Jummp || {};
        Jummp.data = {
            objectModel: ${objectModel},
            existingAnnotations: ${existingAnnotations},
            annoPropsMap: ${annoPropsMap}
        }
    </script>

    <script id="unconstrained-value-template" type="text/x-handlebars-template">
        {{#each this.items}}
            <input type="text" size="50" value="{{getLabelForUnconstrainedChoice this}}"/>
        {{else}}
            <input type="text" size="50" />
        {{/each}}
    </script>
    <script id="multiple-unconstrained-value-template" type="text/x-handlebars-template">
        <div>your multiple input fields go here</div>
    </script>
    <script id="single-constrained-value-template" type="text/x-handlebars-template">
        <form>
        {{#each this.items}}
            <input type="radio" value="{{value}}" name="{{getNameForRadioButton this}}" id="{{getRadioButtonId this @index}}" {{#isSelected}}checked='checked'{{/isSelected}}/><label class="inlineLabel" for="{{getRadioButtonId this @index}}">{{value}}</label>
        {{/each}}
        </form>
    </script>
    <script id="multiple-constrained-value-template" type="text/x-handlebars-template">
        {{renderMultipleConstrainedValues}}
    </script>
    <script id="property-template" type="text/x-handlebars-template">
        <h3>{{this.value}}</h3>
        <div class="valueContainer">{{this.uri}} : {{this.range.name}}</div>
        {{#if tooltip}}
            <div class="ui-state-highlight ui-state-cornerall info">
                <p>
                <span class="ui-icon ui-icon-info infoLabel"></span>
                    {{this.tooltip}}
                </p>
            </div>
        {{/if}}
    </script>
    <script type="text/x-handlebars-template" id="properties-template"></script>
    <script id="tab-heading-template" type="text/x-handlebars-template">
        <a href="#{{this.id}}">{{this.name}}</a>
    </script>
    <script id="tab-headings-template" type="text/x-handlebars-template"></script>
    <script id="section-template" type="text/x-handlebars-template">
        <div id="{{this.id}}">
            <div class="ui-state-highlight ui-state-cornerall info">
                <p class="spacedTopBottom"><span class="ui-icon ui-icon-info infoLabel"></span>{{this.info}}</p>
            </div>
            <div class="properties">
            </div>
        </div>
    </script>
    <script id="sections-template" type="text/x-handlebars-template"></script>
    <script id="root-section-template" type="text/x-handlebars-template">
        <div id="sectionRoot">
            <div id="tabHeadings"></div>
            <div id="tabContent"></div>
        </div>
    </script>
</head>
<body>
    <div id="message"></div>
    <div id="report"></div>
    <h1>Annotate Model ${revision.name} (${revision.model.publicationId ?: revision.model.submissionId})</h1>
    <div id="toolbar" class="ui-corner-all">
        <button id="saveButton" title="Save model properties" class="action">Save</button>
        <button id="validateButton" title="Validate model properties" class="action">Validate</button>
        <button id="backButton" title="Return to the model display page" class="action" onclick="return $.jummp.openPage('${g.createLink(controller: 'model', action: 'show', id: modelId)}')">Return to model display page</button>
    </div>
    <div id="annotationEditor">
        <div id="leftContainer">
        </div>
        <div id="rightContainer"></div>
        <div class="resetFloat"></div>
    </div>
    <script type="text/javascript">
        var Jummp = Jummp || {};
        Jummp.annoEditor.on("before:start", function(options) {
            var annotationSections = new AnnotationSections(Jummp.data.objectModel);
            Jummp.annotationSections = annotationSections;
            var setSelectedForList = function(values, s, prop) {
                var v;
                var haveUri = s.uri != undefined;
                var haveName = s.name != undefined;
                if (!haveName && !haveUri) return;
                if (haveUri) {
                    v = values.findWhere({uri: s.uri});
                } else {
                    v = values.findWhere({value: s.name});
                }
                if (v != undefined) {
                    v.set({'isSelected': true});
                    Jummp.data.foundThisAnnoPropEntry = true;
                }
                /*
                 * not finding a match here isn't a problem because we're dealing
                 * with one layer of what could be a hierarchy at a time.
                 */
            }

            var bind = function (prop, selection) {
                var propRange = prop.get('range').name;
                switch (propRange) {
                    case "SINGLE_UNCONSTRAINED_CHOICE":
                    case "MULTIPLE_UNCONSTRAINED_CHOICE":
                        _.each(selection, function(s) {
                            var newValue = new Value({
                                isSelected: true,
                                options: {},
                                uri: s.uri,
                                value: s.name,
                                annotationProperty: prop
                            });
                            prop.get('values').push(newValue);
                        });
                        break;
                    case "SINGLE_CONSTRAINED_CHOICE":
                    case "MULTIPLE_CONSTRAINED_CHOICE":
                        /* flag to ensure that the revision is not annotated with a stale xref for this property. */
                        Jummp.data.foundThisAnnoPropEntry = false;
                        _.each(selection, function(s) {
                            var values = prop.get('values');
                            setSelectedForList(values, s, prop);
                            values.each(function(parent) {
                                var children = parent.get('children');
                                if (children != undefined) {
                                    setSelectedForList(children, s, prop);
                                }
                            });
                        });
                        if (false == Jummp.data.foundThisAnnoPropEntry) {
                            var propUri = prop.get("uri");
                            delete Jummp.data.annoPropsMap[propUri];
                            Jummp.removeRdfStatement({subject: 'theSubject', predicate: propUri});
                        }
                        delete Jummp.data.foundThisAnnoPropEntry;
                        break;
                    default:
                        console.warn("unsupported property range type", propRange);
                }
            };
            /* used to clear annotations from the previous revision which no longer match what's available in the view. */
            var visitedProps = [];
            annotationSections.each(function(section) {
                var props = section.get('annotationProperties');
                var annoProperties = new AnnotationProperties(props);
                section.set('annotationProperties', annoProperties);
                annoProperties.each(function(p) {
                    var uri = p.get('uri');
                    var vals = p.get('values');
                    var pValues = new Values(vals);
                    pValues.each(function(v) {
                        v.set('annotationProperty', p);
                        var children = v.get('children');
                        if ( children != undefined) {
                            var childValues = new Values(children);
                            childValues.each(function(child) {
                                child.set('annotationProperty', p);
                            });
                            v.set('children', childValues);
                        }
                    });
                    p.set('values', pValues);
                    var selection = Jummp.data.annoPropsMap[uri];
                    if (selection) {
                        bind(p,selection);
                    }
                    visitedProps.push(uri);
                });
            });
            /* remove what's not been visited */
            _.keys(Jummp.data.annoPropsMap).forEach(function(candidate) {
                if (! _.contains(visitedProps, candidate)) {
                    delete Jummp.data.annoPropsMap[candidate];
                    Jummp.removeRdfStatement({subject: 'theSubject', predicate: candidate});
                }
            });
            var sectionTabs = new TabbedLayout({annotationSections: annotationSections});
            Jummp.annoEditor.mainRegion.show(sectionTabs);
        });

        Jummp.annoEditor.start();
</script>
<g:javascript>
    /* use jQueryUI to render radio buttons */
    $("input[type=radio]").buttonset();

    /* toolbar button icons */
    $('#saveButton').button({
        icons: {
            primary: "ui-icon-check"
        }
    }).on("click", function(event) {
        "use strict";
        event.preventDefault();
        $.ajax({
            dataType: "json",
            type: "GET",
            url: $.jummp.createLink("annotation", "save"),
            cache: false,
            data: {
                annotations: JSON.stringify(Jummp.data.existingAnnotations),
                revision: "${revision.model.publicationId ?: revision.model.submissionId}"
            },
            beforeSend: function() {
                $("#message").removeClass("success");
                $("#message").removeClass("failure");
                $("#message").addClass("jummpWarning");
                $('#message').html('Annotations are being saved into database. Please wait...');
                $('#saveButton').attr('disabled',true);
                $('#validateButton').attr('disabled',true);
                $('#backButton').attr('disabled',true);
            },
            error: function(jqXHR) {
                console.error("epic fail", jqXHR.responseText);
                $("#message").removeClass("success");
                $("#message").removeClass("jummpWarning");
                $("#message").addClass("failure");
                $('#message').html("There was an internal error while saving the annotations provided.");
                $('#saveButton').removeAttr('disabled');
                $('#validateButton').removeAttr('disabled');
                $('#backButton').removeAttr('disabled');
            },
            success: function(response) {
                if ("200" == response.status) {
                    $("#message").removeClass("failure");
                    $("#message").removeClass("jummpWarning");
                    $("#message").addClass("success");
                } else {
                    $("#message").removeClass("success");
                    $("#message").removeClass("jummpWarning");
                    $("#message").addClass("failure");
                }
                $('#message').html(response.message);
                $('#saveButton').removeAttr('disabled');
                $('#validateButton').removeAttr('disabled');
                $('#backButton').removeAttr('disabled');
            }
        });
        $('#report').empty();
    });
    $('#validateButton').button({
        icons: {
            primary: "ui-icon-star"
        }
    }).on("click", function(event) {
        "use strict";
        event.preventDefault();
        $.ajax({
            dataType: "json",
            type: "GET",
            url: $.jummp.createLink("annotation", "validate"),
            cache: false,
            data: {
                annotations: JSON.stringify(Jummp.data.existingAnnotations),
                revision: "${revision.model.publicationId ?: revision.model.submissionId}"
            },
            error: function(jqXHR) {
                console.error("epic fail", jqXHR.responseText);
                $("#message").removeClass("success");
                $("#message").removeClass("jummpWarning");
                $("#message").addClass("failure");
                $('#message').html("There was an internal error while validating the information provided.");
            },
            success: function(response) {
                if ("200" == response.status) {
                    $("#message").removeClass("failure");
                    $("#message").removeClass("jummpWarning");
                    $("#message").addClass("success");
                } else {
                    $("#message").removeClass("success");
                    $("#message").removeClass("jummpWarning");
                    $("#message").addClass("failure");
                }
                $('#message').html(response.message);
                if(response.errorReport != null) {
                    $('#report').html(response.errorReport);
                } else {
                    $('#report').empty();
                }
            }
        });
    });
    $('#backButton').button({
        icons: {
            primary: "ui-icon-close"
        }
    });
</g:javascript>
<script>
    /* display all options at all times by setting the size of the select box */
    var selects = $('select');
    var selectCount = selects.length;
    var i;
    for(i = 0; i < selectCount; i++) {
        var thisSelect = selects[i];
        var actualLength = thisSelect.length;
        thisSelect.setAttribute('size', "" + actualLength);
    }

</script>
</body>
<content tag="contexthelp">
    annotate
</content>

