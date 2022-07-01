class UrlMappings {

	static mappings = { appContext ->
        "/$controller/$action?/$id?(.$format)?"{
            constraints {
                // apply constraints here
            }
        }

        def adminURI = "/cms"
        (adminURI + "/editor/$action?")(controller: "cmsEditor")
        //"/cms/editor/edit"(controller: "cmsEditor", action: "edit")
        "/"(view:"/index")
        "500"(view:'/error')
	}
}
