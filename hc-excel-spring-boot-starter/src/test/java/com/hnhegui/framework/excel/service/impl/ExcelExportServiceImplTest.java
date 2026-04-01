package com.hnhegui.framework.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.hnhegui.framework.excel.executor.ExcelAsyncExecutor;
import com.hnhegui.framework.excel.model.DynamicHead;
import com.hnhegui.framework.excel.model.ExcelExportRequest;
import com.hnhegui.framework.excel.model.ExcelTaskStatus;
import com.hnhegui.framework.excel.model.PivotTableHead;
import com.hnhegui.framework.excel.model.PivotTableHeads;
import com.hnhegui.framework.excel.model.TemplateExportRequest;
import lombok.Getter;
import lombok.Setter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * ExcelExportServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
class ExcelExportServiceImplTest {

    @Mock
    private ExcelAsyncExecutor asyncExecutor;

    private ExcelExportServiceImpl exportService;

    @BeforeEach
    void setUp() {
        exportService = new ExcelExportServiceImpl(asyncExecutor);
    }

    // ==================== 测试数据模型 ====================

    @Setter
    @Getter
    public static class TestData {
        private String name;
        private Integer age;

        public TestData() {}

        public TestData(String name, Integer age) {
            this.name = name;
            this.age = age;
        }

    }

    // ==================== 基础导出测试 ====================

    @Test
    void testExportData_SyncWithList() {
        // 准备数据
        ExcelExportRequest request = ExcelExportRequest.builder()
                .sheetName("测试Sheet")
                .headRowNumber(0)
                .build();

        List<TestData> data = Arrays.asList(
            new TestData("张三", 25),
            new TestData("李四", 30)
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行导出
        exportService.exportData(request, data, TestData.class, outputStream);

        // 验证结果
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportData_SyncWithSupplier() {
        // 准备数据
        ExcelExportRequest request = ExcelExportRequest.builder()
                .sheetName("测试Sheet")
                .batchSize(2)
                .build();

        // 每批2条数据，batch1和batch2都返回2条，这样循环会继续
        List<TestData> batch1 = Arrays.asList(new TestData("张三", 25), new TestData("李四", 30));
        List<TestData> batch2 = Arrays.asList(new TestData("王五", 35), new TestData("赵六", 40));
        List<TestData> batch3 = Arrays.asList(new TestData("孙七", 45));
        List<TestData> empty = new ArrayList<>();

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<List<TestData>> dataQuery = () -> {
            int count = callCount.getAndIncrement();
            if (count == 0) return batch1;
            if (count == 1) return batch2;
            if (count == 2) return batch3;
            return empty;
        };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行导出
        exportService.exportData(request, dataQuery, TestData.class, outputStream);

        // 验证结果 - batch1(2条>=2), batch2(2条>=2), batch3(1条<2停止), 第4次返回empty
        assertTrue(outputStream.size() > 0);
        assertEquals(3, callCount.get());
    }

    // ==================== 异步导出测试 ====================

    @Test
    void testExportDataAsync() {
        // 准备数据
        ExcelExportRequest request = ExcelExportRequest.builder().build();
        String expectedTaskId = "test-task-id-123";

        when(asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT)).thenReturn(expectedTaskId);
        doNothing().when(asyncExecutor).executeExport(
            eq(expectedTaskId), eq(request), any(Supplier.class), 
            eq(TestData.class), any()
        );

        Supplier<List<TestData>> dataQuery = () -> List.of(new TestData("张三", 25));

        // 执行
        String taskId = exportService.exportDataAsync(request, dataQuery, TestData.class, null);

        // 验证
        assertEquals(expectedTaskId, taskId);
        verify(asyncExecutor).createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        verify(asyncExecutor).executeExport(eq(expectedTaskId), eq(request), any(Supplier.class), 
            eq(TestData.class), isNull());
    }

    @Test
    void testExportDataAsyncAll() {
        // 准备数据
        ExcelExportRequest request = ExcelExportRequest.builder().build();
        String expectedTaskId = "test-task-id-456";

        when(asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT)).thenReturn(expectedTaskId);
        doNothing().when(asyncExecutor).executeExportAll(
            eq(expectedTaskId), eq(request), any(Supplier.class), 
            eq(TestData.class), any()
        );

        Supplier<List<TestData>> allDataQuery = () -> List.of(new TestData("张三", 25));

        // 执行
        String taskId = exportService.exportDataAsyncAll(request, allDataQuery, TestData.class, null);

        // 验证
        assertEquals(expectedTaskId, taskId);
        verify(asyncExecutor).createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        verify(asyncExecutor).executeExportAll(eq(expectedTaskId), eq(request), any(Supplier.class), 
            eq(TestData.class), isNull());
    }

