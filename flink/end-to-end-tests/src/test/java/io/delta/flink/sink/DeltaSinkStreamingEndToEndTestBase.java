/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.delta.flink.sink;

import io.delta.flink.client.FlinkClient;
import io.delta.flink.client.FlinkClientFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

class DeltaSinkStreamingEndToEndTestBase extends DeltaSinkEndToEndTestBase {

    protected static FlinkClient flinkClient;
    protected static String jarId;

    @BeforeAll
    static void setUpClass() throws Exception {
        flinkClient = FlinkClientFactory.getCustomRestClient(
            getJobManagerHost(), getJobManagerPort());
        jarId = flinkClient.uploadJar(getTestArtifactPath());
    }

    @AfterAll
    static void cleanUpClass() throws Exception {
        if (flinkClient != null && jarId != null) {
            flinkClient.deleteJar(jarId);
        }
    }

    @Override
    protected FlinkClient getFlinkClient() {
        return flinkClient;
    }

}
