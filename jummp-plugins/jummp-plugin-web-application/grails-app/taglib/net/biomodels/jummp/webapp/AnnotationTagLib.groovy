package net.biomodels.jummp.webapp

import net.biomodels.jummp.core.annotation.QualifierTransportCommand as QTC
import net.biomodels.jummp.core.annotation.ResourceReferenceTransportCommand as RRTC
import net.biomodels.jummp.core.constants.BioModels

class AnnotationTagLib {
    static namespace = "anno"

    def annotationRenderingTemplateProvider

    static defaultEncodeAs = [taglib: 'none']
    //static encodeAsForTags = [tagName: [taglib:'html'], otherTagName: [taglib:'none']]

    /**
     * Renders model-level annotations on the 'Overview' tab of the model display.
     *
     * @attr annotations REQUIRED mapping from qualifier to list of resource references
     * that should be rendered.
     */
    def renderGenericAnnotations = { attrs ->
        Map<QTC, List<RRTC>> anno = attrs.annotations
        Map<String, QTC> tempQTC = new HashMap<String, QTC>()
        HashMap<String, List<RRTC>> sortedAnno = new LinkedHashMap<String, List<RRTC>>()
        // group by qualifier
        anno.each {
            List<RRTC> list = sortedAnno.get(it.key.accession)
            if (list == null) {
                list = new ArrayList<RRTC>()
                sortedAnno.put(it.key.accession, list)
            }
            list.addAll(it.value)
            tempQTC.put(it.key.accession, it.key)
        }
        // sort by collection name
        // Step 1: create a list of comparable composed objects of Qualifier Transport Command
        //          and List of Resource Reference Transport Command
        List<BioModelsOrderedStatement> bmOrderedStmts = new ArrayList<BioModelsOrderedStatement>()
        sortedAnno.each {key, value ->
            BioModelsOrderedStatement stmt = new BioModelsOrderedStatement(tempQTC.get(key), value)
            bmOrderedStmts.add(stmt)
        }
        // Step 2: implement sort method against the orders of these composed objects
        Collections.sort(bmOrderedStmts, new Comparator<BioModelsOrderedStatement>() {
            @Override
            int compare(BioModelsOrderedStatement o1, BioModelsOrderedStatement o2) {
                int o1Order = o1.order()
                int o2Order = o2.order()
                return o1Order <=> o2Order
            }
        })
        // copy the bmOrderedStmts to Map object
        Map<QTC, List<RRTC>> annotations = new LinkedHashMap<QTC, List<RRTC>>()
        bmOrderedStmts.each {
            annotations.put(it.qtc, it.listRRTC)
        }
        // rendering
        out << g.render(template: "/annotation/biomodels/openStatement", plugin: "jummp-plugin-web-application")
        String templateName = annotationRenderingTemplateProvider.template
        String tpl = "/annotation/$templateName"
        for (Map.Entry<QTC, List<RRTC>> entry : annotations.entrySet()) {
            int size = entry.value.size()
            out << g.render(template: "/annotation/biomodels/openQualifier",
                            plugin: "jummp-plugin-web-application", model: [qualifier: entry.key, total: size])
            int nbLoops = Math.ceil(size/BioModels.BM_MIN_NB_CR).toInteger()
            int start =  0
            int end = 0
            for (int i = 1; i <= nbLoops; i++) {
                start = (i-1)*BioModels.BM_MIN_NB_CR
                end = start + BioModels.BM_MIN_NB_CR
                end = end > size ? size : end
                List<RRTC> references = entry.value.subList(start, end)
                out << g.render(template: tpl, plugin: "jummp-plugin-web-application",
                                model: [total: size, index: i, qualifier: entry.key, references: references])
            }
            out << g.render(template: "/annotation/biomodels/endQualifier", plugin: "jummp-plugin-web-application")
        }
        List<String> qualifiers = annotations.keySet()*.accession
        out << g.render(template: "/annotation/biomodels/closeStatement", plugin: "jummp-plugin-web-application", model: [qualifiers: qualifiers])

    }
}

class BioModelsOrderedStatement {
    final static Map<String, Integer> COLLECTION_ORDER = ["BioModels Database": 1,
                                                          "PubMed": 2,
                                                          "Taxonomy": 3,
                                                          "Gene Ontology": 4,
                                                          "Others": 5]
    QTC qtc
    List<RRTC> listRRTC

    BioModelsOrderedStatement(QTC qtc,
                               List<RRTC> listRRTC) {
        this.qtc = qtc
        this.listRRTC = listRRTC
    }

    int order() {
        List<String> collectionNames = listRRTC.collect {
            it.collectionName
        }
        if (isAllSame(collectionNames)) {
            return COLLECTION_ORDER.get(collectionNames[0]) ?: COLLECTION_ORDER.get("Others")
        } else {
            int sum = 0
            collectionNames.each {
                int order = COLLECTION_ORDER.get(it) ?: COLLECTION_ORDER.get("Others")
                sum += order
            }
            return sum
        }
    }

    private boolean isAllSame(List list) {
        Arrays.stream(list).distinct().count() == 1
    }
}
