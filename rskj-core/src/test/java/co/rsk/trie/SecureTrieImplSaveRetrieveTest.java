/*
 * This file is part of RskJ
 * Copyright (C) 2017 RSK Labs Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package co.rsk.trie;

import org.ethereum.datasource.HashMapDB;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by ajlopez on 03/04/2017.
 */
public class SecureTrieImplSaveRetrieveTest {
    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(16, store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertEquals(16, trie2.getArity());

        Assert.assertNotNull(trie2);
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expected = key.getBytes();
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(key, value, expected);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValuesUsingBinaryTree() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 0; k < 1000; k++)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertEquals(2, trie2.getArity());

        Assert.assertNotNull(trie2);
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        for (int k = 0; k < 1000; k++) {
            String key = k + "";
            byte[] expected = key.getBytes();
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(key, value, expected);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValuesInverseOrder() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(16, store, true);

        for (int k = 1000; k > 0; k--)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertNotNull(trie2);
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        for (int k = 1000; k > 0; k--) {
            String key = k + "";
            byte[] expected = key.getBytes();
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(key, value, expected);
        }
    }

    @Test
    public void updateSaveRetrieveAndGetOneThousandKeyValuesInverseOrderUsingBinaryTree() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true);

        for (int k = 1000; k > 0; k--)
            trie = trie.put(k + "", (k + "").getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertNotNull(trie2);
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        for (int k = 1000; k > 0; k--) {
            String key = k + "";
            byte[] expected = key.getBytes();
            byte[] value = trie2.get(key);
            Assert.assertArrayEquals(key, value, expected);
        }
    }
    @Test
    public void saveTrieWithKeyValues() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true).put("foo", "bar".getBytes())
                .put("bar", "baz".getBytes())
                .put("answer", "42".getBytes());

        trie.save();

        Assert.assertNotEquals(0, trie.trieSize());
        Assert.assertEquals(trie.trieSize(), map.keys().size());
    }

    @Test
    public void retrieveTrieWithHash() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(16, store, true).put("foo", "bar".getBytes())
                .put("bar", "baz".getBytes())
                .put("answer", "42".getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(16, trie2.getArity());
        Assert.assertEquals(trie.trieSize(), trie2.trieSize());
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        Assert.assertArrayEquals(trie.get("foo"), trie2.get("foo"));
        Assert.assertArrayEquals(trie.get("bar"), trie2.get("bar"));
        Assert.assertArrayEquals(trie.get("answer"), trie2.get("answer"));
    }

    @Test
    public void retrieveTrieWithHashUsingBinaryTree() {
        HashMapDB map = new HashMapDB();
        TrieStoreImpl store = new TrieStoreImpl(map);

        Trie trie = new TrieImpl(store, true).put("foo", "bar".getBytes())
                .put("bar", "baz".getBytes())
                .put("answer", "42".getBytes());

        trie.save();

        Trie trie2 = store.retrieve(trie.getHash());

        Assert.assertNotNull(trie2);
        Assert.assertEquals(2, trie2.getArity());
        Assert.assertEquals(trie.trieSize(), trie2.trieSize());
        Assert.assertArrayEquals(trie.getHash(), trie2.getHash());

        Assert.assertArrayEquals(trie.get("foo"), trie2.get("foo"));
        Assert.assertArrayEquals(trie.get("bar"), trie2.get("bar"));
        Assert.assertArrayEquals(trie.get("answer"), trie2.get("answer"));
    }
}
