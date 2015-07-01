/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchmark.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchmark.driver.Benchmark;
import com.teradata.benchmark.driver.BenchmarkProperties;
import com.teradata.benchmark.driver.Query;
import org.assertj.core.api.MapAssert;
import org.assertj.core.data.MapEntry;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.data.MapEntry.entry;

public class BenchmarkDescriptorTest
{

    private BenchmarkProperties benchmarkProperties;

    private BenchmarkLoader benchmarkLoader;

    @Before
    public void setupBenchmarkLoader()
    {
        QueryLoader queryLoader = mockQueryLoader();
        benchmarkProperties = new BenchmarkProperties();
        BenchmarkNameGenerator benchmarkNameGenerator = new BenchmarkNameGenerator();

        benchmarkLoader = new BenchmarkLoader();

        ReflectionTestUtils.setField(benchmarkLoader, "properties", benchmarkProperties);
        ReflectionTestUtils.setField(benchmarkLoader, "queryLoader", queryLoader);
        ReflectionTestUtils.setField(benchmarkLoader, "benchmarkNameGenerator", benchmarkNameGenerator);
        ReflectionTestUtils.setField(benchmarkNameGenerator, "properties", benchmarkProperties);
    }

    private QueryLoader mockQueryLoader()
    {
        return new QueryLoader()
        {
            @Override
            public Query loadFromFile(String queryName, Map<String, ?> attributes)
            {
                return new Query(queryName, "");
            }
        };
    }

    @Test
    public void shouldLoadSimpleBenchmark()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("simple-benchmark");
        assertThat(benchmarks).hasSize(1);

        Benchmark benchmark = benchmarks.get(0);
        assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        assertThat(benchmark.getDataSource()).isEqualTo("foo");
        assertThat(benchmark.getRuns()).isEqualTo(3);
        assertThat(benchmark.getConcurrency()).isEqualTo(1);
        assertThat(benchmark.getBeforeBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op", "no-op2"));
        assertThat(benchmark.getAfterBenchmarkMacros()).isEqualTo(ImmutableList.of("no-op2"));
        assertThat(benchmark.getPrewarmRuns()).isEqualTo(2);
    }

    @Test
    public void shouldLoadConcurrentBenchmark()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("concurrent-benchmark");
        assertThat(benchmarks).hasSize(1);

        Benchmark benchmark = benchmarks.get(0);
        assertThat(benchmark.getDataSource()).isEqualTo("foo");
        assertThat(benchmark.getQueries()).extracting("name").containsExactly("q1", "q2", "1", "2");
        assertThat(benchmark.getRuns()).isEqualTo(10);
        assertThat(benchmark.getConcurrency()).isEqualTo(20);
    }

    @Test
    public void shouldLoadBenchmarkWithVariables()
            throws IOException
    {
        List<Benchmark> benchmarks = loadBenchmarkWithName("multi-variables-benchmark");
        assertThat(benchmarks).hasSize(5);

        assertThatBenchmarkWithEntries(benchmarks, entry("size", "1GB"), entry("format", "txt"))
                .containsOnly(entry("datasource", "foo"), entry("query-names", "q1, q2, 1, 2"), entry("size", "1GB"), entry("format", "txt"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "1GB"), entry("format", "orc"))
                .containsOnly(entry("datasource", "foo"), entry("query-names", "q1, q2, 1, 2"), entry("size", "1GB"), entry("format", "orc"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "2GB"), entry("format", "txt"))
                .containsOnly(entry("datasource", "foo"), entry("query-names", "q1, q2, 1, 2"), entry("size", "2GB"), entry("format", "txt"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "2GB"), entry("format", "orc"))
                .containsOnly(entry("datasource", "foo"), entry("query-names", "q1, q2, 1, 2"), entry("size", "2GB"), entry("format", "orc"));
        assertThatBenchmarkWithEntries(benchmarks, entry("size", "10GB"), entry("format", "parquet"))
                .containsOnly(entry("datasource", "foo"), entry("query-names", "q1, q2, 1, 2"), entry("size", "10GB"), entry("format", "parquet"));
    }

    private List<Benchmark> loadBenchmarkWithName(String benchmarkName)
    {
        ReflectionTestUtils.setField(benchmarkProperties, "benchmarksDir", "unit-benchmarks");
        ReflectionTestUtils.setField(benchmarkProperties, "activeBenchmarks", benchmarkName);

        return benchmarkLoader.loadBenchmarks("sequenceId");
    }

    private MapAssert<String, String> assertThatBenchmarkWithEntries(List<Benchmark> benchmarks, MapEntry<String, String>... entries)
    {
        Benchmark searchBenchmark = benchmarks.stream()
                .filter(benchmark -> {
                    boolean containsAllEntries = true;
                    for (MapEntry mapEntry : entries) {
                        Object value = benchmark.getVariables().get(mapEntry.key);
                        if (!mapEntry.value.equals(value)) {
                            containsAllEntries = false;
                            break;
                        }
                    }
                    return containsAllEntries;
                })
                .findFirst().get();

        return assertThat(searchBenchmark.getVariables());
    }
}
