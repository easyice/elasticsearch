/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0; you may not use this file except in compliance with the Elastic License
 * 2.0.
 */
package org.elasticsearch.xpack.monitoring.collector.node;

import org.elasticsearch.Version;
import org.elasticsearch.action.admin.cluster.node.stats.NodeStats;
import org.elasticsearch.action.admin.indices.stats.CommonStats;
import org.elasticsearch.action.admin.indices.stats.CommonStatsFlags;
import org.elasticsearch.cluster.node.DiscoveryNode;
import org.elasticsearch.common.bytes.BytesReference;
import org.elasticsearch.common.transport.TransportAddress;
import org.elasticsearch.common.xcontent.XContentHelper;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.cache.query.QueryCacheStats;
import org.elasticsearch.index.cache.request.RequestCacheStats;
import org.elasticsearch.index.engine.SegmentsStats;
import org.elasticsearch.index.fielddata.FieldDataStats;
import org.elasticsearch.index.search.stats.SearchStats;
import org.elasticsearch.index.shard.DocsStats;
import org.elasticsearch.index.shard.IndexingStats;
import org.elasticsearch.index.store.StoreStats;
import org.elasticsearch.indices.NodeIndicesStats;
import org.elasticsearch.monitor.fs.FsInfo;
import org.elasticsearch.monitor.jvm.JvmStats;
import org.elasticsearch.monitor.os.OsStats;
import org.elasticsearch.monitor.process.ProcessStats;
import org.elasticsearch.threadpool.ThreadPoolStats;
import org.elasticsearch.xpack.core.monitoring.MonitoredSystem;
import org.elasticsearch.xpack.core.monitoring.exporter.MonitoringDoc;
import org.elasticsearch.xpack.monitoring.exporter.BaseFilteredMonitoringDocTestCase;
import org.junit.Before;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

public class NodeStatsMonitoringDocTests extends BaseFilteredMonitoringDocTestCase<NodeStatsMonitoringDoc> {

    private String nodeId;
    private boolean isMaster;
    private NodeStats nodeStats;
    private boolean mlockall;

    @Override
    @Before
    public void setUp() throws Exception {
        super.setUp();
        nodeId = randomAlphaOfLength(5);
        isMaster = randomBoolean();
        nodeStats = mock(NodeStats.class);
        mlockall = randomBoolean();
    }

    @Override
    protected NodeStatsMonitoringDoc createMonitoringDoc(String cluster, long timestamp, long interval, MonitoringDoc.Node node,
                                                         MonitoredSystem system, String type, String id) {
        return new NodeStatsMonitoringDoc(cluster, timestamp, interval, node, nodeId, isMaster, nodeStats, mlockall);
    }

    @Override
    protected void assertFilteredMonitoringDoc(final NodeStatsMonitoringDoc document) {
        assertThat(document.getSystem(), is(MonitoredSystem.ES));
        assertThat(document.getType(), is(NodeStatsMonitoringDoc.TYPE));
        assertThat(document.getId(), nullValue());

        assertThat(document.isNodeMaster(), equalTo(isMaster));
        assertThat(document.getNodeId(), equalTo(nodeId));
        assertThat(document.getNodeStats(), is(nodeStats));
        assertThat(document.isMlockall(), equalTo(mlockall));
    }

    @Override
    protected Set<String> getExpectedXContentFilters() {
        return NodeStatsMonitoringDoc.XCONTENT_FILTERS;
    }

    public void testConstructorNodeIdMustNotBeNull() {
        expectThrows(NullPointerException.class, () ->
                new NodeStatsMonitoringDoc(cluster, timestamp, interval, node, null, isMaster, nodeStats, mlockall));
    }

    public void testConstructorNodeStatsMustNotBeNull() {
        expectThrows(NullPointerException.class, () ->
                new NodeStatsMonitoringDoc(cluster, timestamp, interval, node, nodeId, isMaster, null, mlockall));
    }

