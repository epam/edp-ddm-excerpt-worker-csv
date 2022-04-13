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

package com.epam.digital.data.platform.excerpt.worker.repository;

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.dao.ExcerptRecord;
import com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus;
import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ExcerptRecordRepositoryFacade {

  private final Logger log = LoggerFactory.getLogger(ExcerptRecordRepositoryFacade.class);
  private final ExcerptRecordRepository recordRepository;

  public ExcerptRecordRepositoryFacade(ExcerptRecordRepository recordRepository) {
    this.recordRepository = recordRepository;
  }
  
  public void updateRecord(UUID recordId, String cephKey, String checksum, ExcerptProcessingStatus status) {
    log.info("Updating excerpt record. RecordId: {}. CephKey: {}. Checksum: {}",
        recordId, cephKey, checksum);
    var excerptRecord = getRecordById(recordId);
    excerptRecord.setStatus(status);
    excerptRecord.setExcerptKey(cephKey);
    excerptRecord.setChecksum(checksum);
    excerptRecord.setUpdatedAt(LocalDateTime.now());
    recordRepository.save(excerptRecord);
    log.info("Excerpt record updated");
  }

  public void updateRecord(UUID recordId, ExcerptProcessingException exception) {
    log.info("Updating excerpt record. RecordId: {}. Exception status: {}, details: {}",
        recordId, exception.getStatus(), exception.getDetails());
    var excerptRecord = getRecordById(recordId);
    excerptRecord.setStatus(exception.getStatus());
    excerptRecord.setStatusDetails(exception.getDetails());
    excerptRecord.setUpdatedAt(LocalDateTime.now());
    recordRepository.save(excerptRecord);
    log.info("Excerpt record updated");
  }
  
  private ExcerptRecord getRecordById(UUID id) {
    return recordRepository.findById(id).
        orElseThrow(() -> new ExcerptProcessingException(FAILED, "Record not found. Id: " + id));
  }
}
