/*
 * Copyright 2022 EPAM Systems.
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

package com.epam.digital.data.platform.excerpt.worker.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.epam.digital.data.platform.excerpt.model.Request;
import com.epam.digital.data.platform.excerpt.worker.BaseIT;
import com.epam.digital.data.platform.excerpt.worker.TestUtils;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

class ExcerptListenerIT extends BaseIT {

  private static final String EXCERPT_TYPE = "test-report";
  private static final String FOLDER_PATH = "csv/" + EXCERPT_TYPE;

  @Value("${excerpt-templates-ceph.bucket}")
  String bucket;

  @Autowired
  ExcerptListener excerptListener;

  @MockBean(name = "excerptFileCephService")
  CephService excerptFileCephService;
  @MockBean(name = "excerptTemplatesCephService")
  CephService excerptTemplatesCephService;

  @Test
  void shouldCreateExcerpt() throws IOException {
    // given
    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    saveExcerptTemplateToDatabase(EXCERPT_TYPE, FOLDER_PATH);
    when(excerptTemplatesCephService.getAsString(bucket, FOLDER_PATH)).thenReturn(Optional.of(""));

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    var status = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get().getStatus();

    assertThat(status).isEqualTo(ExcerptProcessingStatus.COMPLETED);
  }

  @Test
  void shouldFailWhenTemplateNotFound() throws IOException {
    // given
    when(excerptTemplatesCephService.getAsString(bucket, FOLDER_PATH)).thenReturn(Optional.empty());

    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    var result = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get();

    assertThat(result.getStatus()).isEqualTo(ExcerptProcessingStatus.FAILED);
    assertThat(result.getStatusDetails()).isEqualTo("Excerpt template 'test-report' not found");
  }

  @Test
  void shouldSaveErrorDescriptionToDatabaseWhenGenerationFail() throws IOException {
    // given
    var requestJson = TestUtils.readClassPathResource("/json/request.json");
    var excerptEventDto = new ObjectMapper().readValue(requestJson, ExcerptEventDto.class);
    var excerptRecord = saveExcerptRecordToDatabase(excerptEventDto);

    excerptEventDto.setRecordId(excerptRecord.getId());
    excerptEventDto.getExcerptInputData().put("request2", List.of());

    saveExcerptTemplateToDatabase(EXCERPT_TYPE, FOLDER_PATH);
    when(excerptTemplatesCephService.getAsString(bucket, FOLDER_PATH)).thenReturn(Optional.of(""));

    // when
    excerptListener.generate(new Request<>(excerptEventDto));

    // then
    var record = excerptRecordRepository.findById(excerptEventDto.getRecordId()).get();

    assertThat(record.getStatus()).isEqualTo(ExcerptProcessingStatus.FAILED);
    assertThat(record.getStatusDetails()).isEqualTo("Input contains more than one object");
  }
}
