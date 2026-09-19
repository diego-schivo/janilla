module com.janilla.newwebsite.fullstack {

	exports com.janilla.newwebsite.fullstack;

	opens com.janilla.newwebsite.fullstack;

	requires transitive com.janilla.websitetemplate.fullstack;
	requires transitive com.janilla.newwebsite.backend;
	requires transitive com.janilla.newwebsite.frontend;
}
