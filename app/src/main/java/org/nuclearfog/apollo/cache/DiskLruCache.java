/*
 * Copyright (C) 2011 The Android Open Source Project Licensed under the Apache
 * License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0 Unless required by applicable law
 * or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */

package org.nuclearfog.apollo.cache;

import androidx.annotation.Nullable;

import org.nuclearfog.apollo.BuildConfig;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * *****************************************************************************
 * Taken from the JB source code, can be found in:
 * libcore/luni/src/main/java/libcore/io/DiskLruCache.java or direct link:
 * https:
 * //android.googlesource.com/platform/libcore/+/android-4.1.1_r1/luni/src/
 * main/java/libcore/io/DiskLruCache.java A cache that uses a bounded amount of
 * space on a filesystem. Each cache entry has a string key and a fixed number
 * of values. Values are byte sequences, accessible as streams or files. Each
 * value must be between {@code 0} and {@code Integer.MAX_VALUE} bytes in
 * duration.
 * <p>
 * The cache stores its data in a directory on the filesystem. This directory
 * must be exclusive to the cache; the cache may delete or overwrite files from
 * its directory. It is an error for multiple processes to use the same cache
 * directory at the same time.
 * <p>
 * This cache limits the number of bytes that it will store on the filesystem.
 * When the number of stored bytes exceeds the limit, the cache will remove
 * entries in the background until the limit is satisfied. The limit is not
 * strict: the cache may temporarily exceed it while waiting for files to be
 * deleted. The limit does not include filesystem overhead or the cache journal
 * so space-sensitive applications should set a conservative limit.
 * <p>
 * Clients call {@link #edit} to create or update the values of an entry. An
 * entry may have only one editor at one time; if a value is not available to be
 * edited then {@link #edit} will return null.
 * <ul>
 * <li>When an entry is being <strong>created</strong> it is necessary to supply
 * a full set of values; the empty value should be used as a placeholder if
 * necessary.
 * <li>When an entry is being <strong>edited</strong>, it is not necessary to
 * supply data for every value; values default to their previous value.
 * </ul>
 * Every {@link #edit} call must be matched by a call to {@link Editor#commit}
 * or {@link Editor#abort}. Committing is atomic: a read observes the full set
 * of values as they were before or after the commit, but never a mix of values.
 * <p>
 * Clients call {@link #get} to read a snapshot of an entry. The read will
 * observe the value at the time that {@link #get} was called. Updates and
 * removals after the call do not impact ongoing reads.
 * <p>
 * This class is tolerant of some I/O errors. If files are missing from the
 * filesystem, the corresponding entries will be dropped from the cache. If an
 * error occurs while writing a cache value, the edit will fail silently.
 * Callers should handle other problems by catching {@code IOException} and
 * responding appropriately.
 */
public final class DiskLruCache implements Closeable {
	static final String JOURNAL_FILE = "journal";

	static final String JOURNAL_FILE_TMP = "journal.tmp";

	static final String MAGIC = "libcore.io.DiskLruCache";

	static final String VERSION_1 = "1";

	private static final String CLEAN = "CLEAN";

	private static final String DIRTY = "DIRTY";

	private static final String REMOVE = "REMOVE";

	private static final String READ = "READ";

	private static final int IO_BUFFER_SIZE = 8 * 1024;

	/*
	 * This cache uses a journal file named "journal". A typical journal file
	 * looks like this: libcore.io.DiskLruCache 1 100 2 CLEAN
	 * 3400330d1dfc7f3f7f4b8d4d803dfcf6 832 21054 DIRTY
	 * 335c4c6028171cfddfbaae1a9c313c52 CLEAN 335c4c6028171cfddfbaae1a9c313c52
	 * 3934 2342 REMOVE 335c4c6028171cfddfbaae1a9c313c52 DIRTY
	 * 1ab96a171faeeee38496d8b330771a7a CLEAN 1ab96a171faeeee38496d8b330771a7a
	 * 1600 234 READ 335c4c6028171cfddfbaae1a9c313c52 READ
	 * 3400330d1dfc7f3f7f4b8d4d803dfcf6 The first five lines of the journal form
	 * its header. They are the constant string "libcore.io.DiskLruCache", the
	 * disk cache's version, the application's version, the value count, and a
	 * blank line. Each of the subsequent lines in the file is a record of the
	 * state of a cache entry. Each line contains space-separated values: a
	 * state, a key, and optional state-specific values. o DIRTY lines track
	 * that an entry is actively being created or updated. Every successful
	 * DIRTY action should be followed by a CLEAN or REMOVE action. DIRTY lines
	 * without a matching CLEAN or REMOVE indicate that temporary files may need
	 * to be deleted. o CLEAN lines track a cache entry that has been
	 * successfully published and may be read. A publish line is followed by the
	 * lengths of each of its values. o READ lines track accesses for LRU. o
	 * REMOVE lines track entries that have been deleted. The journal file is
	 * appended to as cache operations occur. The journal may occasionally be
	 * compacted by dropping redundant lines. A temporary file named
	 * "journal.tmp" will be used during compaction; that file should be deleted
	 * if it exists when the cache is opened.
	 */

	private File directory;

	private File journalFile;

	private File journalFileTmp;

	private int appVersion;

	private long maxSize;

	private int valueCount;

	private LinkedHashMap<String, Entry> lruEntries = new LinkedHashMap<>(0, 0.75f, true);
	/**
	 * This cache uses a single background thread to evict entries.
	 */
	private ExecutorService executorService = new ThreadPoolExecutor(0, 1, 60L, TimeUnit.SECONDS, new LinkedBlockingQueue<>());
	private long size = 0;
	private Writer journalWriter;
	private int redundantOpCount;

	private Callable<Void> cleanupCallable = new Callable<Void>() {
		@Override
		public Void call() throws Exception {
			synchronized (DiskLruCache.this) {
				if (journalWriter == null) {
					return null; // closed
				}
				trimToSize();
				if (journalRebuildRequired()) {
					rebuildJournal();
					redundantOpCount = 0;
				}
			}
			return null;
		}
	};

	/**
	 *
	 */
	private DiskLruCache(File directory, int appVersion, int valueCount, long maxSize) {
		this.directory = directory;
		this.appVersion = appVersion;
		journalFile = new File(directory, JOURNAL_FILE);
		journalFileTmp = new File(directory, JOURNAL_FILE_TMP);
		this.valueCount = valueCount;
		this.maxSize = maxSize;
	}

	/**
	 *
	 */
	private static String[] copyOfRange(String[] original, int end) {
		int originalLength = original.length; // For exception priority
		// compatibility.
		if (2 > end) {
			throw new IllegalArgumentException();
		}
		if (2 > originalLength) {
			throw new ArrayIndexOutOfBoundsException();
		}
		int resultLength = end - 2;
		int copyLength = Math.min(resultLength, originalLength - 2);

		String[] result = new String[resultLength];
		System.arraycopy(original, 2, result, 0, copyLength);
		return result;
	}

	/**
	 * Returns the ASCII characters up to but not including the next "\r\n", or
	 * "\n".
	 *
	 * @throws java.io.EOFException if the stream is exhausted before the next newline character.
	 */
	public static String readAsciiLine(InputStream in) throws IOException {
		StringBuilder result = new StringBuilder(80);

		int c = in.read();
		while (c != -1) {
			char t = (char) c;
			if (c != '\n' && c != '\r') {
				result.append(t);
				c = in.read();
			} else {
				break;
			}
		}
		return result.toString();
	}

	/**
	 * Closes 'closeable', ignoring any checked exceptions. Does nothing if
	 * 'closeable' is null.
	 */
	public static void closeQuietly(Closeable closeable) {
		if (closeable != null) {
			try {
				closeable.close();
			} catch (Exception e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * Recursively delete everything in {@code dir}.
	 */
	// TODO: this should specify paths as Strings rather than as Files
	public static void deleteContents(File dir) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IllegalArgumentException("not a directory: " + dir);
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deleteContents(file);
			}
			if (!file.delete()) {
				throw new IOException("failed to delete file: " + file);
			}
		}
	}

	/**
	 * Opens the cache in {@code directory}, creating a cache if none exists
	 * there.
	 *
	 * @param directory  a writable directory
	 * @param valueCount the number of values per cache entry. Must be positive.
	 * @param maxSize    the maximum number of bytes this cache should use to store
	 * @throws IOException if reading or writing the cache directory fails
	 */
	@Nullable
	public static DiskLruCache open(File directory, int appVersion, int valueCount, long maxSize) throws IOException {
		if (maxSize <= 0) {
			throw new IllegalArgumentException("maxSize <= 0");
		}
		if (valueCount <= 0) {
			throw new IllegalArgumentException("valueCount <= 0");
		}
		// prefer to pick up where we left off
		DiskLruCache cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
		if (cache.journalFile.exists()) {
			try {
				cache.readJournal();
				cache.processJournal();
				cache.journalWriter = new BufferedWriter(new FileWriter(cache.journalFile, true), IO_BUFFER_SIZE);
				return cache;
			} catch (IOException journalIsCorrupt) {
				if (BuildConfig.DEBUG) {
					journalIsCorrupt.printStackTrace();
				}
				cache.delete();
			}
		}
		// create a new empty cache
		if (directory.exists() || directory.mkdirs()) {
			cache = new DiskLruCache(directory, appVersion, valueCount, maxSize);
			cache.rebuildJournal();
			return cache;
		}
		return null;
	}

	/**
	 *
	 */
	private static void deleteIfExists(File file) throws IOException {
		if (file.exists() && !file.delete()) {
			throw new IOException();
		}
	}

	/**
	 *
	 */
	private void readJournal() throws IOException {
		InputStream in = new BufferedInputStream(new FileInputStream(journalFile), IO_BUFFER_SIZE);
		try {
			String magic = readAsciiLine(in);
			String version = readAsciiLine(in);
			String appVersionString = readAsciiLine(in);
			String valueCountString = readAsciiLine(in);
			String blank = readAsciiLine(in);
			if (!MAGIC.equals(magic) || !VERSION_1.equals(version) || !Integer.toString(appVersion).equals(appVersionString) ||
					!Integer.toString(valueCount).equals(valueCountString) || !blank.trim().isEmpty()) {
				throw new IOException("unexpected journal header: [" + magic + ", " + version + ", " + valueCountString + ", " + blank + "]");
			}
			String readLn = readAsciiLine(in);
			while (!readLn.isEmpty()) {
				readJournalLine(readLn);
				readLn = readAsciiLine(in);
			}
		} finally {
			closeQuietly(in);
		}
	}

	/**
	 *
	 */
	private void readJournalLine(String line) throws IOException {
		String[] parts = line.split("\\s");
		if (parts.length < 2) {
			throw new IOException("unexpected journal line: " + line);
		}

		String key = parts[1];
		if (parts[0].equals(REMOVE) && parts.length == 2) {
			lruEntries.remove(key);
			return;
		}

		Entry entry = lruEntries.get(key);
		if (entry == null) {
			entry = new Entry(key);
			lruEntries.put(key, entry);
		}

		if (parts[0].equals(CLEAN) && parts.length == 2 + valueCount) {
			entry.readable = true;
			entry.currentEditor = null;
			entry.setLengths(copyOfRange(parts, parts.length));
		} else if (parts[0].equals(DIRTY) && parts.length == 2) {
			entry.currentEditor = new Editor(entry);
		} else if (!parts[0].equals(READ) || parts.length != 2) {
			throw new IOException("unexpected journal line: " + line);
		}
	}

	/**
	 * Computes the initial size and collects garbage as a part of opening the
	 * cache. Dirty entries are assumed to be inconsistent and will be deleted.
	 */
	private void processJournal() throws IOException {
		deleteIfExists(journalFileTmp);
		for (Iterator<Entry> i = lruEntries.values().iterator(); i.hasNext(); ) {
			Entry entry = i.next();
			if (entry.currentEditor == null) {
				for (int t = 0; t < valueCount; t++) {
					size += entry.lengths[t];
				}
			} else {
				entry.currentEditor = null;
				for (int t = 0; t < valueCount; t++) {
					deleteIfExists(entry.getCleanFile(t));
					deleteIfExists(entry.getDirtyFile(t));
				}
				i.remove();
			}
		}
	}

	/**
	 * Creates a new journal that omits redundant information. This replaces the
	 * current journal if it exists.
	 */
	private synchronized void rebuildJournal() throws IOException {
		if (journalWriter != null) {
			journalWriter.close();
		}

		Writer writer = new BufferedWriter(new FileWriter(journalFileTmp), IO_BUFFER_SIZE);
		writer.write(MAGIC);
		writer.write("\n");
		writer.write(VERSION_1);
		writer.write("\n");
		writer.write(Integer.toString(appVersion));
		writer.write("\n");
		writer.write(Integer.toString(valueCount));
		writer.write("\n");
		writer.write("\n");

		for (Entry entry : lruEntries.values()) {
			if (entry.currentEditor != null) {
				writer.write(DIRTY + ' ' + entry.key + '\n');
			} else {
				writer.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
			}
		}
		writer.close();
		journalFileTmp.renameTo(journalFile);
		journalWriter = new BufferedWriter(new FileWriter(journalFile, true), IO_BUFFER_SIZE);
	}

	/**
	 * Returns a snapshot of the entry named {@code key}, or null if it doesn't
	 * exist is not currently readable. If a value is returned, it is moved to
	 * the head of the LRU queue.
	 */
	public synchronized Snapshot get(String key) throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (entry == null || !entry.readable) {
			return null;
		}
		/*
		 * Open all streams eagerly to guarantee that we see a single published
		 * snapshot. If we opened streams lazily then the streams could come
		 * from different edits.
		 */
		InputStream[] ins = new InputStream[valueCount];
		try {
			for (int i = 0; i < valueCount; i++) {
				ins[i] = new FileInputStream(entry.getCleanFile(i));
			}
		} catch (FileNotFoundException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
			// a file must have been deleted manually!
			return null;
		}

		redundantOpCount++;
		journalWriter.append(READ + ' ').append(key).append(String.valueOf('\n'));
		if (journalRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}
		return new Snapshot(ins);
	}

	/**
	 * Returns an editor for the entry named {@code key}, or null if another
	 * edit is in progress.
	 */
	public synchronized Editor edit(String key) throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (entry == null) {
			entry = new Entry(key);
			lruEntries.put(key, entry);
		} else if (entry.currentEditor != null) {
			return null; // another edit is in progress
		}
		Editor editor = new Editor(entry);
		entry.currentEditor = editor;

		// flush the journal before creating files to prevent file leaks
		journalWriter.write(DIRTY + ' ' + key + '\n');
		journalWriter.flush();
		return editor;
	}

	/**
	 *
	 */
	private synchronized void completeEdit(Editor editor, boolean success)
			throws IOException {
		Entry entry = editor.entry;
		if (entry.currentEditor != editor) {
			throw new IllegalStateException();
		}

		// if this edit is creating the entry for the first time, every index
		// must have a value
		if (success && !entry.readable) {
			for (int i = 0; i < valueCount; i++) {
				if (!entry.getDirtyFile(i).exists()) {
					editor.abort();
					throw new IllegalStateException("edit didn't create file " + i);
				}
			}
		}

		for (int i = 0; i < valueCount; i++) {
			File dirty = entry.getDirtyFile(i);
			if (success) {
				if (dirty.exists()) {
					File clean = entry.getCleanFile(i);
					dirty.renameTo(clean);
					long oldLength = entry.lengths[i];
					long newLength = clean.length();
					entry.lengths[i] = newLength;
					size = size - oldLength + newLength;
				}
			} else {
				deleteIfExists(dirty);
			}
		}

		redundantOpCount++;
		entry.currentEditor = null;
		if (entry.readable | success) {
			entry.readable = true;
			journalWriter.write(CLEAN + ' ' + entry.key + entry.getLengths() + '\n');
		} else {
			lruEntries.remove(entry.key);
			journalWriter.write(REMOVE + ' ' + entry.key + '\n');
		}

		if (size > maxSize || journalRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}
	}

	/**
	 * We only rebuild the journal when it will halve the size of the journal
	 * and eliminate at least 2000 ops.
	 */
	private boolean journalRebuildRequired() {
		int REDUNDANT_OP_COMPACT_THRESHOLD = 2000;
		return redundantOpCount >= REDUNDANT_OP_COMPACT_THRESHOLD && redundantOpCount >= lruEntries.size();
	}

	/**
	 * Drops the entry for {@code key} if it exists and can be removed. Entries
	 * actively being edited cannot be removed.
	 */
	public synchronized void remove(String key) throws IOException {
		checkNotClosed();
		validateKey(key);
		Entry entry = lruEntries.get(key);
		if (entry == null || entry.currentEditor != null) {
			return;
		}

		for (int i = 0; i < valueCount; i++) {
			File file = entry.getCleanFile(i);
			if (!file.delete()) {
				throw new IOException("failed to delete " + file);
			}
			size -= entry.lengths[i];
			entry.lengths[i] = 0;
		}

		redundantOpCount++;
		journalWriter.append(REMOVE + ' ').append(key).append(String.valueOf('\n'));
		lruEntries.remove(key);

		if (journalRebuildRequired()) {
			executorService.submit(cleanupCallable);
		}

	}

	/**
	 * Returns true if this cache has been closed.
	 */
	public boolean isClosed() {
		return journalWriter == null;
	}

	private void checkNotClosed() {
		if (journalWriter == null) {
			throw new IllegalStateException("cache is closed");
		}
	}

	/**
	 * Force buffered operations to the filesystem.
	 */
	public synchronized void flush() throws IOException {
		checkNotClosed();
		trimToSize();
		journalWriter.flush();
	}

	/**
	 * Closes this cache. Stored values will remain on the filesystem.
	 */
	@Override
	public synchronized void close() throws IOException {
		if (journalWriter == null) {
			return; // already closed
		}
		for (Entry entry : new ArrayList<>(lruEntries.values())) {
			if (entry.currentEditor != null) {
				entry.currentEditor.abort();
			}
		}
		trimToSize();
		journalWriter.close();
		journalWriter = null;
	}

	/**
	 *
	 */
	private void trimToSize() throws IOException {
		while (size > maxSize) {
			// Map.Entry<String, Entry> toEvict = lruEntries.eldest();
			Map.Entry<String, Entry> toEvict = lruEntries.entrySet().iterator().next();
			remove(toEvict.getKey());
		}
	}

	/**
	 * Closes the cache and deletes all of its stored values. This will delete
	 * all files in the cache directory including files that weren't created by
	 * the cache.
	 */
	public void delete() throws IOException {
		close();
		deleteContents(directory);
	}

	/**
	 *
	 */
	private void validateKey(String key) {
		if (key.contains(" ") || key.contains("\n") || key.contains("\r")) {
			throw new IllegalArgumentException("keys must not contain spaces or newlines: \"" + key + "\"");
		}
	}

	/**
	 * A snapshot of the values for an entry.
	 */
	public static final class Snapshot implements Closeable {

		private InputStream[] ins;

		private Snapshot(InputStream[] ins) {
			this.ins = ins;
		}

		/**
		 * Returns the unbuffered stream with the value for {@code index}.
		 */
		public InputStream getInputStream(int index) {
			return ins[index];
		}


		@Override
		public void close() {
			for (InputStream in : ins) {
				closeQuietly(in);
			}
		}
	}

	/**
	 * Edits the values for an entry.
	 */
	public final class Editor {
		private Entry entry;

		private boolean hasErrors;

		private Editor(Entry entry) {
			this.entry = entry;
		}


		/**
		 * Returns a new unbuffered output stream to write the value at
		 * {@code index}. If the underlying output stream encounters errors when
		 * writing to the filesystem, this edit will be aborted when
		 * {@link #commit} is called. The returned output stream does not throw
		 * IOExceptions.
		 */
		public OutputStream newOutputStream(int index) throws IOException {
			synchronized (DiskLruCache.this) {
				if (entry.currentEditor != this) {
					throw new IllegalStateException();
				}
				return new FaultHidingOutputStream(new FileOutputStream(entry.getDirtyFile(index)));
			}
		}

		/**
		 * Commits this edit so it is visible to readers. This releases the edit
		 * lock so another edit may be started on the same key.
		 */
		public void commit() throws IOException {
			if (hasErrors) {
				completeEdit(this, false);
				remove(entry.key); // the previous entry is stale
			} else {
				completeEdit(this, true);
			}
		}

		/**
		 * Aborts this edit. This releases the edit lock so another edit may be
		 * started on the same key.
		 */
		public void abort() throws IOException {
			completeEdit(this, false);
		}

		private class FaultHidingOutputStream extends FilterOutputStream {

			private FaultHidingOutputStream(OutputStream out) {
				super(out);
			}

			@Override
			public void write(int oneByte) {
				try {
					out.write(oneByte);
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					hasErrors = true;
				}
			}

			@Override
			public void write(byte[] buffer, int offset, int length) {
				try {
					out.write(buffer, offset, length);
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					hasErrors = true;
				}
			}

			@Override
			public void close() {
				try {
					out.close();
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					hasErrors = true;
				}
			}

			@Override
			public void flush() {
				try {
					out.flush();
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					hasErrors = true;
				}
			}
		}
	}

	/**
	 *
	 */
	private final class Entry {
		/**
		 *
		 */
		private String key;

		/**
		 * Lengths of this entry's files.
		 */
		private long[] lengths;

		/**
		 * True if this entry has ever been published
		 */
		private boolean readable;

		/**
		 * The ongoing edit or null if this entry is not being edited.
		 */
		private Editor currentEditor;


		private Entry(String key) {
			this.key = key;
			lengths = new long[valueCount];
		}


		public String getLengths() {
			StringBuilder result = new StringBuilder();
			for (long size : lengths) {
				result.append(' ').append(size);
			}
			return result.toString();
		}

		/**
		 * Set lengths using decimal numbers like "10123".
		 */
		private void setLengths(String[] strings) throws IOException {
			if (strings.length != valueCount) {
				throw invalidLengths(strings);
			}
			try {
				for (int i = 0; i < strings.length; i++) {
					lengths[i] = Long.parseLong(strings[i]);
				}
			} catch (NumberFormatException e) {
				throw invalidLengths(strings);
			}
		}

		/**
		 *
		 */
		private IOException invalidLengths(String[] strings) throws IOException {
			throw new IOException("unexpected journal line: " + Arrays.toString(strings));
		}

		/**
		 *
		 */
		public File getCleanFile(int i) {
			return new File(directory, key + "." + i);
		}

		/**
		 *
		 */
		public File getDirtyFile(int i) {
			return new File(directory, key + "." + i + ".tmp");
		}
	}
}