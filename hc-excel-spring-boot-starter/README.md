# hc-excel-spring-boot-starter

## 模块简介

基于 EasyExcel 4.x 的 Excel 导入导出 Starter，提供同步/异步导出、多 Sheet 导入、动态表头、任务状态追踪等功能。

## 功能特性

- **异步导出**：线程池异步导出，通过任务 ID 查询进度和下载
- **多 Sheet 导入**：支持多 Sheet 独立校验，每个 Sheet 独立返回错误
- **动态表头**：支持运行时动态生成表头
- **数据透视表**：支持交叉表/透视表导出
- **模板导出**：基于模板文件填充数据导出
- **可插拔存储**：任务状态支持本地文件系统或 Redis 存储（自动选择）
- **SPI 扩展**：`ExcelFileStorage`、`ExcelTaskStore`、`ExcelOperationRecorder`、`ExcelOperatorResolver`

## 快速开始

```xml
<dependency>
    <groupId>com.hnhegui.framework</groupId>
    <artifactId>hc-excel-spring-boot-starter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

## 配置说明

```yaml
hc:
  excel:
    async-pool:
      core-size: 4
      max-size: 8
      queue-capacity: 100
```

## 依赖说明

- Java 17
- EasyExcel 4.0.1
- Spring Data Redis（可选，用于分布式任务存储）
