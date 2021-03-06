/*
 * Copyright (C) 2011 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.net;

import static android.net.NetworkStats.SET_DEFAULT;
import static android.net.NetworkStats.SET_FOREGROUND;
import static android.net.NetworkStats.SET_DBG_VPN_IN;
import static android.net.NetworkStats.SET_DBG_VPN_OUT;
import static android.net.NetworkStats.SET_ALL;
import static android.net.NetworkStats.IFACE_ALL;
import static android.net.NetworkStats.TAG_NONE;
import static android.net.NetworkStats.UID_ALL;

import android.test.suitebuilder.annotation.SmallTest;

import com.google.android.collect.Sets;

import junit.framework.TestCase;

import java.util.HashSet;

@SmallTest
public class NetworkStatsTest extends TestCase {

    private static final String TEST_IFACE = "test0";
    private static final String TEST_IFACE2 = "test2";
    private static final int TEST_UID = 1001;
    private static final long TEST_START = 1194220800000L;

    public void testFindIndex() throws Exception {
        final NetworkStats stats = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 10)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 11)
                .addValues(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE, 1024L, 8L, 1024L, 8L, 12);

        assertEquals(2, stats.findIndex(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE));
        assertEquals(2, stats.findIndex(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE));
        assertEquals(0, stats.findIndex(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE));
        assertEquals(-1, stats.findIndex(TEST_IFACE, 6, SET_DEFAULT, TAG_NONE));
    }

    public void testFindIndexHinted() {
        final NetworkStats stats = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 10)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 11)
                .addValues(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE, 1024L, 8L, 1024L, 8L, 12)
                .addValues(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, 1024L, 8L, 0L, 0L, 10)
                .addValues(TEST_IFACE2, 101, SET_DEFAULT, 0xF00D, 0L, 0L, 1024L, 8L, 11)
                .addValues(TEST_IFACE2, 102, SET_DEFAULT, TAG_NONE, 1024L, 8L, 1024L, 8L, 12);

        // verify that we correctly find across regardless of hinting
        for (int hint = 0; hint < stats.size(); hint++) {
            assertEquals(0, stats.findIndexHinted(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, hint));
            assertEquals(1, stats.findIndexHinted(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, hint));
            assertEquals(2, stats.findIndexHinted(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE, hint));
            assertEquals(3, stats.findIndexHinted(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, hint));
            assertEquals(4, stats.findIndexHinted(TEST_IFACE2, 101, SET_DEFAULT, 0xF00D, hint));
            assertEquals(5, stats.findIndexHinted(TEST_IFACE2, 102, SET_DEFAULT, TAG_NONE, hint));
            assertEquals(-1, stats.findIndexHinted(TEST_IFACE, 6, SET_DEFAULT, TAG_NONE, hint));
        }
    }

    public void testAddEntryGrow() throws Exception {
        final NetworkStats stats = new NetworkStats(TEST_START, 2);

        assertEquals(0, stats.size());
        assertEquals(2, stats.internalSize());

        stats.addValues(TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 1L, 1L, 2L, 2L, 3);
        stats.addValues(TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 2L, 2L, 2L, 2L, 4);

        assertEquals(2, stats.size());
        assertEquals(2, stats.internalSize());

        stats.addValues(TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 3L, 30L, 4L, 40L, 7);
        stats.addValues(TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 4L, 40L, 4L, 40L, 8);
        stats.addValues(TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 5L, 50L, 5L, 50L, 10);

        assertEquals(5, stats.size());
        assertTrue(stats.internalSize() >= 5);

        assertValues(stats, 0, TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 1L, 1L, 2L, 2L, 3);
        assertValues(stats, 1, TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 2L, 2L, 2L, 2L, 4);
        assertValues(stats, 2, TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 3L, 30L, 4L, 40L, 7);
        assertValues(stats, 3, TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 4L, 40L, 4L, 40L, 8);
        assertValues(stats, 4, TEST_IFACE, TEST_UID, SET_DEFAULT, TAG_NONE, 5L, 50L, 5L, 50L, 10);
    }

    public void testCombineExisting() throws Exception {
        final NetworkStats stats = new NetworkStats(TEST_START, 10);

        stats.addValues(TEST_IFACE, 1001, SET_DEFAULT, TAG_NONE, 512L, 4L, 256L, 2L, 10);
        stats.addValues(TEST_IFACE, 1001, SET_DEFAULT, 0xff, 128L, 1L, 128L, 1L, 2);
        stats.combineValues(TEST_IFACE, 1001, SET_DEFAULT, TAG_NONE, -128L, -1L, -128L, -1L, -1);

        assertValues(stats, 0, TEST_IFACE, 1001, SET_DEFAULT, TAG_NONE, 384L, 3L, 128L, 1L, 9);
        assertValues(stats, 1, TEST_IFACE, 1001, SET_DEFAULT, 0xff, 128L, 1L, 128L, 1L, 2);

        // now try combining that should create row
        stats.combineValues(TEST_IFACE, 5005, SET_DEFAULT, TAG_NONE, 128L, 1L, 128L, 1L, 3);
        assertValues(stats, 2, TEST_IFACE, 5005, SET_DEFAULT, TAG_NONE, 128L, 1L, 128L, 1L, 3);
        stats.combineValues(TEST_IFACE, 5005, SET_DEFAULT, TAG_NONE, 128L, 1L, 128L, 1L, 3);
        assertValues(stats, 2, TEST_IFACE, 5005, SET_DEFAULT, TAG_NONE, 256L, 2L, 256L, 2L, 6);
    }

    public void testSubtractIdenticalData() throws Exception {
        final NetworkStats before = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 11)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 12);

        final NetworkStats after = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 11)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 12);

        final NetworkStats result = after.subtract(before);

        // identical data should result in zero delta
        assertValues(result, 0, TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 0L, 0L, 0L, 0L, 0);
        assertValues(result, 1, TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 0L, 0L, 0);
    }

    public void testSubtractIdenticalRows() throws Exception {
        final NetworkStats before = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 11)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 12);

        final NetworkStats after = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1025L, 9L, 2L, 1L, 15)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 3L, 1L, 1028L, 9L, 20);

        final NetworkStats result = after.subtract(before);

        // expect delta between measurements
        assertValues(result, 0, TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1L, 1L, 2L, 1L, 4);
        assertValues(result, 1, TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 3L, 1L, 4L, 1L, 8);
    }

    public void testSubtractNewRows() throws Exception {
        final NetworkStats before = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 11)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 12);

        final NetworkStats after = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 1024L, 8L, 0L, 0L, 11)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 1024L, 8L, 12)
                .addValues(TEST_IFACE, 102, SET_DEFAULT, TAG_NONE, 1024L, 8L, 1024L, 8L, 20);

        final NetworkStats result = after.subtract(before);

        // its okay to have new rows
        assertValues(result, 0, TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 0L, 0L, 0L, 0L, 0);
        assertValues(result, 1, TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 0L, 0L, 0L, 0L, 0);
        assertValues(result, 2, TEST_IFACE, 102, SET_DEFAULT, TAG_NONE, 1024L, 8L, 1024L, 8L, 20);
    }

    public void testSubtractMissingRows() throws Exception {
        final NetworkStats before = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, UID_ALL, SET_DEFAULT, TAG_NONE, 1024L, 0L, 0L, 0L, 0)
                .addValues(TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 2048L, 0L, 0L, 0L, 0);

        final NetworkStats after = new NetworkStats(TEST_START, 1)
                .addValues(TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 2049L, 2L, 3L, 4L, 0);

        final NetworkStats result = after.subtract(before);

        // should silently drop omitted rows
        assertEquals(1, result.size());
        assertValues(result, 0, TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 1L, 2L, 3L, 4L, 0);
        assertEquals(4L, result.getTotalBytes());
    }

    public void testTotalBytes() throws Exception {
        final NetworkStats iface = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, UID_ALL, SET_DEFAULT, TAG_NONE, 128L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 256L, 0L, 0L, 0L, 0L);
        assertEquals(384L, iface.getTotalBytes());

        final NetworkStats uidSet = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_FOREGROUND, TAG_NONE, 32L, 0L, 0L, 0L, 0L);
        assertEquals(96L, uidSet.getTotalBytes());

        final NetworkStats uidTag = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 16L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 16L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, 0xF00D, 8L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, 16L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 16L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, 0xF00D, 8L, 0L, 0L, 0L, 0L);
        assertEquals(64L, uidTag.getTotalBytes());
    }

    public void testGroupedByIfaceEmpty() throws Exception {
        final NetworkStats uidStats = new NetworkStats(TEST_START, 3);
        final NetworkStats grouped = uidStats.groupedByIface();

        assertEquals(0, uidStats.size());
        assertEquals(0, grouped.size());
    }

    public void testGroupedByIfaceAll() throws Exception {
        final NetworkStats uidStats = new NetworkStats(TEST_START, 3)
                .addValues(IFACE_ALL, 100, SET_ALL, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(IFACE_ALL, 101, SET_FOREGROUND, TAG_NONE, 128L, 8L, 0L, 2L, 20L);
        final NetworkStats grouped = uidStats.groupedByIface();

        assertEquals(2, uidStats.size());
        assertEquals(1, grouped.size());

        assertValues(grouped, 0, IFACE_ALL, UID_ALL, SET_ALL, TAG_NONE, 256L, 16L, 0L, 4L, 0L);
    }

    public void testGroupedByIface() throws Exception {
        final NetworkStats uidStats = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, 0xF00D, 64L, 4L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, 0xF00D, 128L, 8L, 0L, 0L, 0L);

        final NetworkStats grouped = uidStats.groupedByIface();

        assertEquals(6, uidStats.size());

        assertEquals(2, grouped.size());
        assertValues(grouped, 0, TEST_IFACE, UID_ALL, SET_ALL, TAG_NONE, 256L, 16L, 0L, 2L, 0L);
        assertValues(grouped, 1, TEST_IFACE2, UID_ALL, SET_ALL, TAG_NONE, 1024L, 64L, 0L, 0L, 0L);
    }

    public void testAddAllValues() {
        final NetworkStats first = new NetworkStats(TEST_START, 5)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 100, SET_FOREGROUND, TAG_NONE, 32L, 0L, 0L, 0L, 0L);

        final NetworkStats second = new NetworkStats(TEST_START, 2)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L);

        first.combineAllValues(second);

        assertEquals(3, first.size());
        assertValues(first, 0, TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 64L, 0L, 0L, 0L, 0L);
        assertValues(first, 1, TEST_IFACE, 100, SET_FOREGROUND, TAG_NONE, 32L, 0L, 0L, 0L, 0L);
        assertValues(first, 2, TEST_IFACE2, UID_ALL, SET_DEFAULT, TAG_NONE, 32L, 0L, 0L, 0L, 0L);
    }

    public void testGetTotal() {
        final NetworkStats stats = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, 0xF00D, 64L, 4L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, 0xF00D, 128L, 8L, 0L, 0L, 0L);

        assertValues(stats.getTotal(null), 1280L, 80L, 0L, 2L, 20L);
        assertValues(stats.getTotal(null, 100), 1152L, 72L, 0L, 2L, 20L);
        assertValues(stats.getTotal(null, 101), 128L, 8L, 0L, 0L, 0L);

        final HashSet<String> ifaces = Sets.newHashSet();
        assertValues(stats.getTotal(null, ifaces), 0L, 0L, 0L, 0L, 0L);

        ifaces.add(TEST_IFACE2);
        assertValues(stats.getTotal(null, ifaces), 1024L, 64L, 0L, 0L, 0L);
    }

    public void testWithoutUid() throws Exception {
        final NetworkStats before = new NetworkStats(TEST_START, 3)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, 0xF00D, 64L, 4L, 0L, 0L, 0L)
                .addValues(TEST_IFACE2, 100, SET_FOREGROUND, TAG_NONE, 512L, 32L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 0L, 0L)
                .addValues(TEST_IFACE, 101, SET_DEFAULT, 0xF00D, 128L, 8L, 0L, 0L, 0L);

        final NetworkStats after = before.withoutUids(new int[] { 100 });
        assertEquals(6, before.size());
        assertEquals(2, after.size());
        assertValues(after, 0, TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 0L, 0L);
        assertValues(after, 1, TEST_IFACE, 101, SET_DEFAULT, 0xF00D, 128L, 8L, 0L, 0L, 0L);
    }

    public void testClone() throws Exception {
        final NetworkStats original = new NetworkStats(TEST_START, 5)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 512L, 32L, 0L, 0L, 0L);

        // make clone and mutate original
        final NetworkStats clone = original.clone();
        original.addValues(TEST_IFACE, 101, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 0L, 0L);

        assertEquals(3, original.size());
        assertEquals(2, clone.size());

        assertEquals(128L + 512L + 128L, original.getTotalBytes());
        assertEquals(128L + 512L, clone.getTotalBytes());
    }

    public void testAddWhenEmpty() throws Exception {
        final NetworkStats red = new NetworkStats(TEST_START, -1);
        final NetworkStats blue = new NetworkStats(TEST_START, 5)
                .addValues(TEST_IFACE, 100, SET_DEFAULT, TAG_NONE, 128L, 8L, 0L, 2L, 20L)
                .addValues(TEST_IFACE2, 100, SET_DEFAULT, TAG_NONE, 512L, 32L, 0L, 0L, 0L);

        // We're mostly checking that we don't crash
        red.combineAllValues(blue);
    }

    public void testMigrateTun() throws Exception {
        final int tunUid = 10030;
        final String tunIface = "tun0";
        final String underlyingIface = "wlan0";
        final int testTag1 = 8888;
        NetworkStats delta = new NetworkStats(TEST_START, 17)
            .addValues(tunIface, 10100, SET_DEFAULT, TAG_NONE, 39605L, 46L, 12259L, 55L, 0L)
            .addValues(tunIface, 10100, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L)
            .addValues(tunIface, 10120, SET_DEFAULT, TAG_NONE, 72667L, 197L, 43909L, 241L, 0L)
            .addValues(tunIface, 10120, SET_FOREGROUND, TAG_NONE, 9297L, 17L, 4128L, 21L, 0L)
            // VPN package also uses some traffic through unprotected network.
            .addValues(tunIface, tunUid, SET_DEFAULT, TAG_NONE, 4983L, 10L, 1801L, 12L, 0L)
            .addValues(tunIface, tunUid, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L)
            // Tag entries
            .addValues(tunIface, 10120, SET_DEFAULT, testTag1, 21691L, 41L, 13820L, 51L, 0L)
            .addValues(tunIface, 10120, SET_FOREGROUND, testTag1, 1281L, 2L, 665L, 2L, 0L)
            // Irrelevant entries
            .addValues(TEST_IFACE, 10100, SET_DEFAULT, TAG_NONE, 1685L, 5L, 2070L, 6L, 0L)
            // Underlying Iface entries
            .addValues(underlyingIface, 10100, SET_DEFAULT, TAG_NONE, 5178L, 8L, 2139L, 11L, 0L)
            .addValues(underlyingIface, 10100, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L)
            .addValues(underlyingIface, tunUid, SET_DEFAULT, TAG_NONE, 149873L, 287L,
                    59217L /* smaller than sum(tun0) */, 299L /* smaller than sum(tun0) */, 0L)
            .addValues(underlyingIface, tunUid, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L);

        assertTrue(delta.migrateTun(tunUid, tunIface, underlyingIface));
        assertEquals(21, delta.size());

        // tunIface and TEST_IFACE entries are not changed.
        assertValues(delta, 0, tunIface, 10100, SET_DEFAULT, TAG_NONE,
                39605L, 46L, 12259L, 55L, 0L);
        assertValues(delta, 1, tunIface, 10100, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L);
        assertValues(delta, 2, tunIface, 10120, SET_DEFAULT, TAG_NONE,
                72667L, 197L, 43909L, 241L, 0L);
        assertValues(delta, 3, tunIface, 10120, SET_FOREGROUND, TAG_NONE,
                9297L, 17L, 4128L, 21L, 0L);
        assertValues(delta, 4, tunIface, tunUid, SET_DEFAULT, TAG_NONE,
                4983L, 10L, 1801L, 12L, 0L);
        assertValues(delta, 5, tunIface, tunUid, SET_FOREGROUND, TAG_NONE, 0L, 0L, 0L, 0L, 0L);
        assertValues(delta, 6, tunIface, 10120, SET_DEFAULT, testTag1,
                21691L, 41L, 13820L, 51L, 0L);
        assertValues(delta, 7, tunIface, 10120, SET_FOREGROUND, testTag1, 1281L, 2L, 665L, 2L, 0L);
        assertValues(delta, 8, TEST_IFACE, 10100, SET_DEFAULT, TAG_NONE, 1685L, 5L, 2070L, 6L, 0L);

        // Existing underlying Iface entries are updated
        assertValues(delta, 9, underlyingIface, 10100, SET_DEFAULT, TAG_NONE,
                44783L, 54L, 13829L, 60L, 0L);
        assertValues(delta, 10, underlyingIface, 10100, SET_FOREGROUND, TAG_NONE,
                0L, 0L, 0L, 0L, 0L);

        // VPN underlying Iface entries are updated
        assertValues(delta, 11, underlyingIface, tunUid, SET_DEFAULT, TAG_NONE,
                28304L, 27L, 1719L, 12L, 0L);
        assertValues(delta, 12, underlyingIface, tunUid, SET_FOREGROUND, TAG_NONE,
                0L, 0L, 0L, 0L, 0L);

        // New entries are added for new application's underlying Iface traffic
        assertContains(delta, underlyingIface, 10120, SET_DEFAULT, TAG_NONE,
                72667L, 197L, 41872l, 219L, 0L);
        assertContains(delta, underlyingIface, 10120, SET_FOREGROUND, TAG_NONE,
                9297L, 17L, 3936, 19L, 0L);
        assertContains(delta, underlyingIface, 10120, SET_DEFAULT, testTag1,
                21691L, 41L, 13179L, 46L, 0L);
        assertContains(delta, underlyingIface, 10120, SET_FOREGROUND, testTag1,
                1281L, 2L, 634L, 1L, 0L);

        // New entries are added for debug purpose
        assertContains(delta, underlyingIface, 10100, SET_DBG_VPN_IN, TAG_NONE,
                39605L, 46L, 11690, 49, 0);
        assertContains(delta, underlyingIface, 10120, SET_DBG_VPN_IN, TAG_NONE,
                81964, 214, 45808, 238, 0);
        assertContains(delta, underlyingIface, tunUid, SET_DBG_VPN_IN, TAG_NONE,
                4983, 10, 1717, 10, 0);
        assertContains(delta, underlyingIface, tunUid, SET_DBG_VPN_OUT, TAG_NONE,
                126552, 270, 59215, 297, 0);

    }

    private static void assertContains(NetworkStats stats,  String iface, int uid, int set,
            int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        int index = stats.findIndex(iface, uid, set, tag);
        assertTrue(index != -1);
        assertValues(stats, index, iface, uid, set, tag,
                rxBytes, rxPackets, txBytes, txPackets, operations);
    }

    private static void assertValues(NetworkStats stats, int index, String iface, int uid, int set,
            int tag, long rxBytes, long rxPackets, long txBytes, long txPackets, long operations) {
        final NetworkStats.Entry entry = stats.getValues(index, null);
        assertValues(entry, iface, uid, set, tag);
        assertValues(entry, rxBytes, rxPackets, txBytes, txPackets, operations);
    }

    private static void assertValues(
            NetworkStats.Entry entry, String iface, int uid, int set, int tag) {
        assertEquals(iface, entry.iface);
        assertEquals(uid, entry.uid);
        assertEquals(set, entry.set);
        assertEquals(tag, entry.tag);
    }

    private static void assertValues(NetworkStats.Entry entry, long rxBytes, long rxPackets,
            long txBytes, long txPackets, long operations) {
        assertEquals(rxBytes, entry.rxBytes);
        assertEquals(rxPackets, entry.rxPackets);
        assertEquals(txBytes, entry.txBytes);
        assertEquals(txPackets, entry.txPackets);
        assertEquals(operations, entry.operations);
    }

}
