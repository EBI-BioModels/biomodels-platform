package net.biomodels.jummp.scms

class ScmsTagLib {
    static namespace = "scms"

    static defaultEncodeAs = [taglib: 'none']

    def renderFoundPosts = { Map attrs ->
        out << render(template: "/templates/content/listOfFoundPosts",
            plugin: "jummp-plugin-simple-cms", model: [posts: attrs.post, searchTerm: attrs.searchTerm])
    }
}
