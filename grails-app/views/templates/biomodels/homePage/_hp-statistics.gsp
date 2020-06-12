<style type="text/css">
    svg {
        margin: 0 auto;
        display: inline-block;
    }
    .div-center-content {
        text-align: center;
        width: 100%;
    }
    .orbit-bullets button.is-active {
        background-color: #008080;
    }
</style>
<script>
    var donutWidth = 120;
    var margin = {top: 10, right: 10, bottom: 10, left: 10};
    var width = 600,
        height = 600,
        diameter = 600,
        radius = Math.min(width, height) / 2 - margin.bottom,
        default_ratio = width/height;
    var slideDesc = {
        1: {
            name: "modellingApproach",
            title: "Browse by Modelling Approach",
            description:
                "This shows models distribution based on modelling approaches. Click a slice to display models."
        },
        2: {
            name: "organism",
            title: "Browse by Organism",
            description: "This shows models distribution based on organisms. Click a bubble to display models."
        },
        3: {
            name: "journal",
            title: "Browse by Journal",
            description:
                "This shows models distribution based on journals. Move the mouse over a bubble to see the information."
            /*Click a bubble to display models.
            TODO: index publication journals and turn it searchable, then implement searching on this field */
        }
    };
    function setTabActive(current) {
        /*var allTabs = '.tabs li';
        var allAssocLinks = '.tabs li a';
        $(allTabs).removeClass('is-active');
        $(allAssocLinks).attr('aria-selected', false);

        var currentTab = $(allTabs).find("li[data-slide='" + current + "']".toString());
        console.log(currentTab);
        $(currentTab).addClass('is-active');
        var currentLink = $(allAssocLinks).find("a[data-slide='" + current + "']".toString());
        console.log(currentLink);
        $(currentLink).attr('aria-selected', true);*/
    }
    function slideNumber() {
        var $slides = $('.orbit-slide');
        var $activeSlide = $slides.filter('.is-active');
        var activeNum = $slides.index($activeSlide) + 1;
        $('.slide-number').innerHTML = activeNum;
        // console.log(activeNum);
        // Change description
        // console.log(slideDesc[activeNum]);
        $('#slide-introduction #slide-title').html(slideDesc[activeNum]["title"]);
        $('#slide-introduction #slide-description').html(slideDesc[activeNum]["description"]);
        // console.log(activeNum);
        //setTabActive(activeNum);
    }
    document.addEventListener("DOMContentLoaded", function () {
        slideNumber();
        $('[data-orbit]').on('slidechange.zf.orbit', slideNumber);
    });
</script>
<!-- https://get.foundation/sites/docs/orbit.html -->
<!-- For animation on slide shows -->
<link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/motion-ui@1.2.3/dist/motion-ui.min.css" />

%{--<ul class="tabs hide-for-small-only" data-tabs id="model-browse-tabs">
    <li class="tabs-title is-active" data-slide="1">
        <a href="#panel1" data-slide="1" aria-selected="true">Curation States</a></li>
    <li class="tabs-title" data-slide="2">
        <a href="#panel2" data-slide="2">Modelling Approaches</a></li>
    <li class="tabs-title" data-slide="3">
        <a href="#panel3" data-slide="3">Organisms</a></li>
    <li class="tabs-title" data-slide="4">
        <a href="#panel4" data-slide="4">Journals</a></li>
</ul>
<div class="tabs-content hide-for-small-only" data-tabs-content="model-browse-tabs">
    <div class="tabs-panel is-active" id="panel1">
        <p>Curation States Chart</p>
    </div>
    <div class="tabs-panel" id="panel2">
        <p>Modelling Approaches Chart</p>
    </div>
    <div class="tabs-panel" id="panel3">
        <p>Organisms Chart</p>
    </div>
    <div class="tabs-panel" id="panel4">
        <p>Journals Chart</p>
    </div>
</div>--}%
%{--https://www.ebi.ac.uk/style-lab/websites/patterns/tabs.html--}%
<div id="item" class="large-4 medium-12 small-12 columns chart-placeholder">
    <div id="slide-introduction">
        <div class="homepage_info_box"><h3 id="slide-title"></h3></div>
        <p><span id="slide-description"></span><br/>
            <span id="item-on-focus" style="font-weight: bold"></span></p>
    </div>
    <div class="orbit" role="region" aria-label="Favorite Statistics" data-orbit
         data-options="autoPlay:true; animInFromLeft:fade-in; animInFromRight:fade-in; animOutToLeft:fade-out; animOutToRight:fade-out;">
        <ul class="orbit-container">
            <button class="orbit-previous" aria-label="previous" style="background-color: #008080">
                <span class="show-for-sr">Previous Slide</span>&#9664;</button>
            <button class="orbit-next" aria-label="next" style="background-color: #008080">
                <span class="show-for-sr">Next Slide</span>&#9654;</button>
            <li class="is-active orbit-slide">
                <biomd:renderHomePageStatisticsModellingApproaches/>
            </li>
            <li class="orbit-slide">
                <biomd:renderHomePageStatisticsOrganisms/>
            </li>
            <li class="orbit-slide">
                <biomd:renderHomePageStatisticsJournals/>
            </li>
        </ul>
        <nav class="orbit-bullets">
            <button class="is-active" data-slide="0">
                <span class="show-for-sr">First slide details.</span>
                <span class="show-for-sr">Current Slide</span></button>
            <button data-slide="1"><span class="show-for-sr">Second slide details.</span></button>
            <button data-slide="2"><span class="show-for-sr">Third slide details.</span></button>
        </nav>
    </div>
</div>
<div class="large-4 medium-12 small-12 columns">
    <div id="mom-right-entry">
        <div class="homepage_info_box"><h3>Model of The Month
            <a href="//www.ebi.ac.uk/biomodels/modelOfTheMonth/rss" style="margin-right:0.25em"
               class="icon icon-socialmedia float-right no-underline" data-icon="R"></a></h3></div>
        <div class="widget-body-text"><biomd:renderTheLatestMoMEntryWidget/></div>
    </div>
</div>
<div id="hp-statistics-published-recently" class="large-4 medium-12 small-12 columns">
    <div class="homepage_info_box"><h3>Recently published</h3></div>
    <div class="widget-body-text" style="text-align: left"><biomd:renderRecentlyPublishedModels/></div>
</div>
