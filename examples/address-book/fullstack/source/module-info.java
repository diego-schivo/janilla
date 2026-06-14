module com.janilla.addressbook.fullstack {

	exports com.janilla.addressbook.fullstack;

	opens com.janilla.addressbook.fullstack;

	requires transitive com.janilla.blanktemplate.fullstack;
	requires transitive com.janilla.addressbook.backend;
	requires transitive com.janilla.addressbook.frontend;
}