    // ==================== 模板导出测试 ====================

    @Test
    void testExportByTemplate_SingleData() throws Exception {
        // 创建临时模板文件
        File tempTemplate = createTempTemplateFile();

        TemplateExportRequest request = TemplateExportRequest.builder().build();
        request.setTemplatePath(tempTemplate.getAbsolutePath());
        request.setSheetName("Sheet1");

        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试标题");
        request.setData(data);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行
        exportService.exportByTemplate(request, outputStream);

        // 验证
        assertTrue(outputStream.size() > 0);

        // 清理
        tempTemplate.delete();
    }

    @Test
    void testExportByTemplate_WithListData() throws Exception {
        // 创建临时模板文件
        File tempTemplate = createTempTemplateFile();

        TemplateExportRequest request = TemplateExportRequest.builder().build();
        request.setTemplatePath(tempTemplate.getAbsolutePath());
        request.setSheetName("Sheet1");

        Map<String, Object> data = new HashMap<>();
        data.put("title", "测试标题");
        request.setData(data);

        List<TestData> listData = Arrays.asList(
            new TestData("张三", 25),
            new TestData("李四", 30)
        );

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行
        exportService.exportByTemplate(request, listData, outputStream);

        // 验证
        assertTrue(outputStream.size() > 0);

        // 清理
        tempTemplate.delete();
    }

    @Test
    void testExportByTemplateAsync() {
        TemplateExportRequest request = TemplateExportRequest.builder().build();
        String expectedTaskId = "test-task-id-789";

        when(asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT)).thenReturn(expectedTaskId);
        doNothing().when(asyncExecutor).executeTemplateExport(
            eq(expectedTaskId), eq(request), any(Supplier.class), any()
        );

        Supplier<List<TestData>> dataQuery = () -> List.of(new TestData("张三", 25));

        // 执行
        String taskId = exportService.exportByTemplateAsync(request, dataQuery, null);

