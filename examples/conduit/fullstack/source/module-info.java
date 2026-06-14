module com.janilla.conduit.fullstack {

	exports com.janilla.conduit.fullstack;

	opens com.janilla.conduit.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.conduit.backend;
	requires transitive com.janilla.conduit.frontend;
}
