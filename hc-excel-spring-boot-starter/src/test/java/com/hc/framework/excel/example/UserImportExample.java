package com.hc.framework.excel.example;

import com.alibaba.excel.read.listener.ReadListener;
import com.hc.framework.excel.model.ExcelImportRequest;
import com.hc.framework.excel.model.ExcelImportResult;
import com.hc.framework.excel.model.ExcelTaskStatus;
import com.hc.framework.excel.service.ExcelImportService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Excel导入使用示例
 *
 * <p>演示如何在业务中使用Excel导入功能</p>
 */
@RestController
@RequestMapping("/api/user")
public class UserImportExample {

    @Autowired
    private ExcelImportService excelImportService;

    @Autowired
    private UserService userService;

    // ==================== 数据模型 ====================

    /**
     * 用户导入模型
     */
    @Data
    public static class UserImportDTO {
        //@ExcelProperty("姓名")
        private String name;
        
        //@ExcelProperty("手机号")
        private String phone;
        
        //@ExcelProperty("邮箱")
        private String email;
        
        //@ExcelProperty("部门")
        private String department;
    }

    // ==================== 同步导入（小文件 < 1万行）====================

    /**
     * 同步导入用户（简单场景）
     */
    @PostMapping("/import/sync")
    public Result<String> importUsersSync(@RequestParam("file") MultipartFile file) throws IOException {
        // 1. 创建导入请求
        ExcelImportRequest request = ExcelImportRequest.builder()
                .file(file)
                .sheetName("用户列表")
                .headRowNumber(1)  // 第2行开始是数据（第1行是表头）
                .build();

        // 2. 执行导入
        ExcelImportResult<UserImportDTO> result = excelImportService.importData(
                request, 
                UserImportDTO.class
        );

        // 3. 处理导入结果
        if (result.isSuccess()) {
            // 全部导入成功
            return Result.success("成功导入 " + result.getSuccessRows() + " 条数据");
        } else {
            // 有失败数据
            return Result.fail("导入失败: " + result.getFailRows() + " 条数据有误");
        }
    }

    /**
     * 同步导入带自定义处理
     */
    @PostMapping("/import/sync/custom")
    public Result<String> importUsersSyncCustom(@RequestParam("file") MultipartFile file) throws IOException {
        excelImportService.importData(
                file.getInputStream(),
                UserImportDTO.class,
                new CustomUserReadListener(userService)
        );
        return Result.success("导入完成");
    }

    // ==================== 异步导入（大文件 > 1万行）====================

    /**
     * 异步导入用户（批量处理大文件）
     */
    @PostMapping("/import/async")
    public Result<String> importUsersAsync(@RequestParam("file") MultipartFile file) {
        // 1. 创建导入请求
        ExcelImportRequest request = ExcelImportRequest.builder()
                .file(file)
                .sheetName("用户列表")
                .headRowNumber(1)
                .batchSize(500)  // 每500条处理一次
                .build();

        // 2. 异步导入，返回任务ID
        String taskId = excelImportService.importDataAsync(
                request,
                UserImportDTO.class,
                // 批次处理回调：每读取一批数据就执行
                batchData -> {
                    System.out.println("处理第 " + batchData.size() + " 条数据");
                    userService.saveBatch(batchData);
                },
                // 进度回调：实时返回处理进度
                processedCount -> {
                    System.out.println("已处理: " + processedCount + " 条");
                }
        );

        return Result.success(taskId);
    }

    /**
     * 异步导入带错误处理
     */
    @PostMapping("/import/async/with-error-handler")
    public Result<String> importUsersAsyncWithErrorHandler(@RequestParam("file") MultipartFile file) {
        ExcelImportRequest request = ExcelImportRequest.builder()
                .file(file)
                .batchSize(500)
                .build();

        String taskId = excelImportService.importDataAsync(
                request,
                UserImportDTO.class,
                // 批次处理
                batchData -> userService.saveBatch(batchData),
                // 错误处理：记录哪些行数据有问题
                errorRow -> {
                    System.err.println("第 " + errorRow.getRowNum() + " 行错误: " + errorRow.getErrorMsg());
                    // 可以保存到错误日志表，或发送通知
                },
                // 进度回调
                processedCount -> {}
        );

        return Result.success(taskId);
    }

