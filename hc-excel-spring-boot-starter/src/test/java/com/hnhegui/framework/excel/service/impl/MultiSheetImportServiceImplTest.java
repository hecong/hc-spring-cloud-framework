package com.hnhegui.framework.excel.service.impl;

import com.alibaba.excel.EasyExcel;
import com.hnhegui.framework.excel.executor.ExcelAsyncExecutor;
import com.hnhegui.framework.excel.model.multisheet.MultiSheetImportResult;
import com.hnhegui.framework.excel.model.multisheet.SheetError;
import com.hnhegui.framework.excel.model.multisheet.ValidationResult;
import com.hnhegui.framework.excel.service.ExcelImportService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 多Sheet导入服务测试
 */
@ExtendWith(MockitoExtension.class)
class MultiSheetImportServiceImplTest {

    @Mock
    private ExcelAsyncExecutor asyncExecutor;

    private ExcelImportService excelImportService;

    @BeforeEach
    void setUp() {
        excelImportService = new ExcelImportServiceImpl(asyncExecutor);
    }

    /**
     * 测试基础多Sheet导入
     */
    @Test
    void testMultiSheetImport_Basic() {
        // 创建测试数据
        List<UserBasicDTO> sheet1Data = new ArrayList<>();
        sheet1Data.add(createUserBasic("U001", "张三", "13800138001", "zhangsan@test.com"));
        sheet1Data.add(createUserBasic("U002", "李四", "13800138002", "lisi@test.com"));

        List<UserAddressDTO> sheet2Data = new ArrayList<>();
        sheet2Data.add(createUserAddress("U001", "北京", "北京市", "朝阳区", "朝阳公园1号"));
        sheet2Data.add(createUserAddress("U002", "上海", "上海市", "浦东新区", "世纪公园1号"));

        // 创建Excel文件
        InputStream inputStream = createMultiSheetExcel(sheet1Data, sheet2Data);

        // 执行导入
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchSize(10)
                .sheet(1, "用户地址", UserAddressDTO.class)
                .batchSize(10)
                .doRead();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isAllSuccess());
        assertEquals(4, result.getTotalRows()); // 2 + 2
        assertEquals(4, result.getTotalSuccessRows());
        assertEquals(0, result.getTotalFailRows());

        // 验证Sheet1数据
        List<UserBasicDTO> importedUsers = result.getSheetDataList("用户基础");
        assertEquals(2, importedUsers.size());
        assertEquals("张三", importedUsers.get(0).getName());
        assertEquals("李四", importedUsers.get(1).getName());