        // 验证
        assertEquals(expectedTaskId, taskId);
        verify(asyncExecutor).createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        verify(asyncExecutor).executeTemplateExport(eq(expectedTaskId), eq(request), any(Supplier.class), isNull());
    }

    // ==================== 动态表头导出测试 ====================

    @Test
    void testExportWithDynamicHead_WithList() {
        List<DynamicHead> heads = Arrays.asList(
            DynamicHead.builder().name("姓名").field("name").build(),
            DynamicHead.builder().name("年龄").field("age").build()
        );

        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "张三");
        row1.put("age", 25);
        dataList.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("name", "李四");
        row2.put("age", 30);
        dataList.add(row2);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行
        exportService.exportWithDynamicHead(heads, dataList, outputStream);

        // 验证
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportWithDynamicHead_WithSupplier() {
        List<DynamicHead> heads = Arrays.asList(
            DynamicHead.builder().name("姓名").field("name").build(),
            DynamicHead.builder().name("年龄").field("age").build()
        );

        List<Map<String, Object>> batch1 = new ArrayList<>();
        Map<String, Object> row1 = new HashMap<>();
        row1.put("name", "张三");
        row1.put("age", 25);
        batch1.add(row1);

        AtomicInteger callCount = new AtomicInteger(0);
        Supplier<List<Map<String, Object>>> dataQuery = () -> {
            if (callCount.getAndIncrement() == 0) {
                return batch1;
            }
            return new ArrayList<>();
        };

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行
        exportService.exportWithDynamicHead(heads, dataQuery, outputStream);

        // 验证
        assertTrue(outputStream.size() > 0);
        assertEquals(2, callCount.get());
    }

    @Test
    void testExportWithDynamicHeadAsync() {
        List<DynamicHead> heads = Arrays.asList(
            DynamicHead.builder().name("姓名").field("name").build(),
            DynamicHead.builder().name("年龄").field("age").build()
        );

        String expectedTaskId = "test-task-id-dynamic";

        when(asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT)).thenReturn(expectedTaskId);
        doNothing().when(asyncExecutor).executeDynamicHeadExport(
            eq(expectedTaskId), eq("Sheet1"), eq(heads), any(Supplier.class), any()
        );

        Supplier<List<Map<String, Object>>> dataQuery = ArrayList::new;

        // 执行
        String taskId = exportService.exportWithDynamicHeadAsync(
            "test.xlsx", "Sheet1", heads, dataQuery, null);

        // 验证
        assertEquals(expectedTaskId, taskId);
        verify(asyncExecutor).createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        verify(asyncExecutor).executeDynamicHeadExport(
            eq(expectedTaskId), eq("Sheet1"), eq(heads), any(Supplier.class), isNull());
    }

    // ==================== 任务管理测试 ====================

    @Test
    void testGetTaskStatus() {
        String taskId = "test-task-id";
        ExcelTaskStatus expectedStatus = new ExcelTaskStatus(taskId, ExcelTaskStatus.TaskType.EXPORT);

        when(asyncExecutor.getTaskStatus(taskId)).thenReturn(expectedStatus);

        // 执行
        ExcelTaskStatus result = exportService.getTaskStatus(taskId);

        // 验证
        assertEquals(expectedStatus, result);
        verify(asyncExecutor).getTaskStatus(taskId);
    }

    @Test
    void testGetExportFilePath() {
        String taskId = "test-task-id";
        String expectedPath = "/tmp/test.xlsx";

        when(asyncExecutor.getExportFilePath(taskId)).thenReturn(expectedPath);

        // 执行
        String result = exportService.getExportFilePath(taskId);

        // 验证
        assertEquals(expectedPath, result);
        verify(asyncExecutor).getExportFilePath(taskId);
    }

    // ==================== 列转行表头导出测试 ====================

    @Test
    void testExportWithPivotTable_Full() {
        // 构建列转行表头：固定列+动态列+汇总列
        PivotTableHeads pivotHeads = PivotTableHeads.of(
            Arrays.asList("市场", "区域"),
            Arrays.asList("market", "region"),
            Arrays.asList("产品A", "产品B", "产品C"),
            Arrays.asList("product_a", "product_b", "product_c"),
            "合计",
            "total"
        );

        // 验证表头结构
        assertEquals(2, pivotHeads.getFixedColumnCount());
        assertEquals(3, pivotHeads.getDynamicColumnCount());
        assertEquals(6, pivotHeads.getTotalColumnCount());
        assertNotNull(pivotHeads.getSummaryColumn());

        // 验证完整表头列表顺序：固定列+动态列+汇总列
        List<DynamicHead> allHeads = pivotHeads.toDynamicHeads();
        assertEquals(6, allHeads.size());
        assertEquals("市场", allHeads.get(0).getName());
        assertEquals("区域", allHeads.get(1).getName());
        assertEquals("产品A", allHeads.get(2).getName());
        assertEquals("产品B", allHeads.get(3).getName());
        assertEquals("产品C", allHeads.get(4).getName());
        assertEquals("合计", allHeads.get(5).getName());

        // 构建测试数据
        List<Map<String, Object>> dataList = buildPivotTableData();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行导出
        exportService.exportWithDynamicHead(allHeads, dataList, outputStream);

        // 验证导出结果
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportWithPivotTable_NoSummary() {
        // 仅固定列+动态列，无汇总列
        PivotTableHeads pivotHeads = PivotTableHeads.of(
            Arrays.asList("市场"),
            Arrays.asList("market"),
            Arrays.asList("产品A", "产品B"),
            Arrays.asList("product_a", "product_b")
        );

        // 验证无汇总列
        assertEquals(1, pivotHeads.getFixedColumnCount());
        assertEquals(2, pivotHeads.getDynamicColumnCount());
        assertEquals(3, pivotHeads.getTotalColumnCount());
        assertNull(pivotHeads.getSummaryColumn());

        List<DynamicHead> allHeads = pivotHeads.toDynamicHeads();
        assertEquals(3, allHeads.size());

        // 构建数据
        List<Map<String, Object>> dataList = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("market", "北京");
        row.put("product_a", 100);
        row.put("product_b", 200);
        dataList.add(row);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        exportService.exportWithDynamicHead(allHeads, dataList, outputStream);
        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportWithPivotTable_BuilderStyle() {
        // 使用Builder方式构建表头
        PivotTableHeads pivotHeads = PivotTableHeads.builder()
            .fixedColumns(Arrays.asList(
                PivotTableHead.fixed("市场", "market"),
                PivotTableHead.fixed("区域", "region")
            ))
            .dynamicColumns(Arrays.asList(
                PivotTableHead.dynamic("产品A", "product_a"),
                PivotTableHead.dynamic("产品B", "product_b")
            ))
            .summaryColumn(PivotTableHead.summary("合计", "total"))
            .build();

        // 验证字段列表顺序
        List<String> allFields = pivotHeads.getAllFields();
        assertEquals(Arrays.asList("market", "region", "product_a", "product_b", "total"), allFields);

        List<String> fixedFields = pivotHeads.getFixedFields();
        assertEquals(Arrays.asList("market", "region"), fixedFields);

        List<String> dynamicFields = pivotHeads.getDynamicFields();
        assertEquals(Arrays.asList("product_a", "product_b"), dynamicFields);
    }

    @Test
    void testExportWithPivotTable_EmptyDynamicColumns() {
        // 动态列为空（数据库无数据时的边界场景）
        PivotTableHeads pivotHeads = PivotTableHeads.of(
            Arrays.asList("市场"),
            Arrays.asList("market"),
            new ArrayList<>(),
            new ArrayList<>(),
            "合计",
            "total"
        );

        assertEquals(1, pivotHeads.getFixedColumnCount());
        assertEquals(0, pivotHeads.getDynamicColumnCount());
        assertEquals(2, pivotHeads.getTotalColumnCount());

        List<DynamicHead> allHeads = pivotHeads.toDynamicHeads();
        assertEquals(2, allHeads.size());
        assertEquals("市场", allHeads.get(0).getName());
        assertEquals("合计", allHeads.get(1).getName());
    }

    @Test
    void testExportWithPivotTable_AsyncExport() {
        // 异步导出列转行表头
        PivotTableHeads pivotHeads = PivotTableHeads.of(
            Arrays.asList("市场", "区域"),
            Arrays.asList("market", "region"),
            Arrays.asList("产品A", "产品B"),
            Arrays.asList("product_a", "product_b"),
            "合计",
            "total"
        );

        List<DynamicHead> allHeads = pivotHeads.toDynamicHeads();
        String expectedTaskId = "pivot-task-id-001";

        when(asyncExecutor.createTaskId(ExcelTaskStatus.TaskType.EXPORT)).thenReturn(expectedTaskId);
        doNothing().when(asyncExecutor).executeDynamicHeadExport(
            eq(expectedTaskId), eq("Sheet1"), eq(allHeads), any(Supplier.class), any()
        );

        Supplier<List<Map<String, Object>>> dataQuery = this::buildPivotTableData;

        // 执行异步导出
        String taskId = exportService.exportWithDynamicHeadAsync(
            "pivot_report", "Sheet1", allHeads, dataQuery, null);

        // 验证
        assertEquals(expectedTaskId, taskId);
        verify(asyncExecutor).createTaskId(ExcelTaskStatus.TaskType.EXPORT);
        verify(asyncExecutor).executeDynamicHeadExport(
            eq(expectedTaskId), eq("Sheet1"), eq(allHeads), any(Supplier.class), isNull());
    }

    // ==================== 边界条件测试 ====================

    @Test
    void testExportData_EmptyList() {
        ExcelExportRequest request = ExcelExportRequest.builder().build();
        List<TestData> emptyData = new ArrayList<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行 - 空列表不应抛出异常
        assertDoesNotThrow(() -> {
            exportService.exportData(request, emptyData, TestData.class, outputStream);
        });

        assertTrue(outputStream.size() > 0);
    }

    @Test
    void testExportWithDynamicHead_EmptyData() {
        List<DynamicHead> heads = Arrays.asList(
            DynamicHead.builder().name("姓名").field("name").build()
        );
        List<Map<String, Object>> emptyData = new ArrayList<>();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 执行 - 空数据不应抛出异常
        assertDoesNotThrow(() -> {
            exportService.exportWithDynamicHead(heads, emptyData, outputStream);
        });

        assertTrue(outputStream.size() > 0);
    }

    // ==================== 辅助方法 ====================

    private List<Map<String, Object>> buildPivotTableData() {
        List<Map<String, Object>> dataList = new ArrayList<>();

        Map<String, Object> row1 = new HashMap<>();
        row1.put("market", "北京");
        row1.put("region", "朝阳");
        row1.put("product_a", 100);
        row1.put("product_b", 200);
        row1.put("product_c", 150);
        row1.put("total", 450);
        dataList.add(row1);

        Map<String, Object> row2 = new HashMap<>();
        row2.put("market", "北京");
        row2.put("region", "海淀");
        row2.put("product_a", 80);
        row2.put("product_b", 120);
        row2.put("product_c", 90);
        row2.put("total", 290);
        dataList.add(row2);

        Map<String, Object> row3 = new HashMap<>();
        row3.put("market", "上海");
        row3.put("region", "浦东");
        row3.put("product_a", 300);
        row3.put("product_b", 250);
        row3.put("product_c", 180);
        row3.put("total", 730);
        dataList.add(row3);

        return dataList;
    }

    private File createTempTemplateFile() throws Exception {
        File tempFile = File.createTempFile("template", ".xlsx");
        
        // 创建一个简单的Excel文件作为模板
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            EasyExcel.write(baos)
                .sheet("Sheet1")
                .doWrite(new ArrayList<>());
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile)) {
                fos.write(baos.toByteArray());
            }
        }
        
        return tempFile;
    }
}
