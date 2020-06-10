<div class="small-12 medium-16 large-4 columns">
    <div class="homepage_info_box"><h3>Recently accessed</h3></div>
    <div class="widget-body-text"><biomd:renderRecentlyAccessedModels/></div>
</div>
<div class="small-12 medium-12 large-4 columns">
    <div class="homepage_info_box"><h3>Documentation</h3></div>
    <div class="widget-body-text">
        <div class="row">
            <div class="small-12 medium-1 large-1 columns hide-for-small-only">
                <i class="icon icon-common icon-question-circle" style="font-size: 40px"></i>
            </div>
            <div class="small-12 medium-11 large-11 columns">
                <h4>Frequently Asked Questions (FAQs)</h4>
                <ul>
                    <li>How to browse and search models in BioModels?</li>
                    <li>How to submit a model?</li>
                    <li>Can I submit a model before it is described in a published paper?</li>
                    <li><a href="${g.createLink(controller: 'jummp', action: 'faq')}">Read more</a></li>
                </ul>
            </div>
        </div>

        <div class="row">
            <div class="small-12 medium-1 large-1 columns hide-for-small-only">
                <i class="icon icon-common icon-documentation" style="font-size: 40px"></i>
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
                <i class="icon icon-common icon-tutorial" style="font-size: 40px"></i>
            </div>
            <div class="small-12 medium-11 large-11 columns">
                <h4>Courses</h4>
                <p>BioModels provides online courses, please consult <a href="${g.createLink(controller: 'jummp', action: 'courses')}">more information</a>.</p>
            </div>
        </div>
    </div>
</div>
<div class="small-12 medium-12 large-4 columns">
    <biomd:renderNewsWidget/>
</div>
