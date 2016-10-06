var Jummp = Jummp || {};

Jummp.removeRdfStatement = function(statementMap) {
    if (undefined == statementMap) return;
    var subject = statementMap.subject || '*';
    var predicate = statementMap.predicate || '*';
    var object = statementMap.object || '*';

    if ('*' == subject) {
        //delete all subjects
        Jummp.data.existingAnnotations.subjects = {};
        return;
    }
    if (!Jummp.data.existingAnnotations.subjects.hasOwnProperty(subject)) {
        console.error("cannot find statements with subject '", subject, "'. Use '*' if you wish to delete all statements.");
        return;
    }

    if ('*' == predicate) {
        //delete all predicates for the given subject
        delete Jummp.data.existingAnnotations.subjects[subject];
        return;
    }

    targetPredicate = _.find(
            Jummp.data.existingAnnotations.subjects[subject].predicates,
            function(p) { return p.predicate == predicate; }
    );

    if (!targetPredicate) {
        console.error("Could not find RDF statement with subject ", subject, "and predicate", predicate);
        return;
    }

    if ('*' == object) {
        //remove the predicate
        Jummp.data.existingAnnotations.subjects[subject].predicates = _.reject(
                Jummp.data.existingAnnotations.subjects[subject].predicates,
                function(p) { return p.predicate == predicate; }
        );
        return;
    }

    var targetObjects = _.find(targetPredicate.object, function(o) {
        return o.object == object;
    });

    if (!targetObjects) {
        console.error("triple (", subject, predicate, object, ") did not match any existing annotations.");
        return;
    }

    //delete all objects for the given subject and predicate
    targetPredicate.object = _.reject(targetPredicate.object, function(o) {
        if( o.object == object) {
            return true;
        }
    });
    // put the updated predicate in Jummp.data
    Jummp.data.existingAnnotations.subjects[subject].predicate = s_.reject(
            Jummp.data.existingAnnotations.subjects[subject].predicates,
            function(p) { return p.predicate == targetPredicate.predicate; }
    );
    Jummp.data.existingAnnotations.subjects[subject].predicate.push(targetPredicate);
};

Jummp.addRdfStatement = function(subject, predicate, object) {
    if (undefined === Jummp.data.existingAnnotations.subjects.theSubject.predicates) {
        Jummp.data.existingAnnotations.subjects.theSubject.predicates = []
    }
    var existing = Jummp.data.existingAnnotations;
    var existingSubject = existing.subjects.theSubject || {};
    var subjectPredicates = existingSubject.predicates || [];
    var predicateIdx = _.findIndex(subjectPredicates, { predicate: predicate });
    if (!object) {
        if (-1 != predicateIdx) {
            var removed = Jummp.data.existingAnnotations.subjects.theSubject.predicates.splice(
                    predicateIdx, 1);
            return;
        } else {
            return;
        }
    }
    var thisPredicate;
    if (-1 == predicateIdx) {
        thisPredicate = {
            predicate: predicate,
            object: object
        };
        Jummp.data.existingAnnotations.subjects.theSubject.predicates.push(thisPredicate);
    } else {
        thisPredicate = subjectPredicates[predicateIdx];
        if (!thisPredicate) {
            return;
        }
        thisPredicate.object = object;
        Jummp.data.existingAnnotations.subjects.theSubject.predicates[predicateIdx] = thisPredicate;
    }
};

Jummp.convertToRdfObject = function(item) {
    return {
        "object": item
    };
};

Jummp.annoEditor = new Mn.Application();
Jummp.annoEditor.addRegions({
    mainRegion: "#leftContainer"
});

Jummp.annoEditor.on("start", function() {
    Backbone.history.start();
});

var AnnotationSection = Backbone.Model.extend({
    defaults: {
        name: 'example',
        id: 'ex'
    }
});

var AnnotationSections = Backbone.Collection.extend({ model: AnnotationSection });

var AnnotationProperty = Backbone.Model.extend({});

var AnnotationProperties = Backbone.Collection.extend({ model: AnnotationProperty });

var Value = Backbone.Model.extend({});
var Values = Backbone.Collection.extend({ model: Value });

var ValueContainerView = Mn.ItemView.extend({
    getTemplate: function() {
        var range = this.options.property.get("range").name;
        switch(range) {
            case "SINGLE_UNCONSTRAINED_CHOICE":
            case "MULTIPLE_UNCONSTRAINED_CHOICE":
                return "#unconstrained-value-template";
            case "SINGLE_CONSTRAINED_CHOICE":
                return "#single-constrained-value-template";
            case "MULTIPLE_CONSTRAINED_CHOICE":
                return "#multiple-constrained-value-template";
            default:
                var pName = p.get('value');
                console.warn("Sorry, I don't know how to display property", pName, "with range", range);
                console.warn("Rendering", pName, "as a free text field.");
                return "#single-unconstrained-value-template";
        }
    },

    ui: {
        multipleConstrainedValueOption: "select",
        singleUnconstrainedValueOption: "input[type=text]",
        singleConstrainedValueOption:   "input[type=radio]",
        clearSingleConstrainedChoice:   ".clearRadioGroup"
    },

    events: {
        "change @ui.multipleConstrainedValueOption": "multipleConstrainedValueChanged",
        "focusout @ui.singleUnconstrainedValueOption": "singleUnconstrainedValueChanged",
        "change @ui.singleConstrainedValueOption": "singleConstrainedValueChanged",
        "click @ui.clearSingleConstrainedChoice": "clearRadioGroup"
    },

    singleUnconstrainedValueChanged: function() {
        // get the value from the text box
        var newValue = this.$el.children().get(0).value;
        //  get the qualifier uri
        var uri = this.options.property.get('uri');
        var rdfObject;
        if (!newValue) {
            rdfObject = newValue;
        } else {
            rdfObject = [Jummp.convertToRdfObject(newValue)]
        }
        Jummp.addRdfStatement("theSubject", uri, rdfObject);
    },

    multipleConstrainedValueChanged: function() {
        var uri = this.options.property.get('uri');
        var selectedOptions = this.$el.find('select option:selected');
        var objects = _.map(selectedOptions, function(option) {
            return Jummp.convertToRdfObject(option.value);
        });
        Jummp.addRdfStatement("theSubject", uri, objects);
    },

    singleConstrainedValueChanged: function() {
        var p = this.options.property.get("uri");
        var v = this.$el.find('input[type=radio]:checked')[0].value;
        Jummp.addRdfStatement("theSubject", p, [Jummp.convertToRdfObject(v)]);
        // re-enable the button to clear selection
        this.options.disabled = false;
    },

    clearRadioGroup: function() {
        if (true === this.options.disabled) {
            return;
        }
        var uri = this.options.property.get("uri");
        Jummp.removeRdfStatement({ subject: "theSubject", predicate: uri});
        this.collection.each(function(o) {
            var shouldToggle = o.get("isSelected");
            if (shouldToggle) {
                o.set("isSelected", false);
            }
        });
        // ensure subsequent calls don't try to unset something that isn't there.
        this.options.disabled = true;
        this.render();
    }
});

