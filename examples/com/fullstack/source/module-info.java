module com.janilla.janillacom.fullstack {

	exports com.janilla.janillacom.fullstack;

	opens com.janilla.janillacom.fullstack;

	requires transitive com.janilla.websitetemplate.fullstack;
	requires transitive com.janilla.janillacom.backend;
	requires transitive com.janilla.janillacom.frontend;
}