    /**
     * 查询导入任务状态
     */
    @GetMapping("/import/status/{taskId}")
    public Result<ExcelTaskStatus> getImportStatus(@PathVariable String taskId) {
        ExcelTaskStatus status = excelImportService.getTaskStatus(taskId);
        return Result.success(status);
    }

    // ==================== 实际业务场景 ====================

    /**
     * 实际业务示例：导入订单并更新库存
     */
    @PostMapping("/import/orders")
    public Result<String> importOrders(@RequestParam("file") MultipartFile file) {
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        ExcelImportRequest request = ExcelImportRequest.builder()
                .file(file)
                .batchSize(1000)
                .build();

        String taskId = excelImportService.importDataAsync(
                request,
                OrderImportDTO.class,
                // 批次处理：保存订单并更新库存
                orders -> {
                    for (OrderImportDTO order : orders) {
                        try {
                            // 1. 校验数据
                            validateOrder(order);
                            
                            // 2. 保存订单
                            userService.saveOrder(order);
                            
                            // 3. 更新库存
                            userService.updateStock(order.getProductId(), order.getQuantity());
                            
                            successCount.incrementAndGet();
                        } catch (Exception e) {
                            failCount.incrementAndGet();
                            System.err.println("订单处理失败: " + order.getOrderNo() + ", 原因: " + e.getMessage());
                        }
                    }
                },
                // 错误处理
                errorRow -> {
                    failCount.incrementAndGet();
                    System.err.println("行 " + errorRow.getRowNum() + " 解析失败: " + errorRow.getErrorMsg());
                },
                // 进度回调
                count -> {
                    System.out.println("进度: " + count + " 条");
                }
        );

        return Result.success("导入任务已提交，任务ID: " + taskId);
    }

    // ==================== 辅助方法 ====================

    private void validateOrder(OrderImportDTO order) {
        if (order.getOrderNo() == null || order.getOrderNo().isEmpty()) {
            throw new IllegalArgumentException("订单号不能为空");
        }
        if (order.getQuantity() <= 0) {
            throw new IllegalArgumentException("数量必须大于0");
        }
    }

    // ==================== 模型和服务 ====================

    @Data
    public static class OrderImportDTO {
        private String orderNo;
        private String productId;
        private Integer quantity;
        private String customerName;
    }

    @Data
    public static class Result<T> {
        private boolean success;
        private String message;
        private T data;

        public static <T> Result<T> success(T data) {
            Result<T> r = new Result<>();
            r.success = true;
            r.data = data;
            return r;
        }

        public static <T> Result<T> fail(String message) {
            Result<T> r = new Result<>();
            r.success = false;
            r.message = message;
            return r;
        }
    }

    // 模拟业务服务
    public static class UserService {
        public void saveBatch(List<UserImportDTO> users) {
            // 批量保存用户
        }

        public void saveOrder(OrderImportDTO order) {
            // 保存订单
        }

        public void updateStock(String productId, int quantity) {
            // 更新库存
        }
    }

    // 自定义读取监听器
    public static class CustomUserReadListener implements ReadListener<UserImportDTO> {
        private final UserService userService;

        public CustomUserReadListener(UserService userService) {
            this.userService = userService;
        }

        @Override
        public void invoke(UserImportDTO data, com.alibaba.excel.context.AnalysisContext context) {
            // 每读取一行数据就执行
            System.out.println("读取数据: " + data.getName());
            // 可以实时处理，不用等全部读完
        }

        @Override
        public void doAfterAllAnalysed(com.alibaba.excel.context.AnalysisContext context) {
            // 全部读完后执行
            System.out.println("所有数据处理完成");
        }
    }
}
