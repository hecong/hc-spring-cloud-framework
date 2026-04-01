package com.hnhegui.framework.excel.util;

import com.alibaba.excel.write.handler.CellWriteHandler;
import com.alibaba.excel.write.handler.context.CellWriteHandlerContext;
import com.alibaba.excel.write.metadata.style.WriteCellStyle;
import com.alibaba.excel.write.metadata.style.WriteFont;
import com.alibaba.excel.write.style.HorizontalCellStyleStrategy;
import com.hnhegui.framework.excel.model.PivotTableHeads;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel样式工具类
 * 
 * <p>提供EasyExcel导出时的常用样式处理器，包括行高、列宽、表头样式、内容样式等。</p>
 * 
 * <h3>使用示例：</h3>
 * <pre>{@code
 * // 1. 设置行高
 * EasyExcel.write(outputStream, Demo.class)
 *     .registerWriteHandler(ExcelStyleUtil.createRowHeightHandler(25f))
 *     .sheet("Sheet1")
 *     .doWrite(data);
 *
 * // 2. 设置自适应列宽
 * EasyExcel.write(outputStream, Demo.class)
 *     .registerWriteHandler(ExcelStyleUtil.createAutoColumnWidthHandler())
 *     .sheet("Sheet1")
 *     .doWrite(data);
 *
 * // 3. 设置表头和内容样式
 * EasyExcel.write(outputStream, Demo.class)
 *     .registerWriteHandler(ExcelStyleUtil.createCellStyleStrategy())
 *     .sheet("Sheet1")
 *     .doWrite(data);
 *
 * // 4. 组合多个处理器
 * EasyExcel.write(outputStream, Demo.class)
 *     .registerWriteHandler(ExcelStyleUtil.createRowHeightHandler(22f))
 *     .registerWriteHandler(ExcelStyleUtil.createAutoColumnWidthHandler(15, 50))
 *     .registerWriteHandler(ExcelStyleUtil.createCellStyleStrategy())
 *     .sheet("Sheet1")
 *     .doWrite(data);
 * }</pre>
 *
 * @author hc-framework
 * @since 1.0.0
 */
public class ExcelStyleUtil {

    /**
     * 默认行高（单位：磅）
     */
    public static final float DEFAULT_ROW_HEIGHT = 20f;

    /**
     * 默认列宽（单位：字符）
     */
    public static final int DEFAULT_COLUMN_WIDTH = 20;

    /**
     * 最小列宽（单位：字符）
     */
    public static final int MIN_COLUMN_WIDTH = 10;

    /**
     * 最大列宽（单位：字符）
     */
    public static final int MAX_COLUMN_WIDTH = 100;

    /**
     * 默认表头背景色
     */
    public static final IndexedColors DEFAULT_HEADER_BG_COLOR = IndexedColors.GREY_25_PERCENT;

    /**
     * 默认表头字体颜色
     */
    public static final IndexedColors DEFAULT_HEADER_FONT_COLOR = IndexedColors.BLACK;

    private ExcelStyleUtil() {
    }

    // ==================== 行高处理器 ====================

    /**
     * 创建默认行高处理器
     * 
     * <p>使用默认行高 {@link #DEFAULT_ROW_HEIGHT}</p>
     *
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createRowHeightHandler() {
        return createRowHeightHandler(DEFAULT_ROW_HEIGHT);
    }

    /**
     * 创建行高处理器
     * 
     * <p>设置所有行的行高，适用于需要统一行高的场景。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * EasyExcel.write(outputStream, Demo.class)
     *     .registerWriteHandler(ExcelStyleUtil.createRowHeightHandler(25f))
     *     .sheet("Sheet1")
     *     .doWrite(data);
     * }</pre>
     *
     * @param rowHeight 行高（单位：磅，1磅≈0.35毫米）
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createRowHeightHandler(float rowHeight) {
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                Cell cell = context.getCell();
                Row row = cell.getRow();
                if (row != null) {
                    row.setHeightInPoints(rowHeight);
                }
            }
        };
    }

    // ==================== 列宽处理器 ====================

    /**
     * 创建自适应列宽处理器
     * 
     * <p>根据单元格内容自动计算列宽，使用默认最小/最大列宽限制。</p>
     *
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createAutoColumnWidthHandler() {
        return createAutoColumnWidthHandler(MIN_COLUMN_WIDTH, MAX_COLUMN_WIDTH);
    }

    /**
     * 创建自适应列宽处理器
     * 
     * <p>根据单元格内容自动计算列宽，支持设置最小和最大列宽限制。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * EasyExcel.write(outputStream, Demo.class)
     *     .registerWriteHandler(ExcelStyleUtil.createAutoColumnWidthHandler(12, 50))
     *     .sheet("Sheet1")
     *     .doWrite(data);
     * }</pre>
     *
     * <h4>注意：</h4>
     * <ul>
     *   <li>列宽单位为字符数，一个中文字符约等于2个英文字符宽度</li>
     *   <li>计算结果会额外增加2个字符的padding</li>
     * </ul>
     *
     * @param minWidth 最小列宽（单位：字符）
     * @param maxWidth 最大列宽（单位：字符）
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createAutoColumnWidthHandler(int minWidth, int maxWidth) {
        return new CellWriteHandler() {
            private final List<Integer> columnWidths = new ArrayList<>();

            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                Cell cell = context.getCell();
                int columnIndex = cell.getColumnIndex();
                
                // 获取单元格内容长度
                String cellValue = getCellValue(cell);
                int length = calculateTextLength(cellValue);
                
                // 确保列表大小足够
                while (columnWidths.size() <= columnIndex) {
                    columnWidths.add(minWidth);
                }
                
                // 更新该列的最大宽度
                int currentMax = columnWidths.get(columnIndex);
                if (length > currentMax) {
                    columnWidths.set(columnIndex, length);
                }
                
                // 实时设置列宽
                int width = Math.min(columnWidths.get(columnIndex), maxWidth);
                width = Math.max(width, minWidth);
                cell.getSheet().setColumnWidth(columnIndex, width * 256);
            }

            private String getCellValue(Cell cell) {
                if (cell == null) {
                    return "";
                }
                CellType cellType = cell.getCellType();
                if (cellType == CellType.STRING) {
                    return cell.getStringCellValue();
                } else if (cellType == CellType.NUMERIC) {
                    return String.valueOf(cell.getNumericCellValue());
                } else if (cellType == CellType.BOOLEAN) {
                    return String.valueOf(cell.getBooleanCellValue());
                }
                return "";
            }

            private int calculateTextLength(String text) {
                if (text == null || text.isEmpty()) {
                    return 0;
                }
                // 中文字符按2个字符计算，英文按1个字符
                int length = 0;
                for (char c : text.toCharArray()) {
                    if (Character.toString(c).getBytes().length > 1) {
                        length += 2;
                    } else {
                        length += 1;
                    }
                }
                return length + 2; // 额外padding
            }
        };
    }

    /**
     * 创建固定列宽处理器
     * 
     * <p>为所有列设置统一的固定宽度。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * EasyExcel.write(outputStream, Demo.class)
     *     .registerWriteHandler(ExcelStyleUtil.createFixedColumnWidthHandler(15))
     *     .sheet("Sheet1")
     *     .doWrite(data);
     * }</pre>
     *
     * @param columnWidth 列宽（单位：字符）
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createFixedColumnWidthHandler(int columnWidth) {
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                Cell cell = context.getCell();
                int columnIndex = cell.getColumnIndex();
                cell.getSheet().setColumnWidth(columnIndex, columnWidth * 256);
            }
        };
    }

    // ==================== 单元格样式策略 ====================

    /**
     * 创建默认单元格样式策略
     * 
     * <p>包含默认的表头样式和内容样式。</p>
     * 
     * <h4>样式说明：</h4>
     * <ul>
     *   <li>表头：灰色背景、黑色加粗字体、居中对齐</li>
     *   <li>内容：白色背景、黑色普通字体、居中对齐</li>
     * </ul>
     *
     * @return 水平单元格样式策略
     */
    public static HorizontalCellStyleStrategy createCellStyleStrategy() {
        return createCellStyleStrategy(null, null);
    }

    /**
     * 创建自定义单元格样式策略
     * 
     * <p>支持自定义表头和内容样式，传入null则使用默认样式。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * // 自定义表头样式
     * WriteCellStyle headStyle = new WriteCellStyle();
     * headStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
     * headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
     * 
     * WriteFont headFont = new WriteFont();
     * headFont.setFontHeightInPoints((short) 12);
     * headFont.setBold(true);
     * headStyle.setWriteFont(headFont);
     * 
     * // 创建策略
     * HorizontalCellStyleStrategy strategy = ExcelStyleUtil.createCellStyleStrategy(headStyle, null);
     * 
     * EasyExcel.write(outputStream, Demo.class)
     *     .registerWriteHandler(strategy)
     *     .sheet("Sheet1")
     *     .doWrite(data);
     * }</pre>
     *
     * @param headWriteCellStyle 表头样式，为null时使用默认样式
     * @param contentWriteCellStyle 内容样式，为null时使用默认样式
     * @return 水平单元格样式策略
     */
    public static HorizontalCellStyleStrategy createCellStyleStrategy(
            WriteCellStyle headWriteCellStyle, 
            WriteCellStyle contentWriteCellStyle) {
        
        // 表头样式
        WriteCellStyle headStyle = headWriteCellStyle != null ? headWriteCellStyle : createDefaultHeadStyle();
        // 内容样式
        WriteCellStyle contentStyle = contentWriteCellStyle != null ? contentWriteCellStyle : createDefaultContentStyle();
        
        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    /**
     * 创建带边框的单元格样式策略
     * 
     * <p>为表头和内容单元格添加边框。</p>
     *
     * @return 水平单元格样式策略
     */
    public static HorizontalCellStyleStrategy createBorderedCellStyleStrategy() {
        WriteCellStyle headStyle = createDefaultHeadStyle();
        addBorder(headStyle);
        
        WriteCellStyle contentStyle = createDefaultContentStyle();
        addBorder(contentStyle);
        
        return new HorizontalCellStyleStrategy(headStyle, contentStyle);
    }

    // ==================== 表头样式创建 ====================

    /**
     * 创建默认表头样式
     * 
     * <p>包含：灰色背景、黑色加粗字体、水平居中、垂直居中。</p>
     *
     * @return 表头单元格样式
     */
    public static WriteCellStyle createDefaultHeadStyle() {
        WriteCellStyle headStyle = new WriteCellStyle();
        
        // 背景色
        headStyle.setFillForegroundColor(DEFAULT_HEADER_BG_COLOR.getIndex());
        headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        
        // 字体
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short) 11);
        headFont.setFontName("微软雅黑");
        headFont.setBold(true);
        headFont.setColor(DEFAULT_HEADER_FONT_COLOR.getIndex());
        headStyle.setWriteFont(headFont);
        
