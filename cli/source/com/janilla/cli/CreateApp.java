package com.janilla.cli;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;

public class CreateApp {

	public static void main(String[] args) throws IOException {
		var d0 = Path.of("/Users/diego.schivo/git/janilla/examples");
		var r1 = new R("new-blank", "newblank", "NewBlank", "janilla-new-blank");
//		var r2 = new R("todomvc", "todomvc", "TodoMvc", "janilla-todomvc");
//		var r2 = new R("petclinic", "petclinic", "Petclinic", "janilla-petclinic");
//		var r2 = new R("address-book", "addressbook", "AddressBook", "janilla-address-book");
//		var r2 = new R("acme-dashboard", "acmedashboard", "AcmeDashboard", "janilla-acme-dashboard");
		var r2 = new R("conduit", "conduit", "Conduit", "janilla-conduit");

		var d1 = d0.resolve(r1.n1);
		var d2 = d0.resolve(r2.n1);

		if (Files.exists(d2))
			throw new RuntimeException();

		var m = new HashMap<Path, Path>();

		Files.walkFileTree(d1, new SimpleFileVisitor<>() {

			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				boolean c;
				if (dir.equals(d1))
					c = true;
				else {
					var n = dir.getFileName().toString();
					c = !(n.startsWith(".") || n.equals("target"));
				}

				if (c) {
					var p1 = d1.relativize(dir);

					Path p2;
					if (p1.getNameCount() == 1)
						p2 = p1;
					else {
						var n1 = p1.getFileName();
						var n2 = n1.toString().equals(r1.n2) ? Path.of(r2.n2) : n1;
						p2 = m.get(p1.getParent()).resolve(n2);
					}
					m.put(p1, p2);

					var d = d2.resolve(p2);
					IO.println(d);
					Files.createDirectory(d);
				}

				return c ? FileVisitResult.CONTINUE : FileVisitResult.SKIP_SUBTREE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				var c = !file.getFileName().toString().startsWith(".");
				if (c) {
					var p1 = d1.relativize(file);

					var n1 = p1.getFileName();
					var n1s = n1.toString();
					var n2 = n1s.startsWith(r1.n3) ? Path.of(r2.n3 + n1s.substring(r1.n3.length())) : n1;

					var p2 = p1.getNameCount() == 1 ? n2 : m.get(p1.getParent()).resolve(n2);
					var f = d2.resolve(p2);
					IO.println(f);

					if (n1s.equals("pom.xml") || n1s.endsWith(".java") || n1s.equals("config.json")
							|| n1s.equals("log.json")) {
						var s1 = Files.readString(file);
						var s2 = s1.replace(r1.n4, r2.n4).replace(r1.n3, r2.n3).replace(r1.n2, r2.n2).replace(r1.n1,
								r2.n1);
						Files.writeString(f, s2);
					} else
						Files.copy(file, f);
				}

				return FileVisitResult.CONTINUE;
			}

		});
	}

	record R(String n1, String n2, String n3, String n4) {
	}
}
