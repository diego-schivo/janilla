module com.janilla.newblank.fullstack {

	exports com.janilla.newblank.fullstack;

	opens com.janilla.newblank.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.newblank.backend;
	requires transitive com.janilla.newblank.frontend;
}