    @Override
    public void testToXContent() throws IOException {
        final MonitoringDoc.Node node = new MonitoringDoc.Node("_uuid", "_host", "_addr", "_ip", "_name", 1504169190855L);
        final NodeStats nodeStats = mockNodeStats();

        final NodeStatsMonitoringDoc doc =
                new NodeStatsMonitoringDoc("_cluster", 1502107402133L, 1506593717631L, node, "_node_id", true, nodeStats, false);

        final BytesReference xContent = XContentHelper.toXContent(doc, XContentType.JSON, false);
        final String expected = XContentHelper.stripWhitespace(
            "{"
                + "  \"cluster_uuid\": \"_cluster\","
                + "  \"timestamp\": \"2017-08-07T12:03:22.133Z\","
                + "  \"interval_ms\": 1506593717631,"
                + "  \"type\": \"node_stats\","
                + "  \"source_node\": {"
                + "    \"uuid\": \"_uuid\","
                + "    \"host\": \"_host\","
                + "    \"transport_address\": \"_addr\","
                + "    \"ip\": \"_ip\","
                + "    \"name\": \"_name\","
                + "    \"timestamp\": \"2017-08-31T08:46:30.855Z\""
                + "  },"
                + "  \"node_stats\": {"
                + "    \"node_id\": \"_node_id\","
                + "    \"node_master\": true,"
                + "    \"mlockall\": false,"
                + "    \"indices\": {"
                + "      \"docs\": {"
                + "        \"count\": 1"
                + "      },"
                + "      \"store\": {"
                + "        \"size_in_bytes\": 4"
                + "      },"
                + "      \"indexing\": {"
                + "        \"index_total\": 5,"
                + "        \"index_time_in_millis\": 6,"
                + "        \"throttle_time_in_millis\": 8"
                + "      },"
                + "      \"search\": {"
                + "        \"query_total\": 17,"
                + "        \"query_time_in_millis\": 18"
                + "      },"
                + "      \"query_cache\": {"
                + "        \"memory_size_in_bytes\": 9,"
                + "        \"hit_count\": 10,"
                + "        \"miss_count\": 11,"
                + "        \"evictions\": 13"
                + "      },"
                + "      \"fielddata\": {"
                + "        \"memory_size_in_bytes\": 2,"
                + "        \"evictions\": 3"
                + "      },"
                + "      \"segments\": {"
                + "        \"count\": 19,"
                + "        \"memory_in_bytes\": 20,"
                + "        \"terms_memory_in_bytes\": 21,"
                + "        \"stored_fields_memory_in_bytes\": 22,"
                + "        \"term_vectors_memory_in_bytes\": 23,"
                + "        \"norms_memory_in_bytes\": 24,"
                + "        \"points_memory_in_bytes\": 25,"
                + "        \"doc_values_memory_in_bytes\": 26,"
                + "        \"index_writer_memory_in_bytes\": 27,"
                + "        \"version_map_memory_in_bytes\": 28,"
                + "        \"fixed_bit_set_memory_in_bytes\": 29"
                + "      },"
                + "      \"request_cache\": {"
                + "        \"memory_size_in_bytes\": 13,"
                + "        \"evictions\": 14,"
                + "        \"hit_count\": 15,"
                + "        \"miss_count\": 16"
                + "      },"
                + "      \"bulk\": {"
                + "        \"total_operations\": 0,"
                + "        \"total_time_in_millis\": 0,"
                + "        \"total_size_in_bytes\": 0,"
                + "        \"avg_time_in_millis\": 0,"
                + "        \"avg_size_in_bytes\": 0"
                + "      }"
                + "    },"
                + "    \"os\": {"
                + "      \"cpu\": {"
                + "        \"load_average\": {"
                + "          \"1m\": 43.0,"
                + "          \"5m\": 44.0,"
                + "          \"15m\": 45.0"
                + "        }"
                + "      },"
                + "      \"cgroup\": {"
                + "        \"cpuacct\": {"
                + "          \"control_group\": \"_cpu_acct_ctrl_group\","
                + "          \"usage_nanos\": 49"
                + "        },"
                + "        \"cpu\": {"
                + "          \"control_group\": \"_cpu_ctrl_group\","
                + "          \"cfs_period_micros\": 50,"
                + "          \"cfs_quota_micros\": 51,"
                + "          \"stat\": {"
                + "            \"number_of_elapsed_periods\": 46,"
                + "            \"number_of_times_throttled\": 47,"
                + "            \"time_throttled_nanos\": 48"
                + "          }"
                + "        },"
                + "        \"memory\": {"
                + "          \"control_group\": \"_memory_ctrl_group\","
                + "          \"limit_in_bytes\": \"2000000000\","
                + "          \"usage_in_bytes\": \"1000000000\""
                + "        }"
                + "      }"
                + "    },"
                + "    \"process\": {"
                + "      \"open_file_descriptors\": 53,"
                + "      \"max_file_descriptors\": 54,"
                + "      \"cpu\": {"
                + "        \"percent\": 52"
                + "      }"
                + "    },"
                + "    \"jvm\": {"
                + "      \"mem\": {"
                + "        \"heap_used_in_bytes\": 55,"
                + "        \"heap_used_percent\": 98,"
                + "        \"heap_max_in_bytes\": 56"
                + "      },"
                + "      \"gc\": {"
                + "        \"collectors\": {"
                + "          \"young\": {"
                + "            \"collection_count\": 57,"
                + "            \"collection_time_in_millis\": 58"
                + "          },"
                + "          \"old\": {"
                + "            \"collection_count\": 59,"
                + "            \"collection_time_in_millis\": 60"
                + "          }"
                + "        }"
                + "      }"
                + "    },"
                + "    \"thread_pool\": {"
                + "      \"generic\": {"
                + "        \"threads\": 61,"
                + "        \"queue\": 62,"
                + "        \"rejected\": 63"
                + "      },"
                + "      \"get\": {"
                + "        \"threads\": 64,"
                + "        \"queue\": 65,"
                + "        \"rejected\": 66"
                + "      },"
                + "      \"management\": {"
                + "        \"threads\": 67,"
                + "        \"queue\": 68,"
                + "        \"rejected\": 69"
                + "      },"
                + "      \"search\": {"
                + "        \"threads\": 70,"
                + "        \"queue\": 71,"
                + "        \"rejected\": 72"
                + "      },"
                + "      \"watcher\": {"
                + "        \"threads\": 73,"
                + "        \"queue\": 74,"
                + "        \"rejected\": 75"
                + "      },"
                + "      \"write\": {"
                + "        \"threads\": 76,"
                + "        \"queue\": 77,"
                + "        \"rejected\": 78"
                + "      }"
                + "    },"
                + "    \"fs\": {"
                + "      \"total\": {"
                + "        \"total_in_bytes\": 40,"
                + "        \"free_in_bytes\": 41,"
                + "        \"available_in_bytes\": 42"
                + "      },"
                + "      \"io_stats\": {"
                + "        \"total\": {"
                + "          \"operations\": 10,"
                + "          \"read_operations\": 5,"
                + "          \"write_operations\": 5,"
                + "          \"read_kilobytes\": 2,"
                + "          \"write_kilobytes\": 2"
                + "        }"
                + "      }"
                + "    }"
                + "  }"
                + "}"
        );
        
        assertEquals(expected, xContent.utf8ToString());
    }

