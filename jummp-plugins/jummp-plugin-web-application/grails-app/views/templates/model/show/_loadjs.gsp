<%@ page import="net.biomodels.jummp.core.constants.BioModels"%>
<link rel="alternate" href="https://identifiers.org/biomodels.db/${revision.modelIdentifier()}"/>
<link rel="alternate" href="https://www.ebi.ac.uk/biomodels-main/${revision.modelIdentifier()}"/>
<link rel="alternate" href="https://www.ebi.ac.uk/biomodels-main/${revision.modelIdentifier()}"/>
<link rel="canonical" href="${BioModels.BM_ROOT_URL}/${revision.modelIdentifier()}"/>
<link rel="stylesheet" href="${resource(contextPath: serverURL, dir: 'css', file: 'jquery.handsontable.full.min.css')}"/>
<link rel="stylesheet" href="${resource(contextPath: serverURL, dir: 'css/syntax', file: 'shCore.css')}"/>
<link rel="stylesheet" href="${resource(contextPath: serverURL, dir: 'css/syntax', file: 'shThemeDefault.css')}"/>
<link rel="stylesheet" href="${resource(contextPath: serverURL, dir: 'css', file: 'toastr.min.css')}"/>
<link rel="stylesheet" href="${resource(contextPath: serverURL, dir: 'css', file: 'model-display.css')}"/>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/css/select2.min.css"/>
<script src="https://cdnjs.cloudflare.com/ajax/libs/select2/4.0.5/js/select2.min.js"></script>
<script type="text/x-mathjax-config">
    MathJax.Hub.Config({
        tex2jax: { inlineMath: [['$','$'],['\\(','\\)']] }
    });
</script>
<script type="text/javascript"
        src="${serverURL}/js/MathJax-2.6.1/MathJax.js?config=TeX-AMS-MML_HTMLorMML">
</script>
<script src="${resource(contextPath: serverURL, dir: 'js/syntax', file: 'shCore.js')}"></script>
<script src="${resource(contextPath: serverURL, dir: 'js/syntax', file: 'shBrushMdl.js')}"></script>
<script src="${resource(contextPath: serverURL, dir: 'js/syntax', file: 'shBrushXml.js')}"></script>
<script src="${resource(contextPath: serverURL, dir: 'js', file: 'toastr.min.js')}"></script>
<script src="${resource(contextPath: serverURL, dir: 'js', file: 'jquery.handsontable.full.js')}"></script>
<script src="${resource(contextPath: serverURL, dir: 'js/biomodels', file: 'omicsdi.service.js')}"></script>
<script type="text/javascript" src="https://d3js.org/d3.v4.min.js"></script>
<script  defer="defer" type="text/javascript" language="javascript" src="${reactomeUrl}"></script>
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
<script type="text/javascript">
    $(document).ready(function() {
        $('.model-tags-select2').select2({
            placeholder: "Type here to search a tag",
            tags: false,
            multiple: true
        });

        SyntaxHighlighter.all();
    });
</script>
