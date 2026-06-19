package com.muasamcong.service.bidopening.impl;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.muasamcong.enums.RecordStatus;
import com.muasamcong.model.BidOpening;
import com.muasamcong.model.Bidding;
import com.muasamcong.model.BiddingContractor;
import com.muasamcong.model.Contract;
import com.muasamcong.model.ContractInfo;
import com.muasamcong.model.Contractor;
import com.muasamcong.model.Investor;
import com.muasamcong.model.ProcurementPlan;
import com.muasamcong.repository.BidOpeningRepository;
import com.muasamcong.repository.BiddingContractorRepository;
import com.muasamcong.repository.BiddingRepository;
import com.muasamcong.repository.ContractInfoRepository;
import com.muasamcong.repository.ContractRepository;
import com.muasamcong.service.bidopening.BidOpeningPdfService;
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BidOpeningPdfServiceImpl implements BidOpeningPdfService {
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormat MONEY_FORMAT = new DecimalFormat("#,##0", DecimalFormatSymbols.getInstance(Locale.GERMANY));
    private static final String DEFAULT_CONTRACT_EXECUTION_TIME = "Xem chi tiết tại mẫu tiến độ";

    private final ContractRepository contractRepository;
    private final ContractInfoRepository contractInfoRepository;
    private final BiddingRepository biddingRepository;
    private final BidOpeningRepository bidOpeningRepository;
    private final BiddingContractorRepository biddingContractorRepository;

    @Override
    @Transactional(readOnly = true)
    public byte[] renderByNotifyNo(String notifyNo) {
        Contract contract = contractRepository.findByNotifyNo(normalizeNotifyNo(notifyNo))
                .orElseThrow(() -> new IllegalArgumentException("Contract not found: " + notifyNo));
        ContractInfo info = contractInfoRepository.findByContractAndStatus(contract, RecordStatus.ACTIVE).orElse(null);
        Bidding bidding = biddingRepository.findByContract(contract).orElse(null);
        BidOpening bidOpening = bidOpeningRepository.findByContract(contract)
                .orElseThrow(() -> new IllegalArgumentException("Bid opening not found: " + notifyNo));
        List<BiddingContractor> bidders = biddingContractorRepository.findByBidOpeningWithContractor(bidOpening);

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4, 28, 28, 32, 28);
            PdfWriter.getInstance(document, out);
            document.open();

            Fonts fonts = fonts();
            Paragraph title = new Paragraph("BIÊN BẢN MỞ THẦU", fonts.title());
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(14);
            document.add(title);

            document.add(sectionTitle("Thông tin chung", fonts));
            PdfPTable general = keyValueTable(fonts);
            addRow(general, "Mã TBMT", contract.getNotifyNo(), fonts);
            addRow(general, "Tên gói thầu", value(info == null ? null : info.getBidName()), fonts);
            addRow(general, "Tên chủ đầu tư", investorName(info, contract), fonts);
            addRow(general, "Số lượng nhà thầu", String.valueOf(bidders.size()), fonts);
            addRow(general, "Thời điểm hoàn thành mở thầu", format(bidOpening.getCompletedAt()), fonts);
            addRow(general, "Lĩnh vực", investFieldLabel(info == null ? null : info.getInvestField()), fonts);
            document.add(general);

            document.add(sectionTitle("Thông tin gói thầu", fonts));
            PdfPTable packageInfo = keyValueTable(fonts);
            ProcurementPlan plan = contract.getProcurementPlan();
            addRow(packageInfo, "Mã KHLCNT", plan == null ? null : plan.getPlanNo(), fonts);
            addRow(packageInfo, "Tên kế hoạch LCNT", plan == null ? null : plan.getPlanName(), fonts);
            addRow(packageInfo, "Loại hợp đồng", contractTypeLabel(info == null ? null : info.getContractType()), fonts);
            addRow(packageInfo, "Thời gian thực hiện gói thầu", contractPeriod(info), fonts);
            addRow(packageInfo, "Hình thức lựa chọn nhà thầu", bidFormLabel(info == null ? null : info.getBidForm()), fonts);
            addRow(packageInfo, "Phương thức lựa chọn nhà thầu", bidModeLabel(info == null ? null : info.getBidMode()), fonts);
            addRow(packageInfo, "Thời điểm đóng mở thầu", bidding == null ? null : format(bidding.getBidOpenAt()), fonts);
            addRow(packageInfo, "Giá dự toán", money(info == null ? null : info.getBidEstimatePrice()), fonts);
            document.add(packageInfo);

            document.add(sectionTitle("Thông tin nhà thầu", fonts));
            document.add(bidderTable(bidders, fonts));

            document.close();
            return out.toByteArray();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot render bid opening PDF: " + ex.getMessage(), ex);
        }
    }

    private PdfPTable bidderTable(List<BiddingContractor> bidders, Fonts fonts) {
        PdfPTable table = new PdfPTable(new float[]{0.6f, 1.6f, 2.2f, 1.6f, 1.0f, 1.7f, 1.1f, 1.5f, 1.1f, 1.6f});
        table.setWidthPercentage(100);
        addHeader(table, "STT", fonts);
        addHeader(table, "Mã định danh", fonts);
        addHeader(table, "Tên nhà thầu", fonts);
        addHeader(table, "Giá dự thầu (VND)", fonts);
        addHeader(table, "Tỷ lệ giảm giá (%)", fonts);
        addHeader(table, "Giá dự thầu sau giảm giá (VND)", fonts);
        addHeader(table, "Hiệu lực E-HSDT (ngày)", fonts);
        addHeader(table, "Bảo đảm dự thầu (VND)", fonts);
        addHeader(table, "Hiệu lực BĐDT (ngày)", fonts);
        addHeader(table, "Thời gian thực hiện gói thầu", fonts);

        for (int i = 0; i < bidders.size(); i++) {
            BiddingContractor bidder = bidders.get(i);
            Contractor contractor = bidder.getContractor();
            addCell(table, String.valueOf(i + 1), fonts.body(), Element.ALIGN_CENTER);
            addCell(table, bidderIdentityCode(contractor), fonts.body(), Element.ALIGN_LEFT);
            addCell(table, contractor == null ? null : contractor.getContractorName(), fonts.body(), Element.ALIGN_LEFT);
            addCell(table, money(bidder.getBidPrice()), fonts.body(), Element.ALIGN_RIGHT);
            addCell(table, percentOrZero(bidder.getDiscountRate()), fonts.body(), Element.ALIGN_RIGHT);
            addCell(table, money(bidder.getBidPriceAfterDiscount()), fonts.body(), Element.ALIGN_RIGHT);
            addCell(table, value(bidder.getBidValidityPeriod()), fonts.body(), Element.ALIGN_CENTER);
            addCell(table, money(bidder.getBidGuaranteeValue()), fonts.body(), Element.ALIGN_RIGHT);
            addCell(table, value(bidder.getBidGuaranteeValidityPeriod()), fonts.body(), Element.ALIGN_CENTER);
            addCell(table, DEFAULT_CONTRACT_EXECUTION_TIME, fonts.body(), Element.ALIGN_LEFT);
        }
        return table;
    }

    private PdfPTable keyValueTable(Fonts fonts) {
        PdfPTable table = new PdfPTable(new float[]{1.6f, 6.4f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        return table;
    }

    private void addRow(PdfPTable table, String label, String value, Fonts fonts) {
        PdfPCell labelCell = cell(label, fonts.label(), Element.ALIGN_LEFT);
        labelCell.setBackgroundColor(new Color(230, 230, 230));
        table.addCell(labelCell);
        table.addCell(cell(value(value), fonts.body(), Element.ALIGN_LEFT));
    }

    private void addHeader(PdfPTable table, String value, Fonts fonts) {
        PdfPCell cell = cell(value, fonts.label(), Element.ALIGN_CENTER);
        cell.setBackgroundColor(new Color(230, 230, 230));
        table.addCell(cell);
    }

    private void addCell(PdfPTable table, String value, Font font, int alignment) {
        table.addCell(cell(value(value), font, alignment));
    }

    private PdfPCell cell(String value, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(value(value), font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(4);
        cell.setBorder(Rectangle.BOX);
        cell.setBorderWidth(0.6f);
        return cell;
    }

    private Paragraph sectionTitle(String value, Fonts fonts) {
        Paragraph paragraph = new Paragraph(value, fonts.label());
        paragraph.setSpacingBefore(2);
        paragraph.setSpacingAfter(2);
        return paragraph;
    }

    private String investorName(ContractInfo info, Contract contract) {
        Investor investor = info == null ? null : info.getInvestor();
        if (investor == null && contract.getProcurementPlan() != null) {
            investor = contract.getProcurementPlan().getInvestor();
        }
        return investor == null ? null : investor.getInvestorName();
    }

    private String contractPeriod(ContractInfo info) {
        if (info == null || info.getContractPeriod() == null) {
            return null;
        }
        return info.getContractPeriod() + " " + periodUnitLabel(info.getContractPeriodUnit());
    }

    private String contractTypeLabel(String value) {
        return switch (value(value)) {
            case "TG" -> "Trọn gói";
            case "DGCD" -> "Đơn giá cố định";
            case "DGDC" -> "Đơn giá điều chỉnh";
            default -> value(value);
        };
    }

    private String investFieldLabel(String value) {
        return switch (value(value)) {
            case "PTV" -> "Phi tư vấn";
            case "TV" -> "Tư vấn";
            case "HH" -> "Hàng hóa";
            case "XL" -> "Xây lắp";
            default -> value(value);
        };
    }

    private String periodUnitLabel(String value) {
        return switch (value(value)) {
            case "D" -> "ngày";
            case "M" -> "tháng";
            case "Y" -> "năm";
            default -> value(value);
        };
    }

    private String bidFormLabel(String value) {
        return switch (value(value)) {
            case "DTRR" -> "Đấu thầu rộng rãi";
            case "CHCT" -> "Chào hàng cạnh tranh";
            case "CDTRG" -> "Chỉ định thầu rút gọn";
            case "MSTT" -> "Mua sắm trực tiếp";
            default -> value(value);
        };
    }

    private String bidModeLabel(String value) {
        return switch (value(value)) {
            case "1_MTHS" -> "Một giai đoạn một túi hồ sơ";
            case "1_HTHS" -> "Một giai đoạn hai túi hồ sơ";
            case "2_MTHS" -> "Hai giai đoạn một túi hồ sơ";
            case "2_HTHS" -> "Hai giai đoạn hai túi hồ sơ";
            default -> value(value);
        };
    }

    private String firstTaxCode(Contractor contractor) {
        if (contractor == null || contractor.getTaxCodes() == null || contractor.getTaxCodes().isEmpty()) {
            return null;
        }
        return contractor.getTaxCodes().get(0);
    }

    private String bidderIdentityCode(Contractor contractor) {
        String taxCode = firstTaxCode(contractor);
        if (taxCode == null || taxCode.isBlank()) {
            return taxCode;
        }
        String normalized = taxCode.trim();
        return normalized.regionMatches(true, 0, "vn", 0, 2) ? normalized : "vn" + normalized;
    }

    private String format(OffsetDateTime value) {
        return value == null ? null : value.format(DATE_TIME_FORMAT);
    }

    private String money(Long value) {
        return value == null ? null : MONEY_FORMAT.format(value) + " VND";
    }

    private String percent(BigDecimal value) {
        return value == null ? null : value.stripTrailingZeros().toPlainString();
    }

    private String percentOrZero(BigDecimal value) {
        String percent = percent(value);
        return percent == null ? "0" : percent;
    }

    private String value(Integer value) {
        return value == null ? null : String.valueOf(value);
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private String normalizeNotifyNo(String notifyNo) {
        if (notifyNo == null || notifyNo.isBlank()) {
            throw new IllegalArgumentException("notifyNo is required");
        }
        return notifyNo.trim();
    }

    private Fonts fonts() throws Exception {
        BaseFont baseFont;
        try {
            baseFont = BaseFont.createFont("C:/Windows/Fonts/arial.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
        } catch (Exception ex) {
            baseFont = BaseFont.createFont(BaseFont.HELVETICA, BaseFont.CP1252, BaseFont.NOT_EMBEDDED);
        }
        return new Fonts(
                new Font(baseFont, 12, Font.BOLD),
                new Font(baseFont, 8, Font.BOLD),
                new Font(baseFont, 8)
        );
    }

    private record Fonts(Font title, Font label, Font body) {
    }
}
