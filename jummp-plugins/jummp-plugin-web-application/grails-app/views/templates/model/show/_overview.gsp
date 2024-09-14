<div class="row">
    <div class="small-12 medium-8 large-8 columns">
        <div class="row">
            <div class="small-12 medium-2 large-2 columns">
                <span class="overview-tab-attribute">Model Identifier</span>
            </div>
            <div class="small-12 medium-10 large-10 columns">
                ${revision.modelIdentifier()}
            </div>
        </div>
        <div class="row">
            <div class="small-12 medium-2 large-2 columns">
                <jummp:displayModelDescriptionLabel>
                    <span class="overview-tab-attribute">${description}</span>
                </jummp:displayModelDescriptionLabel>
            </div>
            <div class="small-12 medium-10 large-10 columns">
                <div id="description">
                    ${raw(revision.description)}
                </div>
            </div>
        </div>
        <div class="row">
            <div class="small-12 medium-2 large-2 columns">
                <span class="overview-tab-attribute"><g:message code="model.model.format"/></span>
            </div>
            <div class="small-12 medium-10 large-10 columns">
                ${revision.format.name}
                ${revision.format.formatVersion!="*"?"(${revision.format.formatVersion})":""}
            </div>
        </div>
        <g:render template="/templates/renderPublication"/>
        <g:render template="/templates/renderContributors" model="${contributors}"/>
    </div>

    <div class="small-12 medium-4 large-4 columns">
        <div class="row rounded-header"><h4 style="color: #ffffee">Metadata information</h4></div>
        <g:if test="${genericAnnotations}">
            <anno:renderGenericAnnotations annotations="${genericAnnotations}"/>
        </g:if>
        <g:if test="${curationState}">
            <biomd:insertSectionSeparator/>
            <div class='row'>
                <div class="small-12 medium-6 large-4 columns">Curation status</div>
                <div class="small-12 medium-6 large-8 columns">
                    <g:if test="${canUpdate && hasCuratorRole && curationNotes != null}">
                        <label for="curation_state_change"></label><select id="curation_state_change">
                        <g:each in="${possibleCurationStates}" var="possibleCurationState">
                            <g:if test="${possibleCurationState == curationState}">
                                <option value="${possibleCurationState}" selected>
                                    <jummp:camelCase message="${possibleCurationState}" />
                                </option>
                            </g:if>
                            <g:else>
                                <option value="${possibleCurationState}">
                                    <jummp:camelCase message="${possibleCurationState}" />
                                </option>
                            </g:else>
                        </g:each>
                    </select>
                    </g:if>
                    <g:else>
                        <jummp:camelCase message="${curationState}" />
                    </g:else>
                </div>
            </div></g:if>
        <g:if test="${modellingApproaches}">
            <biomd:insertSectionSeparator/>
            <div class='row'>
                <div class="small-12 medium-6 large-4 columns">Modelling approach(es)</div>
                <div class="small-12 medium-6 large-8 columns">
                    <biomd:renderModellingApproaches modellingApproaches="${modellingApproaches}"/>
                </div>
            </div></g:if>
        <g:if test="${originalModels}">
            <biomd:insertSectionSeparator/>
            <div class='row'>
                <div class="small-12 medium-6 large-4 columns">Original model(s)</div>
                <div class="small-12 medium-6 large-8 columns">
                    <biomd:renderOriginalModels sources="${originalModels}"/></div>
            </div></g:if>
    <!-- Show all tags assigned to the model -->
        <biomd:insertSectionSeparator/>
        <g:if test="${bmTags}">
            <g:if test="${canUpdate && hasCuratorRole}">
                <biomd:showEditableTags bmTags="${bmTags}"/>
            </g:if>
            <g:else>
                <biomd:showTags bmTags="${bmTags}"/>
            </g:else>
        </g:if>
        <g:else>
            <g:if test="${canUpdate && hasCuratorRole}">
                <biomd:showEditableTags bmTags="${bmTags}"/>
            </g:if>
        </g:else>
    <!-- Render a disclaimer if the model has been published without a publicly available manuscript -->
        <g:if test="${shouldDisplayDisclaimer}">
            <biomd:displayDisclaimer revision="${revision}"/></g:if>
        <div class="row rounded-header" style="margin-top: 1.0em"><h4 style="color: #ffffee">Connected external resources</h4></div>
        <g:if test="${true}">
            <biomd:renderLinkOmicsDiRosette /></g:if>
        <g:if test="${hrefLinkToNewtEditor}">
            <biomd:renderLinkToNewtEditor serverURL="${serverURL}"
                                          hrefLinkToNewtEditor="${hrefLinkToNewtEditor}"/>
        </g:if>
        <biomd:doRenderOrAddGalaxyLink hasGalaxyLink="${hasGalaxyLink}" serverURL="${serverURL}"
                                       modelId="${revision.modelIdentifier()}"
                                       canAddGalaxyLink="${canAddGalaxyLink}"/>
        %{--<div class='row'>
            <div class="medium-3 columns">Validation Status</div>
            <div class="medium-9 columns">${validationLevel}</div>
        </div>
        <div class='row'>
            <div class="medium-3 columns">Certification Comment</div>
            <div class="medium-9 columns">${certComment}</div>
        </div>--}%
    </div>
</div>
