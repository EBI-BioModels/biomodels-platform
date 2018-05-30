<%--
 Copyright (C) 2010-2014 EMBL-European Bioinformatics Institute (EMBL-EBI),
 Deutsches Krebsforschungszentrum (DKFZ)

 This file is part of Jummp.

 Jummp is free software; you can redistribute it and/or modify it under the
 terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 details.

 You should have received a copy of the GNU Affero General Public License along
 with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.

--%>








<%=packageName%>
<% import grails.persistence.Event %>

<%  excludedProps = Event.allEvents.toList() << 'version' << 'dateCreated' << 'lastUpdated'
persistentPropNames = domainClass.persistentProperties*.name
boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate') || pluginManager?.hasGrailsPlugin('hibernate4')
if (hasHibernate) {
    def GrailsDomainBinder = getClass().classLoader.loadClass('org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsDomainBinder')
    if (GrailsDomainBinder.newInstance().getMapping(domainClass)?.identity?.generator == 'assigned') {
        persistentPropNames << domainClass.identifier.name
    }
}
props = domainClass.properties.findAll { persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) && (domainClass.constrainedProperties[it.name] ? domainClass.constrainedProperties[it.name].display : true) }
Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
for (p in props) {
    if (p.embedded) {
        def embeddedPropNames = p.component.persistentProperties*.name
        def embeddedProps = p.component.properties.findAll { embeddedPropNames.contains(it.name) && !excludedProps.contains(it.name) }
        Collections.sort(embeddedProps, comparator.constructors[0].newInstance([p.component] as Object[]))
%><fieldset class="embedded"><legend><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /></legend><%
        for (ep in p.component.properties) {
            renderFieldForProperty(ep, p.component, "${p.name}.")
        }
%></fieldset><%
        } else {
            renderFieldForProperty(p, domainClass)
        }
    }

    private renderFieldForProperty(p, owningClass, prefix = "") {
        boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate') || pluginManager?.hasGrailsPlugin('hibernate4')
        boolean required = false
        if (hasHibernate) {
            cp = owningClass.constrainedProperties[p.name]
            required = (cp ? !(cp.propertyType in [boolean, Boolean]) && !cp.nullable : false)
        }
%>
<div class="fieldcontain \${hasErrors(bean: ${propertyName}, field: '${prefix}${p.name}', 'error')} ${required ? 'required' : ''}">
    <label for="${prefix}${p.name}">
        <g:message code="${domainClass.propertyName}.${prefix}${p.name}.label" default="${p.naturalName}" />
        <% if (required) { %><span class="required-indicator">*</span><% } %>
    </label>
    ${renderEditor(p)}
</div>
<%  } %>

