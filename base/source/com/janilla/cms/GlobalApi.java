package com.janilla.cms;

import java.util.List;

import com.janilla.http.HttpExchange;

public interface GlobalApi<ID extends Comparable<ID>, D extends Document<ID>> {

	D create(D document);

	D read(HttpExchange exchange);

	D update(D document, Boolean draft, Boolean autosave);

	D delete();

	List<Version<ID, D>> readVersions();

	Version<ID, D> readVersion(ID versionId);

	D restoreVersion(ID versionId, Boolean draft);
}
