const size = {
    width: window.innerWidth || document.body.clientWidth,
    height: window.innerHeight || document.body.clientHeight
};

let global_diagram;
let reactomeId = "";
let base_height = Math.floor(size.height / 2);
let base_width = Math.floor(size.width / 2);
const REACTOME_HEIGHT = base_height;
const REACTOME_WEIGHT = base_width;
const DIALOG_WEIGHT = base_width + 100;
const DIALOG_HEIGHT = base_height + 150;

$(document).ready(function () {
    $("#reactome-dialog").dialog({
        width: DIALOG_WEIGHT,
        height: DIALOG_HEIGHT,
        modal: true,
        open: function (event, ui) {
            event.preventDefault();
            global_diagram.resize(REACTOME_WEIGHT, REACTOME_HEIGHT);
            global_diagram.resetSelection();
            global_diagram.selectItem(reactomeId);
        },
        autoOpen: false,
        show: {
            effect: "blind",
            duration: 1000
        },
        hide: {
            effect: "explode",
            duration: 1000
        }
    });
});

$("#opener").on("change", function () {
    setReactomeId(this.value);

    if (reactomeId) {
        loadDiagram();
        openDialogBox();
    }
});
// Creating the Reactome Diagram widget
// Take into account a proxy needs to be set up in your server side pointing to www.reactome.org
function loadDiagram() {
    const diagram = Reactome.Diagram.create({
        "placeHolder": "diagramHolder",
        "width": REACTOME_WEIGHT,
        "height": REACTOME_HEIGHT
    });
    diagram.loadDiagram(reactomeId);

    // store this in a global variable, so we can call resetSelection() from
    // the callback for opening the Reactome popup. Calling it here results
    // in a popup window with an invisible pathway, even though the widget
    // control buttons are rendered just fine.
    // Adding different listeners
    global_diagram = diagram;

    diagram.onObjectHovered(function (hovered) {
        console.info("Hovered ", hovered);
    });

    diagram.onObjectSelected(function (selected) {
        console.info("Selected ", selected);
    });
}

function openDialogBox() {
    // the element below is defined in the modelDisplay layout
    $("#reactome-dialog").dialog("open");
}

function setReactomeId(_reactomeId) {
    reactomeId = _reactomeId;
}

