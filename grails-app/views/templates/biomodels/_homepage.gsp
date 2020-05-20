<style type="text/css">
    .welcome-message {
        text-align: justify;
    }
    div.homepage_info_box {
        text-align: center;
        background-color: #007c82;
        border-bottom: 0 none;
        border-top-left-radius: 10px;
        border-top-right-radius: 10px;
        padding: 5px 0 3px 0 !important;
    }
    div.homepage_info_box h3 {
        color: white !important;
        font-weight: bold;
    }
    div#acknowledgements a:visited {
        border-bottom-width: 0px;
        border-bottom-style: none;
        border-bottom-color: inherit;
    }
    #acknowledgements img {
        max-width: 100%;
        height: auto;
    }
    .top-widget-area {
        padding: 1.75em 0 .5em;
    }
    #hp-features a, a:visited {
        border-bottom-style: none;
    }
    .widget-body-text {
        font-size: 89%;
    }
</style>

<div id="hp-intro" class="row text-center top-widget-area">
    <div class="large-12 medium-12 small-12 columns">
    <p class="welcome-message" style="margin-top: 0.5em;">
        BioModels is a repository of mathematical models of biological and biomedical systems.
        It hosts a vast selection of existing literature-based physiologically and pharmaceutically
        relevant mechanistic models in standard formats. Our mission  is to provide the systems
        modelling community with reproducible, high-quality,
        <a href="http://creativecommons.org/publicdomain/zero/1.0/" target="_blank">freely-accessible</a>
        models published in the scientific literature. More information about BioModels can be found
        in the <a href="faq">FAQ</a>.
    </p></div>
</div>
<div id="hp-features" class="row text-center top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-features"/>
</div>
<div id="hp-statistics" class="row text-center top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-statistics"/>
</div>
<div id="hp-updates-mom" class="row top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-updates-mom"/>
</div>
<div id="hp-citations-acknowledgements-tweets" class="row top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-last-row"/>
</div>