        // 验证Sheet2数据
        List<UserAddressDTO> importedAddresses = result.getSheetDataList("用户地址");
        assertEquals(2, importedAddresses.size());
        assertEquals("北京", importedAddresses.get(0).getProvince());
        assertEquals("上海", importedAddresses.get(1).getProvince());
    }

    /**
     * 测试带验证的导入
     */
    @Test
    void testMultiSheetImport_WithValidation() {
        // 创建测试数据（第二条数据姓名为空，会验证失败）
        List<UserBasicDTO> sheet1Data = new ArrayList<>();
        sheet1Data.add(createUserBasic("U001", "张三", "13800138001", "zhangsan@test.com"));
        sheet1Data.add(createUserBasic("U002", "", "13800138002", "lisi@test.com")); // 姓名为空

        InputStream inputStream = createSingleSheetExcel(sheet1Data, UserBasicDTO.class, "用户基础");

        // 执行导入（带验证）
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .validator(data -> {
                    if (data.getName() == null || data.getName().trim().isEmpty()) {
                        return ValidationResult.fail("姓名不能为空", data);
                    }
                    return ValidationResult.ok(data);
                })
                .batchSize(10)
                .doRead();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isAllSuccess());
        assertEquals(2, result.getTotalRows());
        assertEquals(1, result.getTotalSuccessRows());
        assertEquals(1, result.getTotalFailRows());

        // 验证错误信息
        List<SheetError> errors = result.getSheetErrors("用户基础");
        assertEquals(1, errors.size());
        assertEquals("姓名不能为空", errors.get(0).getErrorMsg());
    }

    /**
     * 测试带上下文的验证
     */
    @Test
    void testMultiSheetImport_WithContextValidation() {
        // Sheet1: 用户数据
        List<UserBasicDTO> sheet1Data = new ArrayList<>();
        sheet1Data.add(createUserBasic("U001", "张三", "13800138001", "zhangsan@test.com"));

        // Sheet2: 地址数据（包含一个不存在的用户ID）
        List<UserAddressDTO> sheet2Data = new ArrayList<>();
        sheet2Data.add(createUserAddress("U001", "北京", "北京市", "朝阳区", "朝阳公园1号"));
        sheet2Data.add(createUserAddress("U999", "广东", "广州市", "天河区", "天河路1号")); // 用户不存在

        InputStream inputStream = createMultiSheetExcel(sheet1Data, sheet2Data);

        // 执行导入（Sheet2验证时检查Sheet1的数据）
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchSize(10)
                .sheet(1, "用户地址", UserAddressDTO.class)
                .validator((data, ctx) -> {
                    // 从上下文获取用户数据
                    List<UserBasicDTO> users = ctx.getSheetData("用户基础");
                    boolean exists = users.stream()
                            .anyMatch(u -> u.getUserId().equals(data.getUserId()));
                    if (!exists) {
                        return ValidationResult.fail("用户ID " + data.getUserId() + " 不存在", data);
                    }
                    return ValidationResult.ok(data);
                })
                .batchSize(10)
                .doRead();

        // 验证结果
        assertNotNull(result);
        assertFalse(result.isAllSuccess());
        assertEquals(3, result.getTotalRows()); // 1 + 2
        assertEquals(2, result.getTotalSuccessRows()); // 1 + 1
        assertEquals(1, result.getTotalFailRows()); // 0 + 1

        // 验证Sheet2的错误
        List<SheetError> addressErrors = result.getSheetErrors("用户地址");
        assertEquals(1, addressErrors.size());
        assertTrue(addressErrors.get(0).getErrorMsg().contains("U999"));
    }

    /**
     * 测试批次处理
     */
    @Test
    void testMultiSheetImport_BatchHandler() {
        // 创建大量测试数据
        List<UserBasicDTO> sheet1Data = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            sheet1Data.add(createUserBasic("U" + String.format("%03d", i), "用户" + i, "13800138" + String.format("%03d", i), "user" + i + "@test.com"));
        }

        InputStream inputStream = createSingleSheetExcel(sheet1Data, UserBasicDTO.class, "用户基础");

        // 记录批次调用次数
        List<Integer> batchSizes = new ArrayList<>();

        // 执行导入（批次大小为10）
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchSize(10)
                .batchHandler(dataList -> {
                    batchSizes.add(dataList.size());
                })
                .doRead();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isAllSuccess());

        // 验证批次处理（25条数据，批次大小10，应该有3个批次：10+10+5）
        assertEquals(3, batchSizes.size());
        assertEquals(10, batchSizes.get(0));
        assertEquals(10, batchSizes.get(1));
        assertEquals(5, batchSizes.get(2));
    }

    /**
     * 测试错误处理器
     */
    @Test
    void testMultiSheetImport_ErrorHandler() {
        // 创建测试数据（两条验证失败）
        List<UserBasicDTO> sheet1Data = new ArrayList<>();
        sheet1Data.add(createUserBasic("U001", "", "13800138001", "zhangsan@test.com")); // 姓名为空
        sheet1Data.add(createUserBasic("U002", "", "13800138002", "lisi@test.com")); // 姓名为空

        InputStream inputStream = createSingleSheetExcel(sheet1Data, UserBasicDTO.class, "用户基础");

        // 记录错误调用
        List<SheetError> capturedErrors = new ArrayList<>();

        // 执行导入
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .validator(data -> {
                    if (data.getName() == null || data.getName().trim().isEmpty()) {
                        return ValidationResult.fail("姓名不能为空", data);
                    }
                    return ValidationResult.ok(data);
                })
                .errorHandler(capturedErrors::add)
                .batchSize(10)
                .doRead();

        // 验证错误处理器被调用
        assertEquals(2, capturedErrors.size());
        assertEquals("姓名不能为空", capturedErrors.get(0).getErrorMsg());
        assertEquals("姓名不能为空", capturedErrors.get(1).getErrorMsg());
    }

    /**
     * 测试空数据导入
     */
    @Test
    void testMultiSheetImport_EmptyData() {
        // 创建空数据
        List<UserBasicDTO> emptyData = new ArrayList<>();

        InputStream inputStream = createSingleSheetExcel(emptyData, UserBasicDTO.class, "用户基础");

        // 执行导入
        MultiSheetImportResult result = excelImportService.importMultiSheet(inputStream)
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchSize(10)
                .doRead();

        // 验证结果
        assertNotNull(result);
        assertTrue(result.isAllSuccess());
        assertEquals(0, result.getTotalRows());
        assertEquals(0, result.getTotalSuccessRows());
        assertEquals(0, result.getTotalFailRows());

        List<UserBasicDTO> importedData = result.getSheetDataList("用户基础");
        assertTrue(importedData.isEmpty());
    }

    // ==================== 辅助方法 ====================

    private UserBasicDTO createUserBasic(String userId, String name, String phone, String email) {
        UserBasicDTO dto = new UserBasicDTO();
        dto.setUserId(userId);
        dto.setName(name);
        dto.setPhone(phone);
        dto.setEmail(email);
        return dto;
    }

    private UserAddressDTO createUserAddress(String userId, String province, String city, String district, String detailAddress) {
        UserAddressDTO dto = new UserAddressDTO();
        dto.setUserId(userId);
        dto.setProvince(province);
        dto.setCity(city);
        dto.setDistrict(district);
        dto.setDetailAddress(detailAddress);
        return dto;
    }

    private <T> InputStream createSingleSheetExcel(List<T> data, Class<T> clazz, String sheetName) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        EasyExcel.write(outputStream, clazz).sheet(sheetName).doWrite(data);
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    private InputStream createMultiSheetExcel(List<UserBasicDTO> sheet1Data, List<UserAddressDTO> sheet2Data) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        // 使用同一个ExcelWriter写入多个Sheet
        com.alibaba.excel.ExcelWriter excelWriter = EasyExcel.write(outputStream).build();

        // 写入第一个Sheet
        com.alibaba.excel.write.metadata.WriteSheet writeSheet1 = EasyExcel.writerSheet(0, "用户基础")
                .head(UserBasicDTO.class)
                .build();
        excelWriter.write(sheet1Data, writeSheet1);

        // 写入第二个Sheet
        com.alibaba.excel.write.metadata.WriteSheet writeSheet2 = EasyExcel.writerSheet(1, "用户地址")
                .head(UserAddressDTO.class)
                .build();
        excelWriter.write(sheet2Data, writeSheet2);

        excelWriter.finish();
        return new ByteArrayInputStream(outputStream.toByteArray());
    }

    // ==================== DTO定义 ====================

    public static class UserBasicDTO {
        private String userId;
        private String name;
        private String phone;
        private String email;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    public static class UserAddressDTO {
        private String userId;
        private String province;
        private String city;
        private String district;
        private String detailAddress;

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getProvince() { return province; }
        public void setProvince(String province) { this.province = province; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getDistrict() { return district; }
        public void setDistrict(String district) { this.district = district; }
        public String getDetailAddress() { return detailAddress; }
        public void setDetailAddress(String detailAddress) { this.detailAddress = detailAddress; }
    }
}
