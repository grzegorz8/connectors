package io.delta.flink.e2e.source;

import java.time.Duration;

import io.delta.flink.e2e.client.parameters.JobParameters;
import io.delta.flink.e2e.client.parameters.JobParametersBuilder;
import io.delta.flink.e2e.data.UserDeltaTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static io.delta.flink.e2e.assertions.ParquetAssertions.assertThat;
import static io.delta.flink.e2e.data.UserBuilder.anUser;

@DisplayNameGeneration(DisplayNameGenerator.IndicativeSentences.class)
public class DeltaSourceStreamingEndToEndTest extends DeltaSourceEndToEndTestBase {

    private static final int PARALLELISM = 3;
    private static final String JOB_MAIN_CLASS =
        "io.delta.flink.e2e.source.DeltaSourceStreamingJob";


    @DisplayName("Connector in streaming mode should read records from the latest snapshot")
    @ParameterizedTest(name = "partitioned table: {0}; failover: {1}")
    @CsvSource(value = {"false,false", "true,false"})
    void shouldReadLatestSnapshot(boolean isPartitioned, boolean triggerFailover) throws Exception {
        // GIVEN: Delta Lake table
        UserDeltaTable userTable = isPartitioned
            ? UserDeltaTable.partitionedByCountryAndBirthYear(deltaTableLocation)
            : UserDeltaTable.nonPartitioned(deltaTableLocation);
        userTable.initializeTable();
        // AND: version 1
        userTable.add(
            anUser().name("Philip").surname("Dick").country("US").birthYear(1928).build(),
            anUser().name("Stanisław").surname("Lem").country("PL").birthYear(1921).build()
        );
        // AND: version 2
        userTable.add(
            anUser().name("Arthur").surname("Clarke").country("US").birthYear(1917).build(),
            anUser().name("Isaac").surname("Asimov").country("US").birthYear(1920).build()
        );
        // AND
        JobParameters jobParameters = batchJobParameters()
            .withDeltaTablePath(deltaTableLocation)
            .withTablePartitioned(isPartitioned)
            .withTriggerFailover(triggerFailover)
            .build();

        // WHEN
        jobID = flinkClient.run(jobParameters);
        fileCounter.waitForNewFiles(Duration.ofMinutes(3));

        // THEN
        assertThat(outputLocation)
            .hasRecordCount(4);
    }

    @DisplayName("Connector in streaming mode should read records from the latest snapshot and " +
        "all subsequent changes")
    @ParameterizedTest(name = "partitioned table: {0}; failover: {1}")
    @CsvSource(value = {"false,false", "true,false"})
    void shouldReadLatestSnapshotAndSubsequentChanges(boolean isPartitioned,
                                                      boolean triggerFailover) throws Exception {
        // GIVEN: Delta Lake table
        UserDeltaTable userTable = isPartitioned
            ? UserDeltaTable.partitionedByCountryAndBirthYear(deltaTableLocation)
            : UserDeltaTable.nonPartitioned(deltaTableLocation);
        userTable.initializeTable();
        // AND: version 1
        userTable.add(
            anUser().name("Philip").surname("Dick").country("US").birthYear(1928).build(),
            anUser().name("Stanisław").surname("Lem").country("PL").birthYear(1921).build()
        );
        // AND: version 2
        userTable.add(
            anUser().name("Arthur").surname("Clarke").country("US").birthYear(1917).build(),
            anUser().name("Isaac").surname("Asimov").country("US").birthYear(1920).build()
        );
        // AND
        JobParameters jobParameters = batchJobParameters()
            .withDeltaTablePath(deltaTableLocation)
            .withTablePartitioned(isPartitioned)
            .withTriggerFailover(triggerFailover)
            .build();

        // AND
        jobID = flinkClient.run(jobParameters);
        fileCounter.waitForNewFiles(Duration.ofMinutes(3));
        assertThat(outputLocation).hasRecordCount(4);

        // WHEN: new records are committed to the Delta Lake table
        userTable.add(
            anUser().name("Johannes").surname("Kepler").country("DE").birthYear(1571).build(),
            anUser().name("Herbert George").surname("Wells").country("GB").birthYear(1866).build()
        );
        fileCounter.waitForNewFiles(Duration.ofMinutes(3));

        // THEN
        assertThat(outputLocation)
            .hasRecordCount(6);
    }

    private JobParametersBuilder batchJobParameters() {
        return JobParametersBuilder.builder()
            .withName(getTestDisplayName())
            .withJarId(jarId)
            .withEntryPointClassName(JOB_MAIN_CLASS)
            .withParallelism(PARALLELISM)
            .withOutputPath(outputLocation);
    }

}
