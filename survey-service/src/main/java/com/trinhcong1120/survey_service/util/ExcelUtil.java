package com.trinhcong1120.survey_service.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public final class ExcelUtil {

  private ExcelUtil() {
  }

  public static Workbook createWorkbook() {
    return new XSSFWorkbook();
  }

  public static Sheet createSheet(
          Workbook workbook,
          String name
  ) {
    return workbook.createSheet(name);
  }

  public static CellStyle createHeaderStyle(
          Workbook workbook
  ) {

    CellStyle style =
            workbook.createCellStyle();

    Font font =
            workbook.createFont();

    font.setBold(true);

    style.setFont(font);

    style.setAlignment(
            HorizontalAlignment.CENTER
    );

    style.setVerticalAlignment(
            VerticalAlignment.CENTER
    );

    style.setWrapText(true);

    return style;
  }

  public static CellStyle createWrapStyle(
          Workbook workbook
  ) {

    CellStyle style =
            workbook.createCellStyle();

    style.setVerticalAlignment(
            VerticalAlignment.TOP
    );

    style.setWrapText(true);

    return style;
  }

  public static void createHeader(
          Sheet sheet,
          CellStyle headerStyle,
          String... headers
  ) {

    Row row = sheet.createRow(0);

    for (int i = 0;
         i < headers.length;
         i++) {

      Cell cell =
              row.createCell(i);

      cell.setCellValue(
              headers[i]
      );

      cell.setCellStyle(
              headerStyle
      );
    }
  }

  public static Cell createCell(
          Row row,
          int column,
          String value
  ) {

    Cell cell =
            row.createCell(column);

    cell.setCellValue(
            value == null
                    ? ""
                    : value
    );

    return cell;
  }

  public static Cell createCell(
          Row row,
          int column,
          Number value
  ) {

    Cell cell =
            row.createCell(column);

    if (value != null) {
      cell.setCellValue(
              value.doubleValue()
      );
    }

    return cell;
  }

  public static Cell createCell(
          Row row,
          int column,
          Boolean value
  ) {

    Cell cell =
            row.createCell(column);

    if (value != null) {
      cell.setCellValue(value);
    }

    return cell;
  }

  public static void autoSizeColumns(
          Sheet sheet,
          int columnCount
  ) {

    for (int i = 0;
         i < columnCount;
         i++) {

      sheet.autoSizeColumn(i);

      /*
       * Không cho column quá rộng.
       */
      int currentWidth =
              sheet.getColumnWidth(i);

      int maxWidth =
              50 * 256;

      if (currentWidth > maxWidth) {
        sheet.setColumnWidth(
                i,
                maxWidth
        );
      }
    }
  }

  public static void setColumnWidth(
          Sheet sheet,
          int column,
          int characters
  ) {

    int width =
            Math.min(
                    characters * 256,
                    255 * 256
            );

    sheet.setColumnWidth(
            column,
            width
    );
  }

  public static byte[] toByteArray(
          Workbook workbook
  ) {

    try (
            ByteArrayOutputStream output =
                    new ByteArrayOutputStream()
    ) {

      workbook.write(output);

      return output.toByteArray();

    } catch (IOException e) {

      throw new RuntimeException(
              "Không thể tạo file Excel",
              e
      );

    } finally {

      try {
        workbook.close();
      } catch (IOException ignored) {
      }
    }
  }
}