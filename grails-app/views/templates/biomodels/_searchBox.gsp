<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
--%>






<style type="text/css">
    #clearsearch {
        display: block;
        outline: none;
        text-align: center;
        padding-left: 10px;
        padding-right: 8px;
    }
    .search_box_style {
        background-color: white;
        color: #0a0a0a;
        cursor: pointer;
        border: none;
    }
    .clearable {
        position: relative;
        text-align: left;
    }

    #domain_switcher {
        float: right;
    }
    .example-label {
        font-size: 0.8rem;
    }
</style>
<form id="local-search" name="local-search"
      action="${createLink(controller: 'search', action: 'search')}" method="post">
    <fieldset><div class="row">
        <div class="input-group margin-bottom-none margin-top-large padding-bottom-medium">
            <div class="input-group-field columns large-3 medium-3 small-12" style="vertical-align: text-top">
                <select class="margin-bottom-none align-self-top" id="domain_switcher" name="domain_switcher" title="Please choose a domain">
                    <option value="biomodels_all"><g:message code="net.biomodels.jummp.domain.name.BioModelsAll"/></option>
                    <option value="biomodels"><g:message code="net.biomodels.jummp.domain.name.BioModels"/></option>
                    <option value="biomodels_autogen"><g:message code="net.biomodels.jummp.domain.name.BioModelsAutogen"/></option>
                </select>
                <br/><br/>
                <a class="help-text label-floating-left secondary label"
                   title="Learn more"
                   data-open="domainSwitcherExplanationBox">What is this box used for?</a>
                <input type="text" id="chosenDomain" name="chosenDomain" style="display: none" value="biomodels" aria-label="Choose a domain"/>
            </div>

            <div class="input-group-field columns large-8 medium-8 small-12">
                <input type="text" name="search_block_form" id="local-search-box"
                   placeholder="Search..." class="input-group-field search_box_style clearable"
                   title="Search"
                   tabindex="1" style="width: 100%;">
                <p id="example" class="example-label">
                    Examples:
                    <g:link controller="search" action="search"
                            params="${[query: "*:*"]}" class="secondary label" title="Search all">*:*</g:link>
                    <g:link controller="search" action="search"
                            params="${[query: "MAPK cascade"]}" class="secondary label"
                            title="Search by GO term">MAPK cascade</g:link>
                    <g:link controller="search" action="search"
                            params="${[query: "homo sapiens"]}" class="secondary label"
                            title="Search by Taxonomy term">homo sapiens</g:link>
                    <g:link controller="search" action="search"
                            params="${[query: "lung cancer"]}" class="secondary label"
                            title="Search by Disease term">lung cancer</g:link>
                    <a title="Search tips/tricks" data-open="searchTipsBox"
                       class="secondary label label-floating-right">Search tips</a>
                </p></div>
            <div class="input-group-button columns large-1 medium-2 small-12" style="float: left">
                <input id="btn-search-submit" class="button icon icon-functional" tabindex="2"
                       type="submit" name="searchSubmit" value="1" />
            </div>
        </div>
    </div></fieldset>
</form>
<script type="text/javascript">
    $(document).ready(function () {
        let domain = "${params.domain}";
        // biomodels is the default domain
        let chosenDomain = domain.length > 0 ? domain : "biomodels";
        $('#chosenDomain').val(chosenDomain);
        $('#domain_switcher').val(chosenDomain);

        const urlParams = new URLSearchParams(window.location.search);
        const query = urlParams.get('query');

        if (query) {
            $("#local-search-box").val(query);
        }

    });
    $(document).on('change', '#domain_switcher', {}, function(e) {
        e.preventDefault();
        let domain = $(this).val();
        $('#chosenDomain').val(domain);
    });

    /* Use event capturing (runs before other handlers) */
    document.getElementById('local-search').addEventListener('submit', function(e) {
        e.preventDefault();
        e.stopImmediatePropagation();

        const query = document.getElementById('local-search-box').value;
        const domain = document.getElementById('chosenDomain').value;
        const urlParams = new URLSearchParams(window.location.search);
        urlParams.set('query', query);
        urlParams.set('domain', domain);
        const searchPattern = /^\/search(?:\/|$)/i;
        let newURL;
        if (searchPattern.test(window.location.pathname)) {
            // Matches /search or /search/ but NOT /search-results
            newURL = window.location.pathname + '?' + urlParams.toString();
        } else {
            newURL = "${grailsApplication.config.grails.serverURL}/search?" + urlParams.toString();
        }
        window.location.href = newURL;

        return false;
    }, true); // true = use capture phase
</script>
