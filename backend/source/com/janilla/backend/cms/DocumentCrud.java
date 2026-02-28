package com.janilla.backend.cms;

import java.util.List;
import java.util.Set;

import com.janilla.backend.persistence.Crud;
import com.janilla.cms.Document;
import com.janilla.cms.DocumentStatus;
import com.janilla.cms.Version;

public interface DocumentCrud<ID extends Comparable<ID>, D extends Document<ID>> extends Crud<ID, D> {

//	default D read(ID id, boolean drafts) {
//		return read(id, 0, drafts);
//	}

	D read(ID id, boolean drafts, int depth);

//	default List<D> read(List<ID> ids, boolean drafts) {
//		return read(ids, 0, drafts);
//	}

	List<D> read(List<ID> ids, boolean drafts, int depth);

	D update(ID id, D document, Set<String> include, boolean newVersion);

	List<D> patch(List<ID> ids, D document, Set<String> include);

	List<Version<ID, D>> readVersions(ID id);

	Version<ID, D> readVersion(ID versionId);

	D restoreVersion(ID versionId, DocumentStatus status);

}