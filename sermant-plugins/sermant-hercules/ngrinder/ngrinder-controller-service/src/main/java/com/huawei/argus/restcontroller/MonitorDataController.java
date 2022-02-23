/*
 * Copyright (C) 2022-2022 Huawei Technologies Co., Ltd. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.huawei.argus.restcontroller;

import com.influxdb.client.InfluxDBClient;
import com.influxdb.client.InfluxDBClientFactory;
import com.influxdb.client.WriteApiBlocking;
import com.influxdb.client.domain.WritePrecision;
import com.influxdb.client.write.Point;
import com.influxdb.query.FluxRecord;
import com.influxdb.query.FluxTable;

import java.time.Instant;
import java.util.List;

/**
 * 功能描述：
 *
 * @author zl
 * @since 2022-02-15
 */
public class MonitorDataController {
    public static void main(final String[] args) {

        // You can generate an API token from the "API Tokens Tab" in the UI
        String token = "EsKTd2wneyLAAM_mpTZXEV6BSUSLAu-QEjzUEr9m6amfz95ZdBgvVNSCdLMAsNWpZTVrbgwq1NtfNzjt5FFV5g==";
        String bucket = "public";
        String org = "huawei";

        InfluxDBClient client = InfluxDBClientFactory.create("http://localhost:8086", token.toCharArray());
        for (int i = 0; i < 10; i++) {
            Point point = Point.measurement("server_monitor_cpu")
                .addTag("service", "service" + i)
                .addTag("service_instance", "service_instance" + i)
                .addField("idle_percentage", 100)
                .addField("io_wait_percentage", 200)
                .addField("sys_percentage", 300)
                .addField("user_percentage", 400)
                .time(Instant.now(), WritePrecision.NS);
            WriteApiBlocking writeApi = client.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
        }
        for (int i = 0; i < 10; i++) {
            Point point = Point.measurement("server_monitor_memory")
                .addTag("service", "service" + i)
                .addTag("service_instance", "service_instance" + i)
                .addField("memory_total", 100)
                .addField("swap_cached", 200)
                .addField("cached", 300)
                .addField("buffers", 400)
                .addField("memory_used", 400)
                .time(Instant.now(), WritePrecision.NS);
            WriteApiBlocking writeApi = client.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
        }
        for (int i = 0; i < 10; i++) {
            Point point = Point.measurement("server_monitor_network")
                .addTag("service", "service" + i)
                .addTag("service_instance", "service_instance" + i)
                .addField("read_bytes_per_second", 100)
                .addField("write_bytes_per_second", 200)
                .addField("read_packages_per_second", 300)
                .addField("write_packages_per_second", 400)
                .time(Instant.now(), WritePrecision.NS);
            WriteApiBlocking writeApi = client.getWriteApiBlocking();
            writeApi.writePoint(bucket, org, point);
        }
        String query = "from(bucket: \"public\") |> range(start: -10h)";
        // List<FluxTable> tables = client.getQueryApi().query(query, org);
        // for (FluxTable table : tables) {
        //    for (FluxRecord record : table.getRecords()) {
        //        System.out.println(record);
        //    }
        // }
        List<CpuInfluxEntity> tables = client.getQueryApi().query(query, org, CpuInfluxEntity.class);
        for (CpuInfluxEntity table : tables) {
            System.out.println(table);
        }
        client.close();
    }
}
