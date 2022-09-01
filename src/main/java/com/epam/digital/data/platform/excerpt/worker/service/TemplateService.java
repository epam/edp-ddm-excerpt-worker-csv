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

import static com.epam.digital.data.platform.excerpt.model.ExcerptProcessingStatus.FAILED;

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.epam.digital.data.platform.excerpt.worker.repository.ExcerptTemplateRepository;
import com.epam.digital.data.platform.integration.ceph.service.CephService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TemplateService {

  private final ExcerptTemplateRepository templateRepository;
  private final CephService excerptTemplatesCephService;
  private final String templateBucket;

  public TemplateService(
      ExcerptTemplateRepository templateRepository,
      CephService excerptTemplatesCephService,
      @Value("${excerpt-templates-ceph.bucket}") String templateBucket) {
    this.templateRepository = templateRepository;
    this.excerptTemplatesCephService = excerptTemplatesCephService;
    this.templateBucket = templateBucket;
  }

  public String getTemplate(String excerptType) {
    String path = getTemplatePath(excerptType);
    return excerptTemplatesCephService.getAsString(templateBucket, path)
        .orElseThrow(() -> new ExcerptProcessingException(FAILED, "Template not found"));
  }

  private String getTemplatePath(String excerptType) {
    var excerptTemplate = templateRepository
        .findFirstByTemplateName(excerptType)
        .orElseThrow(() -> new ExcerptProcessingException(FAILED,
            "Excerpt template '" + excerptType + "' not found"));
    return excerptTemplate.getTemplate();
  }
}
