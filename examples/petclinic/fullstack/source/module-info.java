module com.janilla.petclinic.fullstack {

	exports com.janilla.petclinic.fullstack;

	opens com.janilla.petclinic.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.petclinic.backend;
	requires transitive com.janilla.petclinic.frontend;
}
