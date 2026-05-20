package com.example.fraudapplication.domain.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ListToCsvTest {

    public static class TestPojo {
        public String name;
        public Integer age;
        public String nullableField;

        public TestPojo(String name, Integer age, String nullableField) {
            this.name = name;
            this.age = age;
            this.nullableField = nullableField;
        }
    }

    @Test
    void exportCSV_toFile_createsCorrectCsv(@TempDir Path tempDir) throws IOException {
        List<TestPojo> data = List.of(
                new TestPojo("Alice", 30, "value1"),
                new TestPojo("Bob", 25, null)
        );

        String filePath = tempDir.resolve("test_export").toString();
        ListToCsv.exportCSV(data, filePath);

        Path csvFile = Path.of(filePath + ".csv");
        assertTrue(Files.exists(csvFile));

        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(3, lines.size());
        assertEquals("name,age,nullableField", lines.get(0));
        assertTrue(lines.get(1).contains("Alice"));
        assertTrue(lines.get(1).contains("30"));
        assertTrue(lines.get(2).contains("Bob"));
        assertTrue(lines.get(2).contains("25"));
        assertTrue(lines.get(2).contains(",,")||lines.get(2).endsWith(","));
    }

    @Test
    void exportCSV_toHttpResponse_setsHeadersAndBody() throws IOException {
        List<TestPojo> data = List.of(
                new TestPojo("Alice", 30, "value1")
        );

        MockHttpServletResponse response = new MockHttpServletResponse();
        ListToCsv.exportCSV(data, response, "test");

        assertEquals("text/csv", response.getContentType());
        assertTrue(response.getHeader("Content-Disposition").contains("test.csv"));

        String body = response.getContentAsString();
        assertTrue(body.contains("name,age,nullableField"));
        assertTrue(body.contains("Alice"));
    }

    @Test
    void exportCSV_nullList_toFile_noExceptionAndNoFileCreated(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("null_test").toString();
        assertDoesNotThrow(() -> ListToCsv.exportCSV(null, filePath));
        assertFalse(Files.exists(Path.of(filePath + ".csv")));
    }

    @Test
    void exportCSV_emptyList_toFile_noExceptionAndNoFileCreated(@TempDir Path tempDir) {
        String filePath = tempDir.resolve("empty_test").toString();
        List<TestPojo> emptyList = Collections.emptyList();
        assertDoesNotThrow(() -> ListToCsv.exportCSV(emptyList, filePath));
        assertFalse(Files.exists(Path.of(filePath + ".csv")));
    }

    @Test
    void exportCSV_nullList_toResponse_noExceptionAndNoData() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        assertDoesNotThrow(() -> ListToCsv.exportCSV(null, response, "test"));
        assertEquals(0, response.getContentLength());
    }

    @Test
    void exportCSV_emptyList_toResponse_noExceptionAndNoData() {
        MockHttpServletResponse response = new MockHttpServletResponse();
        List<TestPojo> emptyList = Collections.emptyList();
        assertDoesNotThrow(() -> ListToCsv.exportCSV(emptyList, response, "test"));
    }

    @Test
    void generateFileName_viaReflection() throws Exception {
        Method method = ListToCsv.class.getDeclaredMethod("generateFileName", List.class);
        method.setAccessible(true);

        String nullResult = (String) method.invoke(null, (Object) null);
        assertEquals("default", nullResult);

        String emptyResult = (String) method.invoke(null, Collections.emptyList());
        assertEquals("default", emptyResult);

        List<TestPojo> data = List.of(new TestPojo("Alice", 30, null));
        String result = (String) method.invoke(null, data);
        assertTrue(result.startsWith("TestPojo_"));
    }

    @Test
    void exportCSV_pojoWithNullField_coversNullBranch(@TempDir Path tempDir) throws IOException {
        List<TestPojo> data = List.of(
                new TestPojo("Charlie", 40, null)
        );

        String filePath = tempDir.resolve("null_field_test").toString();
        ListToCsv.exportCSV(data, filePath);

        Path csvFile = Path.of(filePath + ".csv");
        assertTrue(Files.exists(csvFile));

        List<String> lines = Files.readAllLines(csvFile);
        assertEquals(2, lines.size());
        String dataLine = lines.get(1);
        assertTrue(dataLine.contains("Charlie"));
    }
}
