package com.janilla.cms;

import java.util.List;

import com.janilla.http.HttpExchange;

public interface CollectionApi<ID extends Comparable<ID>, D extends Document<ID>> {

	D create(D document);

	List<D> read(Long skip, Long limit);

	D read(ID id, HttpExchange exchange);

	D update(ID id, D document, Boolean draft, Boolean autosave);

	D delete(ID id);

	List<D> delete(List<ID> ids);

	D patch(ID id, D document);

	List<D> patch(D document, List<ID> ids);

	List<Version<ID, D>> readVersions(ID id);

	Version<ID, D> readVersion(ID versionId);

	D restoreVersion(ID versionId, Boolean draft);
}
