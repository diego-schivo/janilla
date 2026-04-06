package com.janilla.todomvc.frontend;

import java.util.List;
import java.util.Map;

import com.janilla.frontend.App;
import com.janilla.frontend.Index;
import com.janilla.frontend.Script;
import com.janilla.frontend.Template;
import com.janilla.web.Render;

@Render(template = "index", resource = { "/base/index.html", "/index.html" })
record IndexImpl(String title, Map<String, String> imports, List<Script> scripts, App app, List<Template> templates)
		implements Index {
}
