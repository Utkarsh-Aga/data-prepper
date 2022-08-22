/*
 * Copyright OpenSearch Contributors
 * SPDX-License-Identifier: Apache-2.0
 */

package org.opensearch.dataprepper.parser;

import com.amazon.dataprepper.model.plugin.PluginFactory;
import org.opensearch.dataprepper.TestDataProvider;
import org.opensearch.dataprepper.pipeline.Pipeline;
import org.opensearch.dataprepper.plugin.DefaultPluginFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PipelineParserTests {

    private PluginFactory pluginFactory;

    @BeforeEach
    void setUp() {
        final AnnotationConfigApplicationContext publicContext = new AnnotationConfigApplicationContext();
        publicContext.refresh();

        final AnnotationConfigApplicationContext coreContext = new AnnotationConfigApplicationContext();
        coreContext.setParent(publicContext);
        coreContext.scan(DefaultPluginFactory.class.getPackage().getName());
        coreContext.refresh();
        pluginFactory = coreContext.getBean(DefaultPluginFactory.class);
    }

    @Test
    void parseConfiguration_with_multiple_valid_pipelines_creates_the_correct_pipelineMap() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.VALID_MULTIPLE_PIPELINE_CONFIG_FILE, pluginFactory);
        final Map<String, Pipeline> actualPipelineMap = pipelineParser.parseConfiguration();
        assertThat(actualPipelineMap.keySet(), equalTo(TestDataProvider.VALID_MULTIPLE_PIPELINE_NAMES));
    }

    @Test
    void parseConfiguration_with_invalid_root_pipeline_creates_empty_pipelinesMap() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.CONNECTED_PIPELINE_ROOT_SOURCE_INCORRECT, pluginFactory);
        final Map<String, Pipeline> connectedPipelines = pipelineParser.parseConfiguration();
        assertThat(connectedPipelines.size(), equalTo(0));
    }

    @Test
    void parseConfiguration_with_incorrect_child_pipeline_returns_empty_pipelinesMap() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.CONNECTED_PIPELINE_CHILD_PIPELINE_INCORRECT, pluginFactory);
        final Map<String, Pipeline> connectedPipelines = pipelineParser.parseConfiguration();
        assertThat(connectedPipelines.size(), equalTo(0));
    }

    @Test
    void parseConfiguration_with_a_single_pipeline_with_empty_source_settings_returns_that_pipeline() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.VALID_SINGLE_PIPELINE_EMPTY_SOURCE_PLUGIN_FILE, pluginFactory);
        final Map<String, Pipeline> actualPipelineMap = pipelineParser.parseConfiguration();
        assertThat(actualPipelineMap.keySet().size(), equalTo(1));
    }

    @Test
    void parseConfiguration_with_cycles_in_multiple_pipelines_should_throw() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.CYCLE_MULTIPLE_PIPELINE_CONFIG_FILE, pluginFactory);

        final RuntimeException actualException = assertThrows(RuntimeException.class, pipelineParser::parseConfiguration);
        assertThat(actualException.getMessage(),
                equalTo("Provided configuration results in a loop, check pipeline: test-pipeline-1"));

    }

    @Test
    void parseConfiguration_with_incorrect_source_mapping_in_multiple_pipelines_should_throw() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.INCORRECT_SOURCE_MULTIPLE_PIPELINE_CONFIG_FILE, pluginFactory);

        final RuntimeException actualException = assertThrows(RuntimeException.class, pipelineParser::parseConfiguration);
        assertThat(actualException.getMessage(),
                equalTo("Invalid configuration, expected source test-pipeline-1 for pipeline test-pipeline-2 is missing"));
    }

    @Test
    void parseConfiguration_with_missing_pipeline_name_should_throw() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.MISSING_NAME_MULTIPLE_PIPELINE_CONFIG_FILE, pluginFactory);

        final RuntimeException actualException = assertThrows(RuntimeException.class, pipelineParser::parseConfiguration);
        assertThat(actualException.getMessage(),
                equalTo("name is a required attribute for sink pipeline plugin, " +
                    "check pipeline: test-pipeline-1"));
    }

    @Test
    void parseConfiguration_with_missing_pipeline_name_in_multiple_pipelines_should_throw() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.MISSING_PIPELINE_MULTIPLE_PIPELINE_CONFIG_FILE, pluginFactory);
        final RuntimeException actualException = assertThrows(RuntimeException.class, pipelineParser::parseConfiguration);
        assertThat(actualException.getMessage(), equalTo("Invalid configuration, no pipeline is defined with name test-pipeline-4"));
    }

    @Test
    void testMultipleSinks() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.VALID_MULTIPLE_SINKS_CONFIG_FILE, pluginFactory);
        final Map<String, Pipeline> pipelineMap = pipelineParser.parseConfiguration();
        assertThat(pipelineMap.keySet().size(), equalTo(3));
    }

    @Test
    void testMultipleProcessors() {
        final PipelineParser pipelineParser = new PipelineParser(TestDataProvider.VALID_MULTIPLE_PROCESSERS_CONFIG_FILE, pluginFactory);
        final Map<String, Pipeline> pipelineMap = pipelineParser.parseConfiguration();
        assertThat(pipelineMap.keySet().size(), equalTo(3));
    }

    @Test
    void parseConfiguration_with_a_configuration_file_which_does_not_exist_should_throw() {
        final PipelineParser pipelineParser = new PipelineParser("file_does_no_exist.yml", pluginFactory);
        final RuntimeException actualException = assertThrows(RuntimeException.class, pipelineParser::parseConfiguration);
        assertThat(actualException.getMessage(), equalTo("Failed to parse the configuration file file_does_no_exist.yml"));
    }
}