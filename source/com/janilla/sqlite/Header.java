/*
 * Copyright (c) 2024, 2025, Diego Schivo. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Diego Schivo designates
 * this particular file as subject to the "Classpath" exception as
 * provided by Diego Schivo in the LICENSE file that accompanied this
 * code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Diego Schivo, diego.schivo@janilla.com or visit
 * www.janilla.com if you need additional information or have any questions.
 */
package com.janilla.sqlite;

import java.nio.ByteBuffer;

public class Header {

	protected final ByteBuffer buffer;

	public Header(ByteBuffer buffer) {
		this.buffer = buffer;
	}

	public ByteBuffer buffer() {
		return buffer;
	}

	public String getString() {
		var bb = new byte[16];
		buffer.get(0, bb);
		return new String(bb);
	}

	public void setString(String string) {
		buffer.put(0, string.getBytes());
	}

	public int getPageSize() {
		return Short.toUnsignedInt(buffer.getShort(16));
	}

	public void setPageSize(int pageSize) {
		buffer.putShort(16, (short) pageSize);
	}

	public int getWriteVersion() {
		return Byte.toUnsignedInt(buffer.get(18));
	}

	public void setWriteVersion(int writeVersion) {
		buffer.put(18, (byte) writeVersion);
	}

	public int getReadVersion() {
		return Byte.toUnsignedInt(buffer.get(19));
	}

	public void setReadVersion(int readVersion) {
		buffer.put(19, (byte) readVersion);
	}

	public int getReservedBytes() {
		return Byte.toUnsignedInt(buffer.get(20));
	}

	public void setReservedBytes(int reservedBytes) {
		buffer.put(20, (byte) reservedBytes);
	}

	public int getMaximumFraction() {
		return Byte.toUnsignedInt(buffer.get(21));
	}

	public void setMaximumFraction(int maximumFraction) {
		buffer.put(21, (byte) maximumFraction);
	}

	public int getMinimumFraction() {
		return Byte.toUnsignedInt(buffer.get(22));
	}

	public void setMinimumFraction(int minimumFraction) {
		buffer.put(22, (byte) minimumFraction);
	}

	public int getLeafFraction() {
		return Byte.toUnsignedInt(buffer.get(23));
	}

	public void setLeafFraction(int leafFraction) {
		buffer.put(23, (byte) leafFraction);
	}

	public long getChangeCounter() {
		return Integer.toUnsignedLong(buffer.getInt(24));
	}

	public void setChangeCounter(long changeCounter) {
		buffer.putInt(24, (int) changeCounter);
	}

	public long getSize() {
		return Integer.toUnsignedLong(buffer.getInt(28));
	}

	public void setSize(long size) {
		buffer.putInt(28, (int) size);
	}

	public long getFreelistStart() {
		return Integer.toUnsignedLong(buffer.getInt(32));
	}

	public void setFreelistStart(long freelistStart) {
		buffer.putInt(32, (int) freelistStart);
	}

	public long getFreelistSize() {
		return Integer.toUnsignedLong(buffer.getInt(36));
	}

	public void setFreelistSize(long freelistSize) {
		buffer.putInt(36, (int) freelistSize);
	}

	public long getSchemaCookie() {
		return Integer.toUnsignedLong(buffer.getInt(40));
	}

	public void setSchemaCookie(long schemaCookie) {
		buffer.putInt(40, (int) schemaCookie);
	}

	public long getSchemaFormat() {
		return Integer.toUnsignedLong(buffer.getInt(44));
	}

	public void setSchemaFormat(long schemaFormat) {
		buffer.putInt(44, (int) schemaFormat);
	}

	public long getCacheSize() {
		return Integer.toUnsignedLong(buffer.getInt(48));
	}

	public void setCacheSize(long cacheSize) {
		buffer.putInt(48, (int) cacheSize);
	}

	public long getTextEncoding() {
		return Integer.toUnsignedLong(buffer.getInt(56));
	}

	public void setTextEncoding(long textEncoding) {
		buffer.putInt(56, (int) textEncoding);
	}

	public long getVersion() {
		return Integer.toUnsignedLong(buffer.getInt(92));
	}

	public void setVersion(long version) {
		buffer.putInt(92, (int) version);
	}

	public long getLibraryVersion() {
		return Integer.toUnsignedLong(buffer.getInt(96));
	}

	public void setLibraryVersion(long libraryVersion) {
		buffer.putInt(96, (int) libraryVersion);
	}
}
