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

package com.epam.digital.data.platform.excerpt.worker.service;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.COMPLETED;
import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptRecordRepositoryFacade;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

  private StorageService service;

  private static final String BUCKET = "bucket";
  private static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";
  private static final UUID RECORD_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
  private static final byte[] FILE = new byte[]{1, 2, 3};

  @Mock
  ExcerptRecordRepositoryFacade recordFacade;
  @Mock
  CephService cephService;
  @Captor
  ArgumentCaptor<String> cephKey;

  @BeforeEach
  void init() {
    service = new StorageService(recordFacade, cephService, BUCKET);
  }

  @Test
  void storeFileHappyPath() {
    var checksum = DigestUtils.sha256Hex(FILE);

    service.storeFile(RECORD_ID, FILE);

    verify(recordFacade).updateRecord(eq(RECORD_ID), cephKey.capture(), eq(checksum), eq(COMPLETED));
    verify(cephService).put(eq(BUCKET), eq(cephKey.getValue()), eq(CEPH_OBJECT_CONTENT_TYPE),
        eq(Collections.emptyMap()), any());
  }

  @Test
  void shouldConvertAnyExceptionToExcerptProcessingException() {
    when(cephService.put(any(), any(), any(), any(), any())).thenThrow(RuntimeException.class);

    var exception = assertThrows(ExcerptProcessingException.class,
        () -> service.storeFile(RECORD_ID, FILE));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
    assertThat(exception.getDetails()).isEqualTo("Failed saving file to ceph");
  }
}
