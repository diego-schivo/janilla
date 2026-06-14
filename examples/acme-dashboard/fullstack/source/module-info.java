module com.janilla.acmedashboard.fullstack {

	exports com.janilla.acmedashboard.fullstack;

	opens com.janilla.acmedashboard.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.acmedashboard.backend;
	requires transitive com.janilla.acmedashboard.frontend;
}
