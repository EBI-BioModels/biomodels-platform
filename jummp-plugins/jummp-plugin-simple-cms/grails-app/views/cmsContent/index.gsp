<%--
  Created by IntelliJ IDEA.
  User: nvntung@gmail.com
  Date: 05/02/2024
  Time: 11:06
--%>

<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <meta name="layout" content="biomodels/main"/>
    <title>${titlePage}</title>
    <g:render template="/templates/head"/>

    <style>
    .tree {
        --spacing: 1.5rem;
        --radius: 10px;
    }

    .tree li {
        display: block;
        position: relative;
        padding-left: calc(2 * var(--spacing) - var(--radius) - 2px);
        margin-top: 8px;
        margin-bottom: 10px;
    }

    .tree ul {
        margin-left: calc(var(--radius) - var(--spacing));
        padding-left: 0;
    }

    .tree ul li {
        border-left: 2px solid #ddd;
    }

    .tree ul li:last-child {
        border-color: transparent;
    }

    .tree ul li::before {
        content: '';
        display: block;
        position: absolute;
        top: calc(var(--spacing) / -2);
        left: -2px;
        width: calc(var(--spacing) + 2px);
        height: calc(var(--spacing) + 1px);
        border: solid #ddd;
        border-width: 0 0 2px 2px;
    }

    .tree summary {
        display: block;
        cursor: pointer;
    }

    .tree summary::marker,
    .tree summary::-webkit-details-marker {
        display: none;
    }

    .tree summary:focus {
        outline: none;
    }

    .tree summary:focus-visible {
        outline: 1px dotted #000;
    }

    .tree li::after,
    .tree summary::before {
        content: '';
        display: block;
        position: absolute;
        top: calc(var(--spacing) / 2 - var(--radius));
        left: calc(var(--spacing) - var(--radius) - 1px);
        width: calc(2 * var(--radius));
        height: calc(2 * var(--radius));
        border-radius: 50%;
        background: #ddd;
    }

    .tree summary::before {
        z-index: 1;
        background: #696 url("${grailsApplication.config.grails.serverURL}/images/expand-collapse.svg") 0 0;
    }

    .tree details[open] > summary::before {
        background-position: calc(-2 * var(--radius)) 0;
    }
    </style>
</head>

<body>
<h1>All Items</h1>
<!-- This implementation of tree view is original from the post https://iamkate.com/code/tree-views/ -->
<ul class="tree">
    <g:each in="${items}" var="item" status="i">
        <li>
            <%
                String[] parts = item.key.split(";")
                Long parentId = parts[0] as Long
                String parentTitle = parts[1]
            %>
            <details open>
                <summary><a href="${createLink(controller: "cmsContent", action: "show", id: parentId)}"
                            target="_blank">
                    ${parentTitle}</a></summary>
                <ul>
                    <g:each in="${item.value}" var="child" status="j">
                        <li><details><summary>
                            <a href="${createLink(controller: "cmsContent", action: "show", id: child.id)}"
                               target="_blank">
                                ${child.title}</a></summary></details></li>
                    </g:each>
                </ul>
            </details>
        </li>
    </g:each>
</ul>
</body>
</html>
