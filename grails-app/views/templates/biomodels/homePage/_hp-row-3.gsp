<div class="small-12 medium-12 large-4 columns">
    <div class="homepage_info_box">
        <h3>How to cite BioModels</h3>
    </div>
    <div class="widget-body-text">
        <dl>
            <dt class="pubauthors">Rahuman S Malik-Sheriff, Mihai Glont, Tung V N Nguyen, Krishna Tiwari,
            Matthew G Roberts, Ashley Xavier, Manh T Vu, Jinghao Men, Matthieu Maire, Sarubini Kananathan,
            Emma L Fairbanks, Johannes P Meyer,
            Chinmay Arankalle, Thawfeek M Varusai, Vincent Knight-Schrijver, Lu Li, Corina Dueñas-Roca, Gaurhari Dass,
            Sarah M Keating, Young M Park, Nicola Buso, Nicolas Rodriguez, Michael Hucka, and Henning Hermjakob</dt>
            <dd>
                <div class="pubtitle"><a href="https://academic.oup.com/nar/article/48/D1/D407/5614569"
                                         target="_blank">
                    BioModels — 15 years of sharing computational models in life science.</a></div>

                <div class="pubjournal"><em>Nucl. Acids Res.</em> 2020</div>
            </dd>
        </dl>
        <dl>
            <dt class="pubauthors">
                Mihai Glont, Tung V N Nguyen, Martin Graesslin, Robert Hälke, Raza Ali, Jochen Schramm,
                Sarala M Wimalaratne, Varun B Kothamachu, Nicolas Rodriguez, Maciej J Swat, Jurgen Eils,
                Roland Eils, Camille Laibe, Rahuman S Malik-Sheriff, Vijayalakshmi Chelliah, Nicolas Le Novère,
                and Henning Hermjakob
            </dt>
            <dd>
                <div class="pubtitle"><a href="https://academic.oup.com/nar/article/46/D1/D1248/4584626" target="_blank">
                    BioModels: expanding horizons to include more modelling approaches and formats.</a>
                </div>
                <div class="pubjournal">Nucl. Acids Res. 2018</div>
            </dd>
        </dl>
        <a href="${g.createLink(controller: 'jummp', action: 'howToCiteBioModelsDatabase')}">Read more...</a>
    </div>
</div>

<div class="small-12 medium-12 large-4 columns">
    <div class="homepage_info_box"><h3>Documentation</h3></div>
    <div class="widget-body-text">
        <div class="row">
            <div class="small-12 medium-1 large-1 columns hide-for-small-only">
                <i class="icon icon-common icon-question-circle" style="font-size: 2em"></i>
            </div>
            <div class="small-12 medium-11 large-11 columns">
                <h4>Frequently Asked Questions (FAQs)</h4>
                <ul>
                    <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}#browse-models">
                        How to browse and search models in BioModels?</a></li>
                    <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}#model-submission">
                        How to submit a model?</a></li>
                    <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}#submit-before-paper">
                        Can I submit a model before it is described in a published paper?</a></li>
                    <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}">Read more</a></li>
                </ul>
            </div>
        </div>

        <div class="row">
            <div class="small-12 medium-1 large-1 columns hide-for-small-only">
                <i class="icon icon-common icon-documentation" style="font-size: 2em"></i>
            </div>
            <div class="small-12 medium-11 large-11 columns">
                <h4>Developers' Guide</h4>
                <ul>
                    <li>
                        <a href="${createLink(uri: '/docs', absolute: true)}"
                           target="_blank"
                           title="BioModels provides programmatic access to its content via RESTful Web Services Interface.">
                            RESTful Web Services API Documentation</a><br>
                    </li>
                    <li><a href="${grailsApplication.config.jummp.ws.client.japi.docs}" target="_blank"
                           title="The Java library provides a very convenient way to use a few web services endpoints requested by BioModels's existing clients.">Java based
                        API of RESTful Web Services</a><br/>
                    </li>
                    <li><a href="${createLink(controller: 'jummp', action: 'developerZone')}">Read more</a></li>
                </ul>
            </div>
        </div>

        <div class="row">
            <div class="small-12 medium-1 large-1 columns hide-for-small-only">
                <i class="icon icon-common icon-tutorial" style="font-size: 2em"></i>
            </div>
            <div class="small-12 medium-11 large-11 columns">
                <h4>Courses</h4>
                <p>BioModels provides online courses, please consult <a href="${g.createLink(controller: 'jummp', action: 'courses')}">more information</a>.</p>
            </div>
        </div>
    </div>
</div>

<div class="small-12 medium-12 large-4 columns">
    <div class="homepage_info_box">
        <h3>Acknowledgements</h3>
    </div>
    <div id="acknowledgements" class="widget-body-text">
        <dl>
            <dt  class="widget-body-text">
                BioModels is supported by the <a href="//www.embl.org/" class="external">European Molecular Biology Laboratory</a>,
            the <a href="https://www.bbsrc.ac.uk/" class="external">Biotechnology and Biological Sciences Research Council</a> (Multimod, BB/N019482/1) and
            the <a href="https//www.imi.europa.eu/" title="Innovative Medicines Initiative (IMI)">Innovative Medicines Initiative</a>
                (<a href="https://transqst.org/" title="This project has received funding from the Innovative Medicines Initiative 2 Joint Undertaking under grant agreement No 116030. This Joint Undertaking receives support from the European Union’s Horizon 2020 research and innovation programme and EFPIA.">TransQST, 116030</a>).
            </dt>
            <dd>
                <a href="https://www.embl.org/">
                    <img src="https://wwwdev.ebi.ac.uk/biomodels/static-assets/images/EMBL_logo_lo_res_0.jpg"
                         title="European Molecular Biology Laboratory (EMBL)" alt="EMBL logo" border="0"/></a>
                <a href="http://www.bbsrc.ac.uk/" title="Biotechnology and Biological Sciences Research Council (BBSRC)">
                    <img src="https://wwwdev.ebi.ac.uk/biomodels/static-assets/images/bbsrc_logo.png"
                         title="Biotechnology and Biological Sciences Research Council (BBSRC)" alt="BBSRC logo" /></a>
                <a href="https://www.imi.europa.eu/" title="Innovative Medicines Initiative (IMI)">
                    <img src="https://wwwdev.ebi.ac.uk/biomodels/static-assets/images/IMI_logo-small.png"
                         title="Innovative Medicines Initiative (IMI)" alt="IMI logo" /></a>
                <a href="https://cordis.europa.eu/fp7/" title="Seventh Framework Programme (FP7)">
                    <img src="https://wwwdev.ebi.ac.uk/biomodels/static-assets/images/FP7_logo-small.png"
                         title="Seventh Framework Programme (FP7)" alt="FP7 logo" /></a>
            </dd>
        </dl>

        <p class="padding-top-2">
            <a href="${g.createLink(controller: 'jummp', action: 'acknowledgements')}">Read more...</a>
        </p>
    </div>
</div>
