package com.janilla.persistence;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.janilla.database.BTree;
import com.janilla.database.Database;
import com.janilla.database.Memory;
import com.janilla.database.Store;
import com.janilla.io.ElementHelper;
import com.janilla.persistence.Persistence.Configuration;

public class PersistenceBuilder {

	private Path file;

	private int order = 100;

	private Supplier<Stream<Class<?>>> types;

	private Supplier<Persistence> persistence;

	public void setFile(Path file) {
		this.file = file;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	public void setTypes(Supplier<Stream<Class<?>>> types) {
		this.types = types;
	}

	public void setPersistence(Supplier<Persistence> persistence) {
		this.persistence = persistence;
	}

	public Persistence build() throws IOException {
		FileChannel c;
		{
			var f = Files.createDirectories(file.getParent()).resolve(file.getFileName());
			c = FileChannel.open(f, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
		}

		var m = new Memory();
		{
			var t = m.getFreeBTree();
			t.setChannel(c);
			t.setOrder(order);
			t.setRoot(BTree.readReference(c, 0));
			m.setAppendPosition(Math.max(3 * (8 + 4), c.size()));
		}

		var d = new Database();
		d.setBTreeOrder(order);
		d.setChannel(c);
		d.setMemoryManager(m);
		d.setStoresRoot(8 + 4);
		d.setIndexesRoot(2 * (8 + 4));

		var p = persistence != null ? persistence.get() : new Persistence();
		p.database = d;
		p.configuration = new Configuration();
		types.get().forEach(p::configure);

		d.setInitializeStore((n, s) -> {
			@SuppressWarnings("unchecked")
			var u = (Store<String>) s;
			u.setElementHelper(ElementHelper.STRING);
		});
		d.setInitializeIndex((n, i) -> {
			if (p.initialize(n, i))
				return;
		});
		return p;
	}
}
