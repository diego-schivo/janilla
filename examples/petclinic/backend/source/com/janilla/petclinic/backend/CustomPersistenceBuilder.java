/*
 * Copyright 2012-2026 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.janilla.petclinic.backend;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.janilla.backend.persistence.Persistence;
import com.janilla.backend.persistence.PersistenceBuilder;
import com.janilla.ioc.DiFactory;
import com.janilla.petclinic.Owner;
import com.janilla.petclinic.Pet;
import com.janilla.petclinic.PetType;
import com.janilla.petclinic.Specialty;
import com.janilla.petclinic.Vet;
import com.janilla.petclinic.Visit;

/**
 * @author Diego Schivo
 */
public class CustomPersistenceBuilder extends PersistenceBuilder {

	public CustomPersistenceBuilder(Path databaseFile) {
		super(databaseFile);
	}

	@Override
	public Persistence build(DiFactory diFactory) {
		var fe = Files.exists(databaseFile);
		var p = super.build(diFactory);
//		p.setTypeResolver(x -> {
//			try {
//				return Class.forName(getClass().getPackageName() + "." + x.replace('.', '$'));
//			} catch (ClassNotFoundException e) {
//				throw new RuntimeException(e);
//			}
//		});
		if (!fe)
			seed(p);
		return p;
	}

	private void seed(Persistence persistence) {
		var oo = Arrays.stream("""
				George	Franklin	110 W. Liberty St.	Madison	6085551023
				Betty	Davis	638 Cardinal Ave.	Sun Prairie	6085551749
				Eduardo	Rodriquez	2693 Commerce St.	McFarland	6085558763
				Harold	Davis	563 Friendly St.	Windsor	6085553198
				Peter	McTavish	2387 S. Fair Way	Madison	6085552765
				Jean	Coleman	105 N. Lake St.	Monona	6085552654
				Jeff	Black	1450 Oak Blvd.	Monona	6085555387
				Maria	Escobito	345 Maple St.	Madison	6085557683
				David	Schroeder	2749 Blackhawk Trail	Madison	6085559435
				Carlos	Estaban	2335 Independence La.	Waunakee	6085555487""".split("\n")).map(x -> {
			var y = x.split("\t");
			return persistence.crud(Owner.class).create(new Owner(null, y[0], y[1], y[2], y[3], y[4], null));
		}).collect(Collectors.toMap(x -> x.id(), x -> x));

		var tt = Arrays.stream("""
				cat
				dog
				lizard
				snake
				bird
				hamster""".split("\n")).map(x -> persistence.crud(PetType.class).create(new PetType(null, x)))
				.collect(Collectors.toMap(x -> x.id(), x -> x));

		var pp = Arrays.stream("""
				Leo	2010-09-07	1	1
				Basil	2012-08-06	6	2
				Rosy	2011-04-17	2	3
				Jewel	2010-03-07	2	3
				Iggy	2010-11-30	3	4
				George	2010-01-20	4	5
				Samantha	2012-09-04	1	6
				Max	2012-09-04	1	6
				Lucky	2011-08-06	5	7
				Mulligan	2007-02-24	2	8
				Freddy	2010-03-09	5	9
				Lucky	2010-06-24	2	10
				Sly	2012-06-08	1	10""".split("\n")).map(x -> {
			var y = x.split("\t");
			return persistence.crud(Pet.class).create(new Pet(null, y[0], LocalDate.parse(y[1]),
					tt.get(Long.parseLong(y[2])), oo.get(Long.parseLong(y[3])), null));
		}).collect(Collectors.toMap(x -> x.id(), x -> x));

		Arrays.stream("""
				7	2013-01-01	rabies shot
				8	2013-01-02	rabies shot
				8	2013-01-03	neutered
				7	2013-01-04	spayed""".split("\n")).map(x -> {
			var y = x.split("\t");
			return persistence.crud(Visit.class)
					.create(new Visit(null, pp.get(Long.parseLong(y[0])), LocalDate.parse(y[1]), y[2]));
		}).collect(Collectors.toMap(x -> x.id(), x -> x));

		var ss = Arrays.stream("""
				radiology
				surgery
				dentistry""".split("\n")).map(x -> persistence.crud(Specialty.class).create(new Specialty(null, x)))
				.collect(Collectors.toMap(x -> x.id(), x -> x));

		Arrays.stream("""
				James	Carter
				Helen	Leary	1
				Linda	Douglas	2	3
				Rafael	Ortega	2
				Henry	Stevens	1
				Sharon	Jenkins""".split("\n")).map(x -> {
			var y = x.split("\t");
			return persistence.crud(Vet.class).create(
					new Vet(null, y[0], y[1], Arrays.stream(y).skip(2).map(z -> ss.get(Long.valueOf(z))).toList()));
		}).collect(Collectors.toMap(x -> x.id(), x -> x));
	}
}
