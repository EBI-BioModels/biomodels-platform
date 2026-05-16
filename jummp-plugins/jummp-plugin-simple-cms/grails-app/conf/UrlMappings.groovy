class UrlMappings {

	static mappings = { appContext ->
        "/content/model-of-the-month?year=$year&month=$month"{
            constraints {
                year(nullable: false, matches: /[0-9]+/)
                month(nullable: false, matches: /[0-9]+/)
            }
        }
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
