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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.dataformat.csv.CsvSchema.Builder;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CsvConverter implements Converter {

  private static final String LINE_SEPARATOR = "\r\n";

  private final Logger log = LoggerFactory.getLogger(CsvConverter.class);
  private final ObjectMapper objectMapper;
  private final CsvMapper csvMapper;

  public CsvConverter(ObjectMapper objectMapper, CsvMapper csvMapper) {
    this.objectMapper = objectMapper;
    this.csvMapper = csvMapper;
  }

  @Override
  public byte[] convert(Map<String, Object> object, String template) {
    try {
      return convert(objectMapper.writeValueAsString(object), template);
    } catch (JsonProcessingException e) {
      throw new ExcerptProcessingException(FAILED, "Cannot convert data to Json format");
    }
  }

  private byte[] convert(String jsonData, String template) {
    try {
      var jsonTree = objectMapper.readTree(jsonData);
      validateInputData(jsonTree);

      JsonNode array = jsonTree.elements().next();
      JsonNode firstObject = array.elements().next();

      Builder csvSchemaBuilder = CsvSchema.builder()
          .setLineSeparator(LINE_SEPARATOR)
          .setUseHeader(true);

      firstObject.fieldNames().forEachRemaining(csvSchemaBuilder::addColumn);
      CsvSchema csvSchema = csvSchemaBuilder.build();

      var csvDocument = csvMapper.writerFor(JsonNode.class)
          .with(csvSchema)
          .writeValueAsString(array);

      var headers = getHeadersFromTemplate(template);
      csvDocument = replaceHeaders(csvDocument, headers);

      var result = getBaosWithUtf8Bom();
      result.writeBytes(csvDocument.getBytes(StandardCharsets.UTF_8));
      return result.toByteArray();

    } catch (JsonProcessingException e) {
      throw new ExcerptProcessingException(FAILED, "Cannot parse string to Json nodes", e);
    } catch (ExcerptProcessingException e) {
      throw e;
    } catch (Exception e) {
      throw new ExcerptProcessingException(FAILED,
          "Unexpected error while converting Json to csv", e);
    }
  }

  private ByteArrayOutputStream getBaosWithUtf8Bom() {
    var os = new ByteArrayOutputStream();
    os.write(0xef);
    os.write(0xbb);
    os.write(0xbf);
    return os;
  }

  private void validateInputData(JsonNode jsonTree) {
    Iterator<JsonNode> elementsIterator = jsonTree.elements();
    if (!elementsIterator.hasNext()) {
      throw new ExcerptProcessingException(FAILED, "Input data does not contain any array");
    }
    if (!elementsIterator.next().isArray()) {
      throw new ExcerptProcessingException(FAILED,
          "The input contains an object that is not an array");
    }
    if (elementsIterator.hasNext()) {
      throw new ExcerptProcessingException(FAILED, "Input contains more than one object");
    }
  }

  private Map<String, String> getHeadersFromTemplate(String template) {
    try {
      return objectMapper.readValue(template, new TypeReference<HashMap<String, String>>() {
      });
    } catch (Exception e) {
      log.error("Cannot convert Json to Map<String, String>", e);
    }
    return Map.of();
  }

  private String replaceHeaders(String csv, Map<String, String> headers) {
    var substring = csv.split(LINE_SEPARATOR, 2);
    for (var header : headers.entrySet()) {
      substring[0] = substring[0].replaceAll(header.getKey(), header.getValue());
    }
    return substring[0] + LINE_SEPARATOR + substring[1];
  }
}