var PropertyView = Mn.LayoutView.extend({
    template: "#property-template",
    initialize: function() {
        this.addRegion("valueContainerRegion", this.$el.find(".valueContainer"));
    },
    onShow: function() {
        var view = new ValueContainerView({
                collection: this.model.get("values"),
                property: this.model
        });
        this.valueContainerRegion.show(view);
    }
});

var PropertiesView = Mn.CollectionView.extend({
    template: "#properties-template",
    childView: PropertyView
});

// holds the contents of one tab (a section)
var SectionView = Mn.LayoutView.extend({
    template: "#section-template",
    className: "sectionContainer",
    tagName: "div",

    initialize: function(options) {
        this.addRegion("propertyRegion", this.$el.find(".properties"));
    },

    onShow: function() {
        var properties = new PropertiesView({
            collection: this.model.get('annotationProperties')
        });
        this.propertyRegion.show(properties);
    }
});

// container for sections
var SectionsView = Mn.CollectionView.extend({
    template: "#sections-template",
    childView: SectionView
});

// represents one jQuery tab heading
var TabHeadingView = Mn.ItemView.extend({
    template: "#tab-heading-template",
    tagName: "li"
});

// holds the jQuery UI tab headings
var TabHeadingContainerView = Mn.CollectionView.extend({
    template: "#tab-headings-template",
    childView: TabHeadingView,
    tagName: "ul"
});

// the main layout consisting of the tabHeadingContainer and the sectionsView
var TabbedLayout = Mn.LayoutView.extend({
    template: "#root-section-template",
    annotationSections: [],

    initialize: function(options) {
        var options = options || {};
        this.annotationSections = options.annotationSections;
        this.addRegion("tabHeadingRegion", this.$el.find("#tabHeadings"));
        this.addRegion("sectionRegion", this.$el.find("#tabContent"));
    },

    onShow: function() {
        var headings = new TabHeadingContainerView({ collection: this.annotationSections });
        this.tabHeadingRegion.show(headings);
        var contents = new SectionsView({collection: this.annotationSections});
        this.sectionRegion.show(contents);
    }
});

Marionette.TemplateCache.prototype.compileTemplate = function(rawTemplate, options) {
    return Handlebars.compile(rawTemplate);
};

$(function() {
    $("#leftContainer").tabs();
});

Handlebars.registerHelper({
    // formats the value that should appear in the text field of a
    // SINGLE_UNCONSTRAINED_CHOICE property.
    getLabelForUnconstrainedChoice: function(item) {
        return item.value ? item.value : item.uri ? item.uri : "";
    },

    getNameForRadioButton: function(item) {
        return item.annotationProperty.get("value");
    },

    getRadioButtonId: function(item, index) {
        var safePropertyName = item.annotationProperty.get("value").replace(/[^A-Za-z]/g, '');
        return safePropertyName + "-" + index;
    },

    // builds a select box that allows multiple choice. nested values will appear indented.
    renderMultipleConstrainedValues: function() {
        var values = this.items;
        var result = "<select multiple >";
        _.each(values, function(value) {
            // wrap inside Value so that we are consistent with the representation of child values
            result = Jummp.buildValueTree(new Value(value), result);
        });
        result += "</select>";
        var escapedResult = new Handlebars.SafeString(result);
        return escapedResult;
    }
})

Jummp.buildValueTree = function(item, partialValueTree, isLeaf) {
    partialValueTree = partialValueTree || "";
    isLeaf = isLeaf || false;
    var isSelected = item.get("isSelected") || false;
    var optionValue = item.get("uri");
    var annotationLabel = item.get("value");
    var subValues = item.get("children") || [];

    var thisItem = "<option value='" + optionValue + "'";
    if (isSelected) {
        thisItem += " selected='selected'";
    }
    if (isLeaf) {
        annotationLabel = "&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;" + annotationLabel;
    }
    thisItem += ">" + annotationLabel + "</option>\n";
    partialValueTree += thisItem;
    if (subValues.length > 0) {
        subValues.each(function(child) {
            partialValueTree = Jummp.buildValueTree(child, partialValueTree, true);
        });
    }
    return partialValueTree;
};

