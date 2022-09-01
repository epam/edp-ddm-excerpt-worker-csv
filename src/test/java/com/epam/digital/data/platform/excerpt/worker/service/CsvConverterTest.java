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
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.epam.digital.data.platform.excerpt.worker.exception.ExcerptProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CsvConverterTest {

  private final CsvConverter converter = new CsvConverter(new ObjectMapper(), new CsvMapper());

  @Test
  void happyPath() {
    var result = converter.convert(Map.of("str", List.of(Map.of("header", "data"))), "");
    result = removeBom(result);
    
    String[] lines = new String(result).split("\r\n");

    assertThat(lines).hasSize(2);
    assertThat(lines[0]).isEqualTo("header");
    assertThat(lines[1]).isEqualTo("data");
  }

  @Test
  void shouldSubstituteHeader() {
    var result = converter.convert(Map.of("str", 
        List.of(Map.of("header1", "data1", "header2", "data2"))), 
        "{\"header1\": \"first header\", \"header3\": \"third header\"}");
    result = removeBom(result);

    String[] lines = new String(result).split("\r\n");

    assertThat(lines).hasSize(2);
    assertThat(lines[0]).contains("first header").contains("header2");
    assertThat(lines[1]).contains("data1").contains("data2");
  }

  @Test
  void exceptionWhenMapIsEmpty() {

    var exception = assertThrows(ExcerptProcessingException.class,
        () -> converter.convert(Map.of(), ""));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
    assertThat(exception.getDetails()).isEqualTo("Input data does not contain any array");
  }

  @Test
  void exceptionWhenObjectIsNotArray() {

    var exception = assertThrows(ExcerptProcessingException.class,
        () -> converter.convert(Map.of("str", "str"), ""));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
    assertThat(exception.getDetails())
        .isEqualTo("The input contains an object that is not an array");
  }

  @Test
  void exceptionWhenInputDataHasMoreThenOneArray() {

    var exception = assertThrows(ExcerptProcessingException.class,
        () -> converter.convert(
            Map.of(
                "str", List.of(Map.of("header", "data")),
                "str2", List.of(Map.of("header", "data"))
            ), ""));

    assertThat(exception.getStatus()).isEqualTo(FAILED);
    assertThat(exception.getDetails()).isEqualTo("Input contains more than one object");
  }
  
  private byte[] removeBom(byte[] in) {
    byte[] out = new byte[in.length - 3];
    System.arraycopy(in, 3, out, 0, in.length - 3);
    return out;
  }
}
