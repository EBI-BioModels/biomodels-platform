<%@ page import="grails.plugin.springsecurity.SpringSecurityUtils" %>
<%@ page import="grails.util.Environment" %>
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

<footer id="local-footer" class="local-footer">
    <!-- Optional local footer (insert citation / project-specific copyright / etc here -->
    <div  id="footer" class="row">
        <div>
            <div class="small-12 medium-6 large-6 columns">
                <span style="font-size: small">Build: <g:render template="/templates/version"/></span>
            </div>
            <div class="small-12 medium-6 large-6 columns" style="text-align: right">
                <span style="font-size: small">
                    <g:if test="${Environment.isDevelopmentMode() || SpringSecurityUtils.ifAnyGranted('ROLE_ADMIN')}">
                        <g:render template="/templates/biomodels/renderedFrom"/></span>
                    </g:if>
            </div>
        </div>
        <div class="clear"></div>
    </div>
    %{-- ELIXIR Deposition Database banner (JBM-811: kept, but rendered here instead of by EBI's elixirBanner.js).
         _embl-ebi-data-preservation-statement.gsp relies on the div.elixir-ribbon > div.row > div.column structure. --}%
    <div id="elixir-banner">
        <div id="elixir-ribbon" class="elixir-ribbon">
            <div class="row">
                <div class="column">
                    <a href="//www.elixir-europe.org/platforms/data/elixir-deposition-databases">
                        <div class="elixir-logo-kite"></div>
                        <h5><span class="elixir-banner-name">This service</span> is part of the ELIXIR infrastructure</h5>
                        <div id="elixir-banner-info">
                            <small>
                                <span class="elixir-banner-description">BioModels is an ELIXIR Deposition Database</span>
                                <span class="readmore">Learn more &#8250;</span>
                            </small>
                        </div>
                    </a>
                </div>
            </div>
        </div>
    </div>
    <style>
        .elixir-ribbon {
            padding: 1rem 0;
            background-color: rgb(0, 124, 130);
            font-family: Helvetica, Arial, FreeSans, 'Liberation Sans', sans-serif;
            clear: both;
        }
        .elixir-ribbon,
        .elixir-ribbon h5,
        .elixir-ribbon a,
        .elixir-ribbon a:active,
        .elixir-ribbon a:visited,
        .elixir-ribbon a:hover {
            color: #fff;
            text-decoration: none;
            border-bottom: none;
        }
        .elixir-ribbon a:hover {
            opacity: .8;
        }
        .elixir-ribbon .readmore {
            border-bottom: 1px dotted #fff;
        }
        .elixir-ribbon h5 {
            margin: 0;
            font-size: 1.3rem;
            padding: 0;
            display: inline-block;
        }
        .elixir-ribbon .elixir-logo-kite {
            background: 80% 58% url("<g:resource dir="images/biomodels" file="elixir_kitemark-60px.png"/>") no-repeat;
            position: relative;
            margin-left: 1.25rem;
            height: 60px;
            width: 60px;
            display: inline-block;
            float: left;
            background-size: 60px;
        }
        .elixir-ribbon .row {
            margin: 0 !important;
        }
    </style>
    <!-- End optional local footer -->

    <div id="global-footer" class="global-footer">
        <nav id="global-nav-expanded" class="row global-nav-expanded">
            <!-- Footer will be automatically inserted by footer.js -->
        </nav>
        <section id="ebi-footer-meta" class="row ebi-footer-meta">
            <!-- Footer meta will be automatically inserted by footer.js -->
        </section>
    </div>
     <g:render template="/templates/feedback" plugin="jummp-plugin-web-application"/>
     <g:render template="/templates/biomodels/searchTips" />
     <g:render template="/templates/biomodels/embl-ebi-data-preservation-statement" />
     <g:render template="/templates/biomodels/domainSwitcherExplanation" />
</footer>
