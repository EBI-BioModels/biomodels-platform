<%--
  Created by IntelliJ IDEA.
  Author: tnguyen@ebi.ac.uk
  Date: 2019-07-08
  Time: 16:57
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="layout" content="${grailsApplication.config.jummp.branding.style}/main"/>
    <title>${title} | BioModels</title>
    <style>
        /* Style the input */
        .on-page-search {
            width: 100%;
            font-size: 14px;
            line-height: 26px;
            color: #787d85;
            background-color: #fcfcfc;
            border: 1px solid #e0e1e1;
            padding: 5px 15px;
        }

        /* Style the results */
        .results {
            background: rgb(0,124,130);
            color: white !important;
        }
        .results:hover {
            background: #de1919;
            color: white;
        }
    </style>
</head>

<body>
    <h2>Browse Path2Models</h2>
    <g:render template="/templates/p2mBrowseFilter"/>
    <div id="categories">
    <g:each in="${categories}" var="cate">
        <g:render template="/templates/p2mBrowsePageHeaderCategory" model="[cate: cate]" />
        <g:render template="/templates/p2mBrowsePageBodyCategory" model="[categories: cate.value]" />
    </g:each>
    </div>
    <script type="text/javascript">
        window.onload = function() {
            if (window.jQuery) {
                $('.back-to-top').click(function () {
                    $('html, body').animate({"scrollTop": "0px"}, 1000);
                });
            } else {
                console.log("jQuery is not loaded");
            }
        };

        jQuery(document).ready(function($) {
            $("#searchWithKeywords").on("keyup", function () {
                let v = $(this).val();
                $(".results").removeClass("results");
                $("li.category-name").each(function () {
                    if (v !== "" && $(this).text().search(new RegExp(v,'gi')) !== -1) {
                        $(this).addClass("results");
                    }
                });
            });
        });
    </script>
</body>
</html>