        // 对齐
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return headStyle;
    }

    /**
     * 创建自定义表头样式
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * WriteCellStyle headStyle = ExcelStyleUtil.createHeadStyle(
     *     IndexedColors.LIGHT_BLUE,   // 背景色
     *     IndexedColors.WHITE,        // 字体颜色
     *     12,                          // 字体大小
     *     true                         // 是否加粗
     * );
     * }</pre>
     *
     * @param bgColor 背景颜色
     * @param fontColor 字体颜色
     * @param fontSize 字体大小（磅）
     * @param bold 是否加粗
     * @return 表头单元格样式
     */
    public static WriteCellStyle createHeadStyle(
            IndexedColors bgColor, 
            IndexedColors fontColor, 
            int fontSize, 
            boolean bold) {
        WriteCellStyle headStyle = new WriteCellStyle();
        
        // 背景色
        headStyle.setFillForegroundColor(bgColor.getIndex());
        headStyle.setFillPatternType(FillPatternType.SOLID_FOREGROUND);
        
        // 字体
        WriteFont headFont = new WriteFont();
        headFont.setFontHeightInPoints((short) fontSize);
        headFont.setFontName("微软雅黑");
        headFont.setBold(bold);
        headFont.setColor(fontColor.getIndex());
        headStyle.setWriteFont(headFont);
        
        // 对齐
        headStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        headStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return headStyle;
    }

    // ==================== 内容样式创建 ====================

    /**
     * 创建默认内容样式
     * 
     * <p>包含：白色背景、黑色普通字体、水平居中、垂直居中。</p>
     *
     * @return 内容单元格样式
     */
    public static WriteCellStyle createDefaultContentStyle() {
        WriteCellStyle contentStyle = new WriteCellStyle();
        
        // 字体
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints((short) 10);
        contentFont.setFontName("微软雅黑");
        contentStyle.setWriteFont(contentFont);
        
        // 对齐
        contentStyle.setHorizontalAlignment(HorizontalAlignment.CENTER);
        contentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        
        return contentStyle;
    }

    /**
     * 创建自定义内容样式
     *
     * @param fontColor 字体颜色
     * @param fontSize 字体大小（磅）
     * @param horizontalAlignment 水平对齐方式
     * @param verticalAlignment 垂直对齐方式
     * @return 内容单元格样式
     */
    public static WriteCellStyle createContentStyle(
            IndexedColors fontColor, 
            int fontSize,
            HorizontalAlignment horizontalAlignment,
            VerticalAlignment verticalAlignment) {
        WriteCellStyle contentStyle = new WriteCellStyle();
        
        // 字体
        WriteFont contentFont = new WriteFont();
        contentFont.setFontHeightInPoints((short) fontSize);
        contentFont.setFontName("微软雅黑");
        contentFont.setColor(fontColor.getIndex());
        contentStyle.setWriteFont(contentFont);
        
        // 对齐
        contentStyle.setHorizontalAlignment(horizontalAlignment);
        contentStyle.setVerticalAlignment(verticalAlignment);
        
        return contentStyle;
    }

    // ==================== 辅助方法 ====================

    /**
     * 为样式添加边框
     *
     * @param style 单元格样式
     */
    public static void addBorder(WriteCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    /**
     * 创建字体
     *
     * @param fontName 字体名称
     * @param fontSize 字体大小（磅）
     * @param bold 是否加粗
     * @param color 字体颜色
     * @return 字体对象
     */
    public static WriteFont createFont(String fontName, int fontSize, boolean bold, IndexedColors color) {
        WriteFont font = new WriteFont();
        font.setFontName(fontName);
        font.setFontHeightInPoints((short) fontSize);
        font.setBold(bold);
        font.setColor(color.getIndex());
        return font;
    }

    // ==================== 列转行表头样式处理器 ====================

    /**
     * 创建列转行表头样式处理器
     * 
     * <p>为固定列、动态列、汇总列设置不同的样式，适用于动态列表头场景。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * // 构建表头定义
     * PivotTableHeads heads = PivotTableHeads.of(
     *     Arrays.asList("市场", "区域"),
     *     Arrays.asList("market", "region"),
     *     Arrays.asList("产品A", "产品B", "产品C"),
     *     Arrays.asList("product_a", "product_b", "product_c"),
     *     "合计",
     *     "total"
     * );
     * 
     * // 创建样式处理器
     * CellWriteHandler handler = ExcelStyleUtil.createPivotTableStyleHandler(heads);
     * 
     * EasyExcel.write(outputStream)
     *     .head(heads.toDynamicHeads().stream().map(h -> Arrays.asList(h.getName())).collect(Collectors.toList()))
     *     .registerWriteHandler(handler)
     *     .sheet("Sheet1")
     *     .doWrite(data);
     * }</pre>
     *
     * <h4>默认样式说明：</h4>
     * <ul>
     *   <li>固定列：浅灰色背景、黑色加粗字体</li>
     *   <li>动态列：白色背景、蓝色加粗字体</li>
     *   <li>汇总列：浅黄色背景、红色加粗字体</li>
     * </ul>
     *
     * @param heads 列转行表头定义
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createPivotTableStyleHandler(PivotTableHeads heads) {
        return createPivotTableStyleHandler(heads, null, null, null);
    }

    /**
     * 创建列转行表头样式处理器（自定义样式）
     * 
     * <p>支持为固定列、动态列、汇总列分别指定表头样式。</p>
     *
     * @param heads 列转行表头定义
     * @param fixedStyle 固定列表头样式（null使用默认）
     * @param dynamicStyle 动态列表头样式（null使用默认）
     * @param summaryStyle 汇总列表头样式（null使用默认）
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createPivotTableStyleHandler(
            PivotTableHeads heads,
            WriteCellStyle fixedStyle,
            WriteCellStyle dynamicStyle,
            WriteCellStyle summaryStyle) {
        
        // 默认样式
        WriteCellStyle defaultFixedStyle = fixedStyle != null ? fixedStyle : createHeadStyle(
                IndexedColors.GREY_25_PERCENT, IndexedColors.BLACK, 11, true);
        WriteCellStyle defaultDynamicStyle = dynamicStyle != null ? dynamicStyle : createHeadStyle(
                IndexedColors.WHITE, IndexedColors.LIGHT_BLUE, 11, true);
        WriteCellStyle defaultSummaryStyle = summaryStyle != null ? summaryStyle : createHeadStyle(
                IndexedColors.LIGHT_YELLOW, IndexedColors.RED, 11, true);
        
        final int fixedCount = heads.getFixedColumnCount();
        final int dynamicCount = heads.getDynamicColumnCount();
        final boolean hasSummary = heads.getSummaryColumn() != null;
        final int summaryIndex = fixedCount + dynamicCount;
        
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                // 只处理表头行
                if (context.getHead() == null) {
                    return;
                }
                
                Cell cell = context.getCell();
                int columnIndex = cell.getColumnIndex();
                
                // 获取或创建单元格样式
                CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
                
                // 根据列索引选择样式
                WriteCellStyle targetStyle;
                if (columnIndex < fixedCount) {
                    // 固定列
                    targetStyle = defaultFixedStyle;
                } else if (hasSummary && columnIndex == summaryIndex) {
                    // 汇总列
                    targetStyle = defaultSummaryStyle;
                } else {
                    // 动态列
                    targetStyle = defaultDynamicStyle;
                }
                
                // 应用样式
                applyWriteCellStyle(cellStyle, targetStyle, cell.getSheet().getWorkbook());
                cell.setCellStyle(cellStyle);
            }
        };
    }

    /**
     * 创建列转行内容样式处理器
     * 
     * <p>为固定列、动态列、汇总列的内容区域设置不同的样式。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * PivotTableHeads heads = PivotTableHeads.of(...);
     * 
     * // 固定列左对齐，动态列居中，汇总列右对齐+加粗
     * CellWriteHandler handler = ExcelStyleUtil.createPivotTableContentStyleHandler(
     *     heads,
 *         HorizontalAlignment.LEFT,
     *     HorizontalAlignment.CENTER,
     *     HorizontalAlignment.RIGHT,
     *     true  // 汇总列是否加粗
     * );
     * }</pre>
     *
     * @param heads 列转行表头定义
     * @param fixedAlignment 固定列对齐方式
     * @param dynamicAlignment 动态列对齐方式
     * @param summaryAlignment 汇总列对齐方式
     * @param summaryBold 汇总列是否加粗
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createPivotTableContentStyleHandler(
            PivotTableHeads heads,
            HorizontalAlignment fixedAlignment,
            HorizontalAlignment dynamicAlignment,
            HorizontalAlignment summaryAlignment,
            boolean summaryBold) {
        
        final int fixedCount = heads.getFixedColumnCount();
        final int dynamicCount = heads.getDynamicColumnCount();
        final boolean hasSummary = heads.getSummaryColumn() != null;
        final int summaryIndex = fixedCount + dynamicCount;
        
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                // 只处理内容行（非表头）
                if (context.getHead() != null) {
                    return;
                }
                
                Cell cell = context.getCell();
                int columnIndex = cell.getColumnIndex();
                
                // 获取或创建单元格样式
                CellStyle cellStyle = cell.getSheet().getWorkbook().createCellStyle();
                
                // 根据列索引设置对齐方式
                HorizontalAlignment alignment;
                boolean bold = false;
                
                if (columnIndex < fixedCount) {
                    alignment = fixedAlignment != null ? fixedAlignment : HorizontalAlignment.LEFT;
                } else if (hasSummary && columnIndex == summaryIndex) {
                    alignment = summaryAlignment != null ? summaryAlignment : HorizontalAlignment.RIGHT;
                    bold = summaryBold;
                } else {
                    alignment = dynamicAlignment != null ? dynamicAlignment : HorizontalAlignment.CENTER;
                }
                
                cellStyle.setAlignment(alignment);
                cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
                
                // 设置字体
                org.apache.poi.ss.usermodel.Font font = cell.getSheet().getWorkbook().createFont();
                font.setFontName("微软雅黑");
                font.setFontHeightInPoints((short) 10);
                font.setBold(bold);
                cellStyle.setFont(font);
                
                cell.setCellStyle(cellStyle);
            }
        };
    }

    /**
     * 创建列转行表头列宽处理器
     * 
     * <p>为固定列、动态列、汇总列设置不同的列宽。</p>
     * 
     * <h4>使用示例：</h4>
     * <pre>{@code
     * PivotTableHeads heads = PivotTableHeads.of(...);
     * 
     * CellWriteHandler handler = ExcelStyleUtil.createPivotTableColumnWidthHandler(
     *     heads,
     *     15,   // 固定列宽
     *     12,   // 动态列宽
     *     10    // 汇总列宽
     * );
     * }</pre>
     *
     * @param heads 列转行表头定义
     * @param fixedWidth 固定列宽（字符）
     * @param dynamicWidth 动态列宽（字符）
     * @param summaryWidth 汇总列宽（字符）
     * @return 单元格写入处理器
     */
    public static CellWriteHandler createPivotTableColumnWidthHandler(
            PivotTableHeads heads,
            int fixedWidth,
            int dynamicWidth,
            int summaryWidth) {
        
        final int fixedCount = heads.getFixedColumnCount();
        final int dynamicCount = heads.getDynamicColumnCount();
        final boolean hasSummary = heads.getSummaryColumn() != null;
        final int summaryIndex = fixedCount + dynamicCount;
        
        return new CellWriteHandler() {
            @Override
            public void afterCellDispose(CellWriteHandlerContext context) {
                Cell cell = context.getCell();
                int columnIndex = cell.getColumnIndex();
                
                int width;
                if (columnIndex < fixedCount) {
                    width = fixedWidth;
                } else if (hasSummary && columnIndex == summaryIndex) {
                    width = summaryWidth;
                } else {
                    width = dynamicWidth;
                }
                
                cell.getSheet().setColumnWidth(columnIndex, width * 256);
            }
        };
    }

    /**
     * 应用WriteCellStyle到POI CellStyle
     *
     * @param cellStyle POI单元格样式
     * @param writeCellStyle EasyExcel写入样式
     * @param workbook 工作簿
     */
    private static void applyWriteCellStyle(CellStyle cellStyle, WriteCellStyle writeCellStyle,
                                            org.apache.poi.ss.usermodel.Workbook workbook) {
        // 背景色
        if (writeCellStyle.getFillForegroundColor() != null) {
            cellStyle.setFillForegroundColor(writeCellStyle.getFillForegroundColor());
            cellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        
        // 对齐
        if (writeCellStyle.getHorizontalAlignment() != null) {
            cellStyle.setAlignment(writeCellStyle.getHorizontalAlignment());
        }
        if (writeCellStyle.getVerticalAlignment() != null) {
            cellStyle.setVerticalAlignment(writeCellStyle.getVerticalAlignment());
        }
        
        // 边框
        if (writeCellStyle.getBorderTop() != null) {
            cellStyle.setBorderTop(writeCellStyle.getBorderTop());
        }
        if (writeCellStyle.getBorderBottom() != null) {
            cellStyle.setBorderBottom(writeCellStyle.getBorderBottom());
        }
        if (writeCellStyle.getBorderLeft() != null) {
            cellStyle.setBorderLeft(writeCellStyle.getBorderLeft());
        }
        if (writeCellStyle.getBorderRight() != null) {
            cellStyle.setBorderRight(writeCellStyle.getBorderRight());
        }
        
        // 字体
        if (writeCellStyle.getWriteFont() != null) {
            org.apache.poi.ss.usermodel.Font font = workbook.createFont();
            WriteFont writeFont = writeCellStyle.getWriteFont();
            
            if (writeFont.getFontName() != null) {
                font.setFontName(writeFont.getFontName());
            }
            if (writeFont.getFontHeightInPoints() != null) {
                font.setFontHeightInPoints(writeFont.getFontHeightInPoints());
            }
            if (writeFont.getBold() != null) {
                font.setBold(writeFont.getBold());
            }
            if (writeFont.getColor() != null) {
                font.setColor(writeFont.getColor());
            }
            
            cellStyle.setFont(font);
        }
    }
}
