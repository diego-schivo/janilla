package com.janilla.petclinic.frontend;

import com.janilla.blanktemplate.frontend.BlankApiClient;
import com.janilla.ioc.DiFactory;

class ApiClientImpl extends BlankApiClient {

	protected final OwnerApiClient owners;

	protected final PetApiClient pets;

	protected final PetTypeApiClient petTypes;

	protected final VetApiClient vets;

	protected final VisitApiClient visits;

	public ApiClientImpl(DiFactory diFactory) {
		super(diFactory);

		owners = diFactory.newInstance(diFactory.classFor(OwnerApiClient.class));
		pets = diFactory.newInstance(diFactory.classFor(PetApiClient.class));
		petTypes = diFactory.newInstance(diFactory.classFor(PetTypeApiClient.class));
		vets = diFactory.newInstance(diFactory.classFor(VetApiClient.class));
		visits = diFactory.newInstance(diFactory.classFor(VisitApiClient.class));
	}

	public OwnerApiClient owners() {
		return owners;
	}

	public PetApiClient pets() {
		return pets;
	}

	public PetTypeApiClient petTypes() {
		return petTypes;
	}

	public VetApiClient vets() {
		return vets;
	}

	public VisitApiClient visits() {
		return visits;
	}

}
