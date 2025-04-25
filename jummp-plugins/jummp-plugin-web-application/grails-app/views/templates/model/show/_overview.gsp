<%@ page import="grails.converters.JSON"%>
<% JSON tagsJSON = bmTags as JSON %>
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
        <g:if test="${hasRosetteLink}">
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

    $('.model-tags-select2').select2({
        placeholder: "Type here to search a tag",
        tags: false,
        multiple: true
    });
</g:javascript>
<script>
    $(document).ready(function() {
        $('.model-tags-select2').select2({
            placeholder: "Type here to search a tag",
            tags: false,
            multiple: true
        });

        const qualifiers = $("#all-qualifier-accessions").text().split(",");
        jQuery.each(qualifiers, (index, item) => {
            const pQualifier = $("#" + item + "Qualifier").text();
            let parts = pQualifier.split(",");
            parts = parts.filter(v => v !== '');
            $.each(parts, (i, e) => {
                if (i >= 1) {
                    $("#" + e).css("display", "none");
                } else if (parts.length >= 2) {
                    $('<a id="' + item + 'ShowMore" class="show-more">Show more...</a><br/>').insertAfter($("#" + e));
                } else {
                    $("#" + e).css("display", "block");
                }
            });
        });

        checkAndRenderOmicsDiRosette();
    });

    $(".each-qualifier-block").on("click", ".show-more", function() {
        // a <br/> tag is inserted between blocks of 5 elements
        const nextElement = $(this).next();
        nextElement.remove();
        const id = $(this).prop("id");
        const prevId = $(this).prev().prop("id");
        const qualAccession = id.substring(0, id.length - "ShowMore".length);
        const index = prevId.substring(qualAccession.length)
        let nextId = parseInt(index) + 1;
        const newEle = $("#" + qualAccession + nextId.toString())
        newEle.css("display", "block");
        $('<a id="' + qualAccession + 'ShowMore" class="show-more">Show more...</a><br/>').insertAfter(newEle);
        $(this).remove();
    });

    function checkAndRenderOmicsDiRosette() {
        // OmicsDI Service for showing rosette
        let isAvailable = ${hasRosetteLink}; //checkModelAvailability("${revision.modelIdentifier()}");
        if (isAvailable) {
            createRosette("${revision.modelIdentifier()}");
            $('.ext-rsc-text').attr('style', 'display:inline-block;');
        }
    }

    $("#curation_state_change").on('change', function () {
        const curationState = this.value;
        $.ajax({
            type: "PUT",
            url: $.jummp.createLink("model", "updateCurationState"),
            cache: false,
            dataType: 'json',
            headers: {
                "Content-Type": "application/json"
            },
            data: JSON.stringify({
                curationState: curationState,
                modelId: "${revision.model.submissionId}",
                revisionNumber: "${revision.revisionNumber}"
            }),
            beforeSend: function() {
                toastr.info('Updating curation status...');
            },
            error: function(jqXHR) {
                toastr.clear();
                toastr.error(jqXHR.responseText.message);
            },
            success: function(response) {
                toastr.clear();
                toastr.success(response.message);
                // Updating the curation state feature allows us to change
                // back and forth non-curated and curated without any problem. On top of that,
                // the update procedure considers the situation where the curation state of
                // the public model has been changed to curated. We need to generate
                // the publication identifier for such a model. Hence, the page is only refreshed if
                // we are changing the curation state of the non-curated public model from non-curated to
                // curated. The page will be redirected to itself with the newly-created publication
                // identifier that has been assigned to the model.
                const publicationId = response.publicationId;
                const criteria = publicationId !== null;
                if (criteria) {
                    let message = response.message;
                    message += ". Please wait a few seconds while the web page is being refreshed.";
                    $('.flashNotificationDiv').html(message).show();
                    const modelDisplayPage = $.jummp.createURI(publicationId);
                    <%-- force a redirect if a publication identifier was generated  --%>
                    <%-- Using setTimeout to fresh the web page with the newly-created publication identifier
                     after 5 seconds. --%>
                    setTimeout(function () {
                        $.jummp.openPage(modelDisplayPage);
                    }, 5000);
                }
            }
        });
    });

    $('#btnSaveTags').on("click", function (event) {
        "use strict";
        event.preventDefault();
        let updatedTags = getDataFromSelect2();
        if (initialTags.length === updatedTags.length && !initialTags.length) {
            toastr.clear();
            toastr.warning("No tag applied to the model. Alternatively, select at least one tag from the list.");
        } else {
            $.ajax({
                type: "POST",
                url: $.jummp.createLink("modelTag", "saveModelTag"),
                cache: true,
                async: true,
                processData: true,
                dataType: "json",
                data: {
                    modelId: "${revision.model.submissionId}",
                    tags: buildTagSet()
                },
                beforeSend: function () {
                    let msg;
                    if (updatedTags.length === 0) {
                        msg = "No tags applied to the model.";
                    } else {
                        msg = "The tags applied to the model are being saved into our database. Please wait...";
                    }
                    toastr.clear();
                    toastr.info(msg);
                }
            }).done(function (data, txtStatus, jqXHR) {
                const msg = data.message;
                const statusCode = data.status
                toastr.clear();
                if (statusCode === 200) {
                    toastr.success(msg);
                    // update select2 data

                } else if (statusCode === 400) {
                    toastr.error(msg);
                } else if (statusCode === 422) {
                    toastr.warn(msg);
                } else {
                    toastr.error("Cannot determine the reason for the unexpected error");
                }
                initialTags = updatedTags;

            }).fail(function (jqXHR, status, errorThrown) {
                const msg = jqXHR.statusText;
                toastr.clear();
                toastr.error(msg);
            });
        }
    });

    function buildTagSet() {
        let data = $('.model-tags-select2').select2('data');
        let updatedTags = [];
        $.each(data, function (index, value) {
            updatedTags.push({"id": value.id, "name": value.text});
        });
        return JSON.stringify(updatedTags, ['id', 'name']);
    }

    function getDataFromSelect2() {
        let data = $('.model-tags-select2').select2('data');
        let updatedTags = [];
        $.each(data, function (index, value) {
            updatedTags.push(value.text);
        });
        return updatedTags;
    }

</script>
