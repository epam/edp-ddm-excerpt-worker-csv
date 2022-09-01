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

import com.epam.digital.data.platform.excerpt.model.ExcerptEventDto;
import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptRecordRepositoryFacade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ExcerptService {

  private final Logger log = LoggerFactory.getLogger(ExcerptService.class);
  private final ExcerptRecordRepositoryFacade recordFacade;
  private final TemplateService templateService;
  private final Converter converter;
  private final StorageService storageService;

  public ExcerptService(
      ExcerptRecordRepositoryFacade recordFacade,
      TemplateService templateService,
      Converter converter,
      StorageService storageService) {
    this.recordFacade = recordFacade;
    this.templateService = templateService;
    this.converter = converter;
    this.storageService = storageService;
  }

  public void generateExcerpt(ExcerptEventDto event) {
    try {
      log.info("Converting data to csv format");
      var template = templateService.getTemplate(event.getExcerptType());
      var file = converter.convert(event.getExcerptInputData(), template);
      storageService.storeFile(event.getRecordId(), file);
      log.info("Excerpt generated");
    } catch (ExcerptProcessingException e) {
      log.error("Can not generate excerpt. Status: {}. Details: {}", e.getStatus(), e.getDetails());
      recordFacade.updateRecord(event.getRecordId(), e);
    }
  }
}
