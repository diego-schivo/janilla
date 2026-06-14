module com.janilla.todomvc.fullstack {

	exports com.janilla.todomvc.fullstack;

	opens com.janilla.todomvc.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.todomvc.backend;
	requires transitive com.janilla.todomvc.frontend;
}
