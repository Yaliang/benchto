/*
 * Copyright 2013-2015, Teradata, Inc. All rights reserved.
 */
package com.teradata.benchto.driver.loader;

import com.facebook.presto.jdbc.internal.guava.collect.ImmutableList;
import com.teradata.benchto.driver.Query;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class AnnotatedQueryParserTest
{
    private final AnnotatedQueryParser queryParser = new AnnotatedQueryParser();

    @Test
    public void singleQuery()
    {
        List<String> fileContent = ImmutableList.of(
                "single sql",
                "query");
        Query parsingResult = queryParser.parseLines("whatever", fileContent);

        assertThat(parsingResult.getProperty("unknownProperty")).isEmpty();
        assertThat(parsingResult.getSqlTemplates()).containsExactly("single sql\nquery");
    }

    @Test
    public void multipleQueriesWithProperties()
    {
        List<String> fileContent = ImmutableList.of(
                "-- property1: value1;",
                "-- property2: value2",
                "sql query 1;",
                "sql query 2");
        Query parsingResult = queryParser.parseLines("whatever", fileContent);

        assertThat(parsingResult.getProperty("property1").get()).isEqualTo("value1");
        assertThat(parsingResult.getProperty("property2").get()).isEqualTo("value2");

        assertThat(parsingResult.getProperty("property3")).isEmpty();

        assertThat(parsingResult.getSqlTemplates()).containsExactly(
                "sql query 1",
                "sql query 2");
    }

    @Test(expected = IllegalStateException.class)
    public void shouldFailRedundantOptions()
    {
        List<String> fileContent = ImmutableList.of("-- property1: value", "-- property1: value2");
        queryParser.parseLines("whatever", fileContent);
    }
}
