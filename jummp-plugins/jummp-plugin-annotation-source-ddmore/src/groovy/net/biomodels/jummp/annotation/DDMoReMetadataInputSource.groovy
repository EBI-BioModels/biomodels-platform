/**
 * Copyright (C) 2010-2015 EMBL-European Bioinformatics Institute (EMBL-EBI),
 * Deutsches Krebsforschungszentrum (DKFZ)
 *
 * This file is part of Jummp.
 *
 * Jummp is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Affero General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * Jummp is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Affero General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Affero General Public License along
 * with Jummp; if not, see <http://www.gnu.org/licenses/agpl-3.0.html>.
 */

package net.biomodels.jummp.annotation

import eu.ddmore.metadata.api.*
import eu.ddmore.metadata.api.domain.Id
import eu.ddmore.metadata.api.domain.enums.PropertyRange
import eu.ddmore.metadata.api.domain.enums.ValueSetType
import eu.ddmore.metadata.api.domain.properties.Property
import eu.ddmore.metadata.api.domain.sections.*
import eu.ddmore.metadata.api.domain.values.*
import groovy.transform.CompileStatic
import net.biomodels.jummp.core.model.RevisionTransportCommand
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import ontologies.OntologySource

@CompileStatic
class DDMoReMetadataInputSource implements MetadataInputSource {
    private final Log log = LogFactory.getLog(DDMoReMetadataInputSource.class)
    MetadataInformationService service
    /**
     * Compare two Values based on either their rank or their label.
     * The latter are assumed to never be empty or undefined.
     */
    final Comparator VALUE_COMPARATOR = { Value v1, Value v2 ->
        String v1Name = v1.valueId?.label
        String v2Name = v2.valueId?.label
        String v1Rank = v1.valueRank
        String v2Rank = v2.valueRank
        if (v1Rank && v2Rank) {
            return v1Rank <=> v2Rank
        } else {
            return v1Name <=> v2Name
        }
    } as Comparator

    boolean supports(RevisionTransportCommand revision) {
        "PharmML" == revision?.format?.name
    }

    Set<SectionContainer> buildObjectModel(RevisionTransportCommand revision = null) {
        List<Section> sectionGraph = service.getAllPopulatedRootSections()
        Set<SectionContainer> sectionContainers = processSections(sectionGraph)
    }

    private SectionContainer createSectionContainer(Section source) {
        final String name = source.sectionLabel
        final int order = source.sectionOrder
        final String id = "section${source.sectionOrder}"
        final String msg = source.toolTip
        boolean isComposite = source instanceof CompositeSection
        if (!isComposite) {
            return new SectionContainer(name: name, id: id, relativeOrder: order, info: msg)
        } else {
            boolean haveNestedCompositeSections =
                    null != source.sections.find { it instanceof CompositeSection }
            if (haveNestedCompositeSections) {
                return new CompositeSectionContainer(name: name, id: id, relativeOrder: order,
                        info: msg, children: new TreeSet<SectionContainer>())
            } else {
                return new SectionContainer(name: name, id: id, relativeOrder: order, info: msg)
            }
        }
    }

    Set<SectionContainer> processSections(List<Section> sections, SectionContainer parent = null) {
        def response = new TreeSet<SectionContainer>()
        sections.each { Section section ->
            if (!section.isEnabled()) {
                return
            }
            switch (section.getClass()) {
                case GenericSection:
                    processSectionProperties(section, parent)
                    break
                case CompositeSection:
                    SectionContainer thisSection = createSectionContainer(section)
                    List<Section> subSections = ((CompositeSection) section).sections
                    Set children = processSections(subSections, thisSection)
                    if (thisSection instanceof CompositeSectionContainer) {
                        thisSection.addAll children
                    }
                    response.add thisSection
                    break
                default:
                    log.error("Can't handle section of unkown type: ${section.dump()}")
            }
        }
        response
    }

    void processSectionProperties(Section src, SectionContainer target) {
        if (!target) {
            log.error("BUG:Encountered GenericSection ${src.dump()} without a parent container")
            return
        }
        final String name = src.sectionLabel
        final String tooltip = src.toolTip
        final int order = src.sectionOrder
        /*
         * src.properties seems to return the properties of the Groovy object src,
         * i.e. the instance variables.
         * the problem goes away if we annotate this method with @CompileDynamic,
         * but then we'd lose the performance improvements of static compilation.
         */
        java.lang.reflect.Method m = src.getClass().getMethod('getProperties', null)
        List<Property> props = (List<Property>) m.invoke(src, m.parameterTypes)
        props.each { Property p ->
            Id pId = p.propertyId
            def pVertex = new PropertyContainer(value: name, uri: pId.uri,
                    tooltip: tooltip, range: buildPropertyRange(p.range), relativeOrder: order)
            buildValuesForProperty(p, pVertex)
            target.annotationProperties.add(pVertex)
        }
    }


    private void buildValuesForProperty(Property p, PropertyContainer vtx) {
        List<Value> values
        if(p.getValueSetType().equals(ValueSetType.ONTOLOGY)){
            def ontologySources = service.findOntologyResourcesForProperty(p)
            if (ontologySources) {
                def autoCompletionOpts = [:]
                List<String> ontologyIDs = ontologySources.collect { OntologySource o ->
                    o.sourceId
                }
                autoCompletionOpts['src'] = ontologyIDs
                vtx.options['autoCompletion'] = autoCompletionOpts
            }
        } else {
            values = service.findValuesForProperty(p)
        }

        TreeMap orderedValues = new TreeMap(VALUE_COMPARATOR)
        values.each { Value v ->
            ValueContainer vVertex = visit(v)
            orderedValues.put(v, vVertex)
        }
        vtx.values.addAll(orderedValues.values())
    }

    private ValueContainer visit(Value v) {
        Id vId = v.valueId
        String name = vId.label
        String uri = vId.uri
        ValueContainer vVertex
        if (v.isValueTree()) {
            vVertex = new CompositeValueContainer(value: name, uri: uri)
            List<Value> childValues = ((CompositeValue) v).getValues()
            TreeMap orderedValues = new TreeMap(VALUE_COMPARATOR)
            childValues.each { Value child ->
                ValueContainer thisValueContainer = visit(child)
                orderedValues.put(child, thisValueContainer)
            }
            vVertex.children.addAll orderedValues.values()
        } else {
            vVertex = new ValueContainer(value: name, uri: uri)
        }
        vVertex
    }

    private AnnotationPropertyRange buildPropertyRange(PropertyRange r) {
        AnnotationPropertyRange.valueOf(r.toString())
    }
}

