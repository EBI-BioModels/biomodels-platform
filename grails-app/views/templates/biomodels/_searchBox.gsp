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
        text-align: center;
    }
</style>
<form id="local-search" name="local-search"
      action="${createLink(controller: 'search', action: 'searchRedir')}" method="post">
    <fieldset>
        <div class="input-group">
            <input type="text" name="search_block_form" id="local-searchbox"
                   placeholder="Search..." class="input-group-field search_box_style"
                   tabindex="1" size="35" maxlength="2048">
            <div class="input-group-button">
                <input id="clearsearch" type="button" value="X" tabindex="3"
                       class="clearable search_box_style">
            </div>
            <div class="input-group-button">
                <input id="search_submit" class="button icon icon-functional" tabindex="2" type="submit" name="submit1" value="1" />
            </div>
        </div>
        <p id="example">
            Examples:
            <g:link controller="search" action="search" params="${[query: "*:*"]}" class="secondary label" title="Search all">*:*</g:link>
            <g:link controller="search" action="search" params="${[query: "MAPK cascade"]}" class="secondary label" title="Search by GO term">MAPK cascade</g:link>
            <g:link controller="search" action="search" params="${[query: "homo sapiens"]}" class="secondary label" title="Search by Taxonomy term">homo sapiens</g:link>
            <g:link controller="search" action="search" params="${[query: "lung cancer"]}" class="secondary label" title="Search by Disease term">lung cancer</g:link>
            <a title="Search tips/tricks" data-open="searchTipsBox" class="secondary label label-floating-right">Search tips</a>
        </p>
    </fieldset>
</form>
<script>
    function doShowOrHide(e) {
        if ($(e).val() == '') {
            $('#clearsearch').hide();
        } else {
            $('#clearsearch').show();
        }
    }
    $('#local-searchbox').focus(function() {
        doShowOrHide(this);
    });

    $('#local-searchbox').keyup(function() {
        doShowOrHide(this);
    });

    $('#local-searchbox').change(function() {
        doShowOrHide(this);
    });

    $('#local-searchbox').dblclick(function() {
        doShowOrHide(this);
    });

    $('#clearsearch').click(function () {
        $('#local-searchbox').val('');
        $(this).hide();
    });
    $(document).ready(function () {
        doShowOrHide('#local-searchbox');
    });
</script>
