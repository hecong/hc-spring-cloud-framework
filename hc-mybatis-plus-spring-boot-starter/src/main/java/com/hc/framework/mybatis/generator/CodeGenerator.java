package com.hc.framework.mybatis.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;

/**
 * MyBatis-Plus 代码生成器
 * <p>
 * 基于 MyBatis-Plus Generator 3.5.5 实现
 *
 * @author hc
 */
@Slf4j
public class CodeGenerator {

    /**
     * 代码生成器配置
     */
    public static class GeneratorConfig {
        /** 数据库URL */
        private String url;
        /** 数据库用户名 */
        private String username;
        /** 数据库密码 */
        private String password;
        /** 作者 */
        private String author = "hc";
        /** 输出目录 */
        private String outputDir = System.getProperty("user.dir") + "/src/main/java";
        /** 父包名 */
        private String parentPackage = "com.example";
        /** 模块名 */
        private String moduleName = "";
        /** 表前缀 */
        private String[] tablePrefix = {};
        /** 要生成的表名 */
        private String[] includeTables = {};

        public GeneratorConfig url(String url) {
            this.url = url;
            return this;
        }

        public GeneratorConfig username(String username) {
            this.username = username;
            return this;
        }

        public GeneratorConfig password(String password) {
            this.password = password;
            return this;
        }

        public GeneratorConfig author(String author) {
            this.author = author;
            return this;
        }

        public GeneratorConfig outputDir(String outputDir) {
            this.outputDir = outputDir;
            return this;
        }

        public GeneratorConfig parentPackage(String parentPackage) {
            this.parentPackage = parentPackage;
            return this;
        }

        public GeneratorConfig moduleName(String moduleName) {
            this.moduleName = moduleName;
            return this;
        }

        public GeneratorConfig tablePrefix(String... tablePrefix) {
            this.tablePrefix = tablePrefix;
            return this;
        }

        public GeneratorConfig includeTables(String... includeTables) {
            this.includeTables = includeTables;
            return this;
        }

        public void generate() {
            if (url == null || username == null || password == null) {
                throw new IllegalArgumentException("数据库连接信息不能为空");
            }

            FastAutoGenerator.create(url, username, password)
                    // 全局配置
                    .globalConfig(builder -> {
                        builder.author(author)
                                .outputDir(outputDir)
                                .disableOpenDir()
                                .commentDate("yyyy-MM-dd");
                    })
                    // 包配置
                    .packageConfig(builder -> {
                        builder.parent(parentPackage)
                                .moduleName(moduleName)
                                .entity("entity")
                                .service("service")
                                .serviceImpl("service.impl")
                                .mapper("mapper")
                                .xml("mapper.xml")
                                .pathInfo(Collections.singletonMap(OutputFile.xml,
                                        outputDir.replace("/java", "/resources/mapper")));
                    })
                    // 策略配置
                    .strategyConfig(builder -> {
                        builder.addInclude(includeTables)
                                .addTablePrefix(tablePrefix)
                                // Entity 策略
                                .entityBuilder()
                                .enableLombok()
                                .enableTableFieldAnnotation()
                                .logicDeleteColumnName("deleted")
                                .addTableFills(
                                        new com.baomidou.mybatisplus.generator.fill.Column("create_time", com.baomidou.mybatisplus.annotation.FieldFill.INSERT),
                                        new com.baomidou.mybatisplus.generator.fill.Column("update_time", com.baomidou.mybatisplus.annotation.FieldFill.INSERT_UPDATE)
                                )
                                // Service 策略
                                .serviceBuilder()
                                .formatServiceFileName("%sService")
                                .formatServiceImplFileName("%sServiceImpl")
                                // Mapper 策略
                                .mapperBuilder()
                                .enableMapperAnnotation()
                                .enableBaseResultMap()
                                .enableBaseColumnList();
                    })
                    // 使用默认模板配置
                    // 模板引擎
                    .templateEngine(new FreemarkerTemplateEngine())
                    .execute();

            log.info("代码生成完成！输出目录: {}", outputDir);
        }
    }

    /**
     * 创建代码生成器配置
     */
    public static GeneratorConfig create() {
        return new GeneratorConfig();
    }

    /**
     * 快速生成代码
     *
     * @param url       数据库URL
     * @param username  用户名
     * @param password  密码
     * @param tables    要生成的表名
     */
    public static void quickGenerate(String url, String username, String password, String... tables) {
        create()
                .url(url)
                .username(username)
                .password(password)
                .includeTables(tables)
                .generate();
    }
}
