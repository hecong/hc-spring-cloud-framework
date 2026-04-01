package com.hnhegui.framework.excel.example;

import com.hnhegui.framework.excel.model.multisheet.*;
import com.hnhegui.framework.excel.service.ExcelImportService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * 多Sheet导入使用示例
 * 演示如何使用链式API导入多个Sheet，每个Sheet不同结构
 */
@Service
public class MultiSheetImportExample {

    private final ExcelImportService excelImportService;

    public MultiSheetImportExample(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    /**
     * 示例1：基础用法 - 导入用户信息（多个Sheet）
     *
     * Sheet1: 用户基础信息
     * Sheet2: 用户地址信息
     * Sheet3: 用户订单信息
     */
    public void importUserData(MultipartFile file) throws IOException {
        MultiSheetImportResult result = excelImportService.importMultiSheet(file.getInputStream())
                // Sheet1: 用户基础信息
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchSize(100)
                .batchHandler(dataList -> {
                    // 批次处理用户基础信息
                    System.out.println("用户基础信息导入: " + dataList.size() + " 条");
                    // userService.saveBasic(dataList);
                })

                // Sheet2: 用户地址信息
                .sheet(1, "用户地址", UserAddressDTO.class)
                .batchSize(50)
                .batchHandler(dataList -> {
                    // 批次处理用户地址信息
                    System.out.println("用户地址信息导入: " + dataList.size() + " 条");
                    // userService.saveAddress(dataList);
                })

                // Sheet3: 用户订单信息
                .sheet(2, "用户订单", UserOrderDTO.class)
                .batchSize(200)
                .batchHandler(dataList -> {
                    // 批次处理用户订单信息
                    System.out.println("用户订单信息导入: " + dataList.size() + " 条");
                    // orderService.saveOrder(dataList);
                })

                .doRead();

        // 处理结果
        handleResult(result);
    }

    /**
     * 示例2：带数据验证的导入
     */
    public void importWithValidation(MultipartFile file) throws IOException {
        MultiSheetImportResult result = excelImportService.importMultiSheet(file.getInputStream())
                // Sheet1: 用户基础信息（带验证）
                .sheet(0, "用户基础", UserBasicDTO.class)
                .validator(data -> {
                    // 单条数据验证
                    if (data.getName() == null || data.getName().trim().isEmpty()) {
                        return ValidationResult.fail("姓名不能为空", data);
                    }
                    if (data.getPhone() == null || !data.getPhone().matches("^1[3-9]\\d{9}$")) {
                        return ValidationResult.fail("手机号格式不正确", data);
                    }
                    return ValidationResult.ok(data);
                })
                .errorHandler(error -> {
                    // 错误处理
                    System.err.println("验证失败: " + error);
                })
                .batchHandler(dataList -> {
                    System.out.println("验证通过的用户: " + dataList.size() + " 条");
                })

                // Sheet2: 用户地址（带验证）
                .sheet(1, "用户地址", UserAddressDTO.class)
                .validator(data -> {
                    if (data.getProvince() == null || data.getProvince().trim().isEmpty()) {
                        return ValidationResult.fail("省份不能为空", data);
                    }
                    if (data.getCity() == null || data.getCity().trim().isEmpty()) {
                        return ValidationResult.fail("城市不能为空", data);
                    }
                    return ValidationResult.ok(data);
                })
                .batchHandler(dataList -> {
                    System.out.println("验证通过的地址: " + dataList.size() + " 条");
                })

                .doRead();

        handleResult(result);
    }

    /**
     * 示例3：带上下文的验证（Sheet间数据关联）
     * 例如：验证地址表中的userId是否在用户表中存在
     */
    public void importWithContextValidation(MultipartFile file) throws IOException {
        MultiSheetImportResult result = excelImportService.importMultiSheet(file.getInputStream())
                // Sheet1: 先导入用户，缓存到上下文
                .sheet(0, "用户基础", UserBasicDTO.class)
                .batchHandler(dataList -> {
                    System.out.println("用户导入: " + dataList.size() + " 条");
                })

                // Sheet2: 地址验证时检查userId是否存在
                .sheet(1, "用户地址", UserAddressDTO.class)
                .validator((data, ctx) -> {
                    // 从上下文获取已导入的用户数据
                    List<UserBasicDTO> users = ctx.getSheetData("用户基础");

                    // 检查userId是否存在
                    boolean userExists = users.stream()
                            .anyMatch(u -> u.getUserId().equals(data.getUserId()));

                    if (!userExists) {
                        return ValidationResult.fail("用户ID " + data.getUserId() + " 不存在", data);
                    }

                    return ValidationResult.ok(data);
                })
                .batchHandler(dataList -> {
                    System.out.println("地址导入: " + dataList.size() + " 条");
                })

                .doRead();

        handleResult(result);
    }

    /**
     * 示例4：处理导入结果
     */
    private void handleResult(MultiSheetImportResult result) {
        // 判断是否全部成功
        if (result.isAllSuccess()) {
            System.out.println("全部导入成功！");
            System.out.println("总行数: " + result.getTotalRows());
        } else {
            System.out.println("导入完成，但存在错误:");
            System.out.println("总成功: " + result.getTotalSuccessRows() + " 条");
            System.out.println("总失败: " + result.getTotalFailRows() + " 条");

            // 获取所有错误
            List<SheetError> allErrors = result.getAllErrors();
            for (SheetError error : allErrors) {
                System.out.println("  - " + error);
            }
        }

        // 获取各Sheet的数据
        List<UserBasicDTO> users = result.getSheetDataList("用户基础");
        List<UserAddressDTO> addresses = result.getSheetDataList("用户地址");
        List<UserOrderDTO> orders = result.getSheetDataList("用户订单");

        System.out.println("用户数据: " + users.size() + " 条");
        System.out.println("地址数据: " + addresses.size() + " 条");
        System.out.println("订单数据: " + orders.size() + " 条");
    }

    // ==================== DTO定义 ====================

    /**
     * 用户基础信息DTO
     */
    public static class UserBasicDTO {
        private String userId;
        private String name;
        private String phone;
        private String email;

        // Getters and Setters
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPhone() { return phone; }
        public void setPhone(String phone) { this.phone = phone; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }

    /**
     * 用户地址信息DTO
     */
    public static class UserAddressDTO {
        private String userId;
        private String province;
        private String city;
        private String district;
        private String detailAddress;

        // Getters and Setters
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

    /**
     * 用户订单信息DTO
     */
    public static class UserOrderDTO {
        private String orderId;
        private String userId;
        private BigDecimal amount;
        private String status;

        // Getters and Setters
        public String getOrderId() { return orderId; }
        public void setOrderId(String orderId) { this.orderId = orderId; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public BigDecimal getAmount() { return amount; }
        public void setAmount(BigDecimal amount) { this.amount = amount; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }
}
