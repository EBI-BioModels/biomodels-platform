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
        font-size: 16px;
    }
</style>

<script type="text/javascript">
    $(window).on("resize", function () {
        showOrHidePublicationInfo();
        setTwitterHeight();
    });
    $(document).ready(function () {
        showOrHidePublicationInfo();
        setTwitterHeight();
    });

    $(window).on('load', function() {
        setTwitterHeight();
    });

    function setTwitterHeight() {
        /**
         * Because D3 charts are automatically responsive so that
         * we set the height of Twitter widget equal to theirs.
         * @type {*|jQuery}
         * See the solution was posted here https://stackoverflow.com/a/18390628/865603
         */
        //setTwitterHeight();
        if ($('.twitter-timeline').length) {
            //Timeline exists is it rendered ?
            interval_timeline = false;
            interval_timeline = setInterval(function(){
                if ($('.twitter-timeline').hasClass('twitter-timeline-rendered')) {
                    clearInterval(interval_timeline);
                    var chartAreaHeight = $('.chart-placeholder').height();
                    $('#twitter-widget-0').height(chartAreaHeight);
                }
            }, 50);
        }
    }
    function showOrHidePublicationInfo() {
        var innerWidth = window.innerWidth;
        if (innerWidth < 2000 || innerWidth > 2100) {
            $('.publication-info').hide();
        } else {
            $('.publication-info').show();
        }
    }
</script>
<div id="hp-intro" class="row text-center top-widget-area">
    <div class="large-12 medium-12 small-12 columns">
    <p class="welcome-message" style="margin-top: 0.5em;">
        BioModels is a repository of mathematical models of biological and biomedical systems.
        It hosts a vast selection of existing literature-based physiologically and pharmaceutically
        relevant mechanistic models in standard formats. Our mission  is to provide the systems
        modelling community with reproducible, high-quality,
        <a href="http://creativecommons.org/publicdomain/zero/1.0/" target="_blank">freely-accessible</a>
        models published in the scientific literature. More information about using BioModels such as <a
        href="faq#submit-model"
        class="tag">model submission</a> <a href="faq#update-existing-model"
        class="tag">update</a> <a href="faq#access-after-submission"
        class="tag">publication</a> can be found in the <a href="faq">FAQ</a>.
    </p></div>
</div>
<div id="hp-features" class="row text-center top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-features"/>
</div>
<div id="hp-row-1" class="row text-center top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-row-1"/>
</div>
<div id="hp-row-2" class="row top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-row-2"/>
</div>
<div id="hp-last-row" class="row top-widget-area">
    <g:render template="/templates/biomodels/homePage/hp-row-3"/>
</div>


