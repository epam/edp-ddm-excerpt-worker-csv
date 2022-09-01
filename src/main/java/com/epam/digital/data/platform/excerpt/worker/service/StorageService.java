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

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptRecordRepositoryFacade;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import java.io.ByteArrayInputStream;
import java.util.Collections;
import java.util.UUID;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageService {

  static final String CEPH_OBJECT_CONTENT_TYPE = "application/octet-stream";

  private final Logger log = LoggerFactory.getLogger(StorageService.class);
  private final ExcerptRecordRepositoryFacade recordFacade;
  private final CephService excerptFileCephService;
  private final String excerptFileBucket;

  public StorageService(
      ExcerptRecordRepositoryFacade recordFacade,
      CephService excerptFileCephService,
      @Value("${file-excerpt-ceph.bucket}") String excerptFileBucket) {
    this.recordFacade = recordFacade;
    this.excerptFileCephService = excerptFileCephService;
    this.excerptFileBucket = excerptFileBucket;
  }

  public void storeFile(UUID recordId, byte[] bytes) {
    var cephKey = UUID.randomUUID().toString();
    saveFileToCeph(cephKey, bytes);
    String checksum = DigestUtils.sha256Hex(bytes);
    recordFacade.updateRecord(recordId, cephKey, checksum, COMPLETED);
  }

  private void saveFileToCeph(String cephKey, byte[] bytes) {
    log.info("Storing Excerpt to Ceph. Key: {}", cephKey);
    try {
      excerptFileCephService.put(
          excerptFileBucket, cephKey, CEPH_OBJECT_CONTENT_TYPE, Collections.emptyMap(),
          new ByteArrayInputStream(bytes));
    } catch (Exception e) {
      throw new ExcerptProcessingException(FAILED, "Failed saving file to ceph", e);
    }
  }
}