    private static NodeStats mockNodeStats() {

        // This value is used in constructors of various stats objects,
        // when the value is not printed out in the final XContent.
        final long no = -1;

        // This value is used in constructors of various stats objects,
        // when the value is printed out in the XContent. Must be
        // incremented for each usage.
        long iota = 0L;

        // Indices
        final CommonStats indicesCommonStats = new CommonStats(CommonStatsFlags.ALL);
        indicesCommonStats.getDocs().add(new DocsStats(++iota, no, randomNonNegativeLong()));
        indicesCommonStats.getFieldData().add(new FieldDataStats(++iota, ++iota, null));
        indicesCommonStats.getStore().add(new StoreStats(++iota, no));

        final IndexingStats.Stats indexingStats = new IndexingStats.Stats(++iota, ++iota, ++iota, no, no, no, no, no, false, ++iota);
        indicesCommonStats.getIndexing().add(new IndexingStats(indexingStats));
        indicesCommonStats.getQueryCache().add(new QueryCacheStats(++iota, ++iota, ++iota, ++iota, no));
        indicesCommonStats.getRequestCache().add(new RequestCacheStats(++iota, ++iota, ++iota, ++iota));

        final SearchStats.Stats searchStats = new SearchStats.Stats(++iota, ++iota, no, no, no, no, no, no, no, no, no, no);
        indicesCommonStats.getSearch().add(new SearchStats(searchStats, no, null));

        final SegmentsStats segmentsStats = new SegmentsStats();
        segmentsStats.add(++iota, ++iota);
        segmentsStats.addTermsMemoryInBytes(++iota);
        segmentsStats.addStoredFieldsMemoryInBytes(++iota);
        segmentsStats.addTermVectorsMemoryInBytes(++iota);
        segmentsStats.addNormsMemoryInBytes(++iota);
        segmentsStats.addPointsMemoryInBytes(++iota);
        segmentsStats.addDocValuesMemoryInBytes(++iota);
        segmentsStats.addIndexWriterMemoryInBytes(++iota);
        segmentsStats.addVersionMapMemoryInBytes(++iota);
        segmentsStats.addBitsetMemoryInBytes(++iota);
        indicesCommonStats.getSegments().add(segmentsStats);

        final NodeIndicesStats indices = new NodeIndicesStats(indicesCommonStats, emptyMap());

        // Filesystem
        final FsInfo.DeviceStats ioStatsOne = new FsInfo.DeviceStats((int) no, (int) no, null, ++iota, ++iota, ++iota, ++iota,++iota, null);
        final FsInfo.DeviceStats ioStatsTwo = new FsInfo.DeviceStats((int) no, (int) no, null, ++iota, ++iota, ++iota, ++iota, ++iota,
            ioStatsOne);

        final FsInfo.IoStats ioStats = new FsInfo.IoStats(new FsInfo.DeviceStats[]{ioStatsTwo});
        final FsInfo fs = new FsInfo(no, ioStats, new FsInfo.Path[]{new FsInfo.Path(null, null, ++iota, ++iota, ++iota)});

        // Os
        final OsStats.Cpu osCpu = new OsStats.Cpu((short) no, new double[]{++iota, ++iota, ++iota});
        final OsStats.Cgroup.CpuStat osCpuStat = new OsStats.Cgroup.CpuStat(++iota, ++iota, ++iota);
        final OsStats.Cgroup osCgroup = new OsStats.Cgroup("_cpu_acct_ctrl_group", ++iota, "_cpu_ctrl_group", ++iota, ++iota, osCpuStat,
                "_memory_ctrl_group", "2000000000", "1000000000");

        final OsStats.Mem osMem = new OsStats.Mem(0, 0);
        final OsStats.Swap osSwap = new OsStats.Swap(0, 0);
        final OsStats os = new OsStats(no, osCpu, osMem, osSwap, osCgroup);

        // Process
        final ProcessStats.Cpu processCpu = new ProcessStats.Cpu((short) ++iota, no);
        final ProcessStats process = new ProcessStats(no, ++iota, ++iota, processCpu, null);

        // Jvm
        final JvmStats.Threads jvmThreads = new JvmStats.Threads((int) no, (int) no);
        final JvmStats.Classes jvmClasses = new JvmStats.Classes(no, no, no);
        final JvmStats.Mem jvmMem = new JvmStats.Mem(no, ++iota, ++iota, no, no, emptyList());
        final JvmStats.GarbageCollectors gcs = new JvmStats.GarbageCollectors(new JvmStats.GarbageCollector[]{
                new JvmStats.GarbageCollector("young", ++iota, ++iota),
                new JvmStats.GarbageCollector("old", ++iota, ++iota)});
        final JvmStats jvm = new JvmStats(no, no, jvmMem, jvmThreads, gcs, emptyList(), jvmClasses);

        // Threadpools
        final List<ThreadPoolStats.Stats> threadpools = new ArrayList<>();
        threadpools.add(new ThreadPoolStats.Stats("generic", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        threadpools.add(new ThreadPoolStats.Stats("get", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        threadpools.add(new ThreadPoolStats.Stats("management", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        threadpools.add(new ThreadPoolStats.Stats("search", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        threadpools.add(new ThreadPoolStats.Stats("watcher", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        threadpools.add(new ThreadPoolStats.Stats("write", (int) ++iota, (int) ++iota, (int) no, ++iota, (int) no, no));
        final ThreadPoolStats threadPool = new ThreadPoolStats(threadpools);

        final DiscoveryNode discoveryNode = new DiscoveryNode("_node_name",
                                                                "_node_id",
                                                                "_ephemeral_id",
                                                                "_host_name",
                                                                "_host_address",
                                                                new TransportAddress(TransportAddress.META_ADDRESS, 1234),
                                                                emptyMap(),
                                                                emptySet(),
                                                                Version.CURRENT);

        return new NodeStats(discoveryNode, no, indices, os, process, jvm, threadPool, fs, null, null, null, null, null, null, null, null);
    }
}
