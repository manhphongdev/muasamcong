package com.muasamcong.service.biddingresult.impl;

import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.BiddingResultGoods;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.repository.BiddingResultGoodsRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.service.biddingresult.BiddingResultGoodsExcelService;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BiddingResultGoodsExcelServiceImpl implements BiddingResultGoodsExcelService {
    private static final String SHEET_NAME = "Du thau hang hoa";

    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BiddingResultGoodsRepository goodsRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportByNotifyNo(String notifyNo) {
        String normalizedNotifyNo = normalizeNotifyNo(notifyNo);
        Contract contract = contractRepository.findByNotifyNo(normalizedNotifyNo)
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + normalizedNotifyNo));
        ContractInfo info = contractInfoRepository.findByContractAndStatus(contract, RecordStatus.ACTIVE).orElse(null);
        List<BiddingResultGoods> goods = goodsRepository.findByNotifyNoOrderBySortOrderAsc(normalizedNotifyNo);
        if (goods.isEmpty()) {
            goods = goodsRepository.findByContractOrderBySortOrderAsc(contract);
        }

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            var sheet = workbook.createSheet(SHEET_NAME);
            Styles styles = createStyles(workbook);

            int rowIndex = 0;
            Row titleRow = sheet.createRow(rowIndex++);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("THÔNG TIN CHUNG");
            titleCell.setCellStyle(styles.sectionTitle());
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 13));

            Row notifyRow = sheet.createRow(rowIndex++);
            addInfoRow(notifyRow, "Mã TBMT", normalizedNotifyNo, styles);
            Row bidNameRow = sheet.createRow(rowIndex++);
            addInfoRow(bidNameRow, "Tên gói thầu", firstNonBlank(firstBidName(goods), info == null ? null : info.getBidName()), styles);

            Row header = sheet.createRow(rowIndex++);
            String[] headers = {
                    "STT",
                    "Danh mục hàng hóa",
                    "Ký mã hiệu",
                    "Nhãn hiệu",
                    "Năm sản xuất",
                    "Xuất xứ (quốc gia, vùng lãnh thổ sản xuất)",
                    "Hãng sản xuất",
                    "Cấu hình, tính năng kỹ thuật cơ bản",
                    "Đơn vị tính",
                    "Khối lượng",
                    "Mã HS",
                    "Đơn giá trúng thầu",
                    "Thành tiền đã bao gồm thuế, phí, lệ phí (nếu có)",
                    "Thời gian giao hàng (ngày)/Tiến độ cung cấp"
            };
            for (int i = 0; i < headers.length; i++) {
                Cell cell = header.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(styles.header());
            }

            long totalAmount = 0L;
            for (int i = 0; i < goods.size(); i++) {
                BiddingResultGoods item = goods.get(i);
                Row row = sheet.createRow(rowIndex++);
                int col = 0;
                addCell(row, col++, i + 1, styles.bodyCenter());
                addCell(row, col++, item.getGoodsName(), styles.body());
                addCell(row, col++, item.getGoodsCode(), styles.body());
                addCell(row, col++, item.getGoodsLabel(), styles.body());
                addCell(row, col++, item.getYearManufacture(), styles.body());
                addCell(row, col++, item.getOrigin(), styles.body());
                addCell(row, col++, item.getManufacturer(), styles.body());
                addCell(row, col++, item.getTechnicalFeatures(), styles.body());
                addCell(row, col++, item.getUnit(), styles.body());
                addCell(row, col++, item.getQuantity(), styles.bodyNumber());
                addCell(row, col++, item.getHsCode(), styles.body());
                addCell(row, col++, item.getWinningUnitPrice(), styles.money());
                addCell(row, col++, item.getAmount(), styles.money());
                addCell(row, col, item.getDeliveryTime(), styles.body());
                if (item.getAmount() != null) {
                    totalAmount += item.getAmount();
                }
            }

            Row totalRow = sheet.createRow(rowIndex);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Tổng cộng giá dự thầu của hàng hóa đã bao gồm thuế, phí, lệ phí (nếu có)");
            totalLabel.setCellStyle(styles.total());
            sheet.addMergedRegion(new CellRangeAddress(rowIndex, rowIndex, 0, 11));
            for (int i = 1; i <= 11; i++) {
                totalRow.createCell(i).setCellStyle(styles.total());
            }
            addCell(totalRow, 12, totalAmount, styles.moneyTotal());
            totalRow.createCell(13).setCellStyle(styles.total());

            int[] widths = {8, 28, 20, 20, 16, 28, 24, 42, 14, 14, 14, 20, 28, 26};
            for (int i = 0; i < widths.length; i++) {
                sheet.setColumnWidth(i, widths[i] * 256);
            }
            sheet.createFreezePane(0, 4);

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot export bidding result goods Excel: " + ex.getMessage(), ex);
        }
    }

    private void addInfoRow(Row row, String label, String value, Styles styles) {
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        labelCell.setCellStyle(styles.infoLabel());
        Cell valueCell = row.createCell(1);
        valueCell.setCellValue(value(value));
        valueCell.setCellStyle(styles.infoValue());
        row.getSheet().addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 1, 13));
    }

    private void addCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value(value));
        cell.setCellStyle(style);
    }

    private void addCell(Row row, int col, Integer value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void addCell(Row row, int col, Long value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        cell.setCellStyle(style);
    }

    private void addCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private Styles createStyles(Workbook workbook) {
        Font bold = workbook.createFont();
        bold.setBold(true);
        Font normal = workbook.createFont();

        XSSFCellStyle sectionTitle = style(workbook, bold, HorizontalAlignment.LEFT, false);
        XSSFCellStyle infoLabel = style(workbook, normal, HorizontalAlignment.LEFT, false);
        XSSFCellStyle infoValue = style(workbook, normal, HorizontalAlignment.LEFT, false);
        XSSFCellStyle header = style(workbook, bold, HorizontalAlignment.CENTER, true);
        header.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        header.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        XSSFCellStyle body = style(workbook, normal, HorizontalAlignment.LEFT, true);
        XSSFCellStyle bodyCenter = style(workbook, normal, HorizontalAlignment.CENTER, true);
        XSSFCellStyle bodyNumber = style(workbook, normal, HorizontalAlignment.RIGHT, true);
        bodyNumber.setDataFormat(workbook.createDataFormat().getFormat("#,##0.####"));
        XSSFCellStyle money = style(workbook, normal, HorizontalAlignment.RIGHT, true);
        money.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        XSSFCellStyle total = style(workbook, bold, HorizontalAlignment.CENTER, true);
        XSSFCellStyle moneyTotal = style(workbook, bold, HorizontalAlignment.RIGHT, true);
        moneyTotal.setDataFormat(workbook.createDataFormat().getFormat("#,##0"));
        return new Styles(sectionTitle, infoLabel, infoValue, header, body, bodyCenter, bodyNumber, money, total, moneyTotal);
    }

    private XSSFCellStyle style(Workbook workbook, Font font, HorizontalAlignment alignment, boolean border) {
        XSSFCellStyle style = (XSSFCellStyle) workbook.createCellStyle();
        style.setFont(font);
        style.setAlignment(alignment);
        style.setVerticalAlignment(VerticalAlignment.CENTER);
        style.setWrapText(true);
        if (border) {
            style.setBorderTop(BorderStyle.THIN);
            style.setBorderBottom(BorderStyle.THIN);
            style.setBorderLeft(BorderStyle.THIN);
            style.setBorderRight(BorderStyle.THIN);
        }
        return style;
    }

    private String firstBidName(List<BiddingResultGoods> goods) {
        return goods.stream()
                .map(BiddingResultGoods::getBidName)
                .filter(value -> value != null && !value.isBlank())
                .findFirst()
                .orElse(null);
    }

    private String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private String value(String value) {
        return value == null ? "" : value;
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }

    private record Styles(
            CellStyle sectionTitle,
            CellStyle infoLabel,
            CellStyle infoValue,
            CellStyle header,
            CellStyle body,
            CellStyle bodyCenter,
            CellStyle bodyNumber,
            CellStyle money,
            CellStyle total,
            CellStyle moneyTotal
    ) {
    }
}
