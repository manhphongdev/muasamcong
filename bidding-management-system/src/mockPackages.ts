/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { PackageData, ApiStageName, ApiState, PackageLifecycle, ScrapeStatus } from './types';

// Helper to offset time from now in ISO format
const getOffsetTimeISO = (hoursOffset: number): string => {
  const d = new Date();
  d.setMilliseconds(0);
  d.setSeconds(0);
  d.setTime(d.getTime() + hoursOffset * 60 * 60 * 1000);
  return d.toISOString();
};

export const mockPackages: PackageData[] = [
  {
    notifyNo: 'IB2600047379',
    title: 'Gói thầu số 02: Thiết kế thi công hệ thống xử lý nước thải công suất 500m3/ngày đêm',
    investor: 'Ban quản lý các KCN tỉnh Đồng Nai',
    lifecycle: 'Đã đóng thầu',
    scrapeStatus: 'PARTIAL',
    budget: 8520000000,
    publishDate: '15/05/2026',
    closeTime: getOffsetTimeISO(-4), // Đóng 4 giờ trước
    openTime: getOffsetTimeISO(-3.5),
    folderPath: '\\\\192.168.1.150\\Public_MSC_Folder\\Water_DongNai_02',
    folderExists: true,
    completenessPercent: 70,
    downloadCompletenessPercent: 60,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Missing', // Chưa có KQLCNT
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'bidValidityDays',
        source: 'BaoCaoOpenDetail (BBMT)',
        reason: 'API BBMT chưa trả field hoặc nằm trong bidOpenView chưa parse.',
        suggestion: 'Kiểm tra lại tệp BBMT gốc hoặc cập nhật thủ công.'
      },
      {
        field: 'inputResultId',
        source: 'KQLCNT API',
        reason: 'Gói chưa có KQLCNT.',
        suggestion: 'Chờ hội đồng phê duyệt kết quả và đồng bộ lại vào ngày mai.'
      }
    ],
    bidders: [
      {
        no: 1,
        taxCode: '0312456789',
        name: 'Công ty Cổ phần Môi trường xanh Đồng Nai',
        bidPrice: 8400000000,
        discountRate: 2,
        finalPrice: 8232000000,
        bidValidityDays: null, // Thiếu hiệu lực E-HSDT (Mandatory case 1)
        bidSecurity: '120,000,000 VND',
        securityValidityDays: 120,
        executionTimeText: '180 ngày'
      },
      {
        no: 2,
        taxCode: '0101122334',
        name: 'Tổng công ty nước và xây dựng công nghiệp Việt Nam (WASECO)',
        bidPrice: 8310000000,
        discountRate: 0,
        finalPrice: 8310000000,
        bidValidityDays: null, // Thiếu hiệu lực E-HSDT (Mandatory case 1)
        bidSecurity: '120,000,000 VND',
        securityValidityDays: 120,
        executionTimeText: '150 ngày'
      }
    ],
    logs: [
      { timestamp: '2026-06-13 09:12:10', level: 'INFO', stage: 'Search', message: 'Tìm thấy gói thầu IB2600047379 trên luồng tìm kiếm.' },
      { timestamp: '2026-06-13 09:12:12', level: 'INFO', stage: 'TBMT', message: 'Tải thành công metadata TBMT. Giá dự toán 8,520,000,000 VND.' },
      { timestamp: '2026-06-13 09:12:15', level: 'INFO', stage: 'HSMT', message: 'Tệp HSMT (ZIP, PDF) được liên kết thành công dưới dạng fileId 487192.' },
      { timestamp: '2026-06-13 09:12:18', level: 'INFO', stage: 'BBMT', message: 'Tải thành công biên bản mở thầu từ API BaoCaoLotOpen.' },
      { timestamp: '2026-06-13 09:12:19', level: 'WARN', stage: 'BBMT', message: 'Trường bidValidityDays (Hiệu lực E-HSDT) trả về rỗng từ API BaoCaoOpenDetail.' },
      { timestamp: '2026-06-13 09:12:21', level: 'WARN', stage: 'KQLCNT', message: 'Không phát hiện Quyết định phê duyệt Trúng thầu (Chưa được chủ đầu tư công bố).' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-13 09:12:21',
    lastMessage: 'Đã hoàn thành sync một phần (Chưa có kết quả chọn nhà thầu)'
  },
  {
    notifyNo: 'IB2600026487',
    title: 'Cung cấp trang chủ thiết bị công nghệ thông tin và máy trạm chuyên dụng hệ thống điều khiển giao thông đô thị',
    investor: 'Sở Giao thông Vận tải Hà Nội',
    lifecycle: 'Đã mở thầu',
    scrapeStatus: 'COMPLETED',
    budget: 4500000000,
    publishDate: '10/05/2026',
    closeTime: getOffsetTimeISO(-48),
    openTime: getOffsetTimeISO(-47.5),
    folderPath: 'D:/DuAn_IT_Hanoi/GoiIT_MayTram_02',
    folderExists: true,
    completenessPercent: 85,
    downloadCompletenessPercent: 90,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Not_Applicable',
      Contract: 'Not_Applicable'
    },
    missingFields: [
      {
        field: 'bidSecurity',
        source: 'BaoCaoOpenDetail',
        reason: 'Bảo đảm dự thầu chỉ có ở 1 nhà thầu (3 nhà thầu còn lại ghi bảo lãnh không kèm file xác nhận hoặc null).',
        suggestion: 'Kiểm tra thủ công tệp nộp thầu của nhà thầu số 2, 3, 4 xem có bảo lãnh ngân hàng bản cứng không.'
      }
    ],
    bidders: [
      {
        no: 1,
        taxCode: '0102049911',
        name: 'Công ty TNHH Giải pháp Phần cứng & Máy tính Sao Việt',
        bidPrice: 4200000000,
        discountRate: 0,
        finalPrice: 4200000000,
        bidValidityDays: 90,
        bidSecurity: '90,000,000 VND (Có hiệu lực)', // Chỉ bên này có bảo đảm thầu (Mandatory case 2)
        securityValidityDays: 120,
        executionTimeText: '90 ngày'
      },
      {
        no: 2,
        taxCode: '0310228833',
        name: 'Công ty Cổ phần Đầu tư và Phát triển Công nghệ Việt',
        bidPrice: 4320000000,
        discountRate: 2,
        finalPrice: 4233600000,
        bidValidityDays: 90,
        bidSecurity: 'Không cam kết nộp thầu điện tử', // Incomplete (Mandatory case 2)
        securityValidityDays: null,
        executionTimeText: '60 ngày'
      },
      {
        no: 3,
        taxCode: '3700123456',
        name: 'Tập đoàn Công nghệ HiPT',
        bidPrice: 4410000000,
        discountRate: 0,
        finalPrice: 4410000000,
        bidValidityDays: 90,
        bidSecurity: 'Trống', // Incomplete (Mandatory case 2)
        securityValidityDays: null,
        executionTimeText: '75 ngày'
      },
      {
        no: 4,
        taxCode: '0100998877',
        name: 'Công ty Cổ phần tích hợp hệ thống CMC',
        bidPrice: 4150000000,
        discountRate: 0,
        finalPrice: 4150000000,
        bidValidityDays: 90,
        bidSecurity: 'Trống', // Incomplete (Mandatory case 2)
        securityValidityDays: null,
        executionTimeText: '90 ngày'
      }
    ],
    logs: [
      { timestamp: '2026-06-12 10:00:00', level: 'INFO', stage: 'Search', message: 'Bắt đầu quét đồng bộ thủ công do người dùng yêu cầu.' },
      { timestamp: '2026-06-12 10:00:02', level: 'INFO', stage: 'TBMT', message: 'Lấy dữ liệu TBMT thành công. Giá dự toán 4.5 tỷ.' },
      { timestamp: '2026-06-12 10:00:05', level: 'INFO', stage: 'BBMT', message: 'Tải BBMT thành công. Có 4 nhà thầu tham dự.' },
      { timestamp: '2026-06-12 10:00:06', level: 'WARN', stage: 'BBMT', message: 'Cảnh báo: Phát hiện 3/4 nhà thầu không có hoặc bị trống dữ liệu Bảo đảm dự thầu trong file JSON trả về.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-12 10:00:06',
    lastMessage: 'Đồng bộ hoàn tất thành công với cảnh báo thiếu bảo đảm thầu'
  },
  {
    notifyNo: 'IB2600099112',
    title: 'Mua sắm nâng cấp hệ thống camera giao thông kết hợp AI nhận diện biển số khu vực nội thành thành phố Thủ Đức',
    investor: 'Công an Thành phố Thủ Đức, TP.HCM',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 12400000000,
    publishDate: '11/06/2026',
    closeTime: getOffsetTimeISO(12), // Còn 12 giờ nữa đóng thầu (Mandatory case 3 - Cảnh báo đỏ)
    openTime: getOffsetTimeISO(12.5),
    folderPath: 'E:/CongAn_ThuDuc/Cam_AI_Traffic_2026',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped', // Chưa đóng thầu nên skipped
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [],
    logs: [
      { timestamp: '2026-06-12 23:45:00', level: 'INFO', stage: 'Search', message: 'Hệ thống tự động phát hiện gói thầu mới đăng tải.' },
      { timestamp: '2026-06-13 00:00:01', level: 'INFO', stage: 'TBMT', message: 'Scrape thành công thông tin mời thầu. Ngày đóng thầu: 13/06/2026 late.' },
      { timestamp: '2026-06-13 01:15:30', level: 'INFO', stage: 'HSMT', message: 'Tải thành công toàn bộ hồ sơ thiết kế, dự toán đính kèm.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-13 01:15:32',
    lastMessage: 'Đang mời thầu - Toàn bộ tệp thiết kế đã tải về sẵn sàng'
  },
  {
    notifyNo: 'IB2600010492',
    title: 'Trang bị văn phòng phẩm, bàn ghế phòng họp dự toán chi thường xuyên Đợt 2',
    investor: 'Phòng Giáo dục và Đào tạo Quận Bình Thạnh',
    lifecycle: 'Đã đóng thầu',
    scrapeStatus: 'PARTIAL',
    budget: 720000000,
    publishDate: '01/06/2026',
    closeTime: getOffsetTimeISO(-2), // Đã đóng 2 tiếng trước
    openTime: getOffsetTimeISO(-1.5),
    folderPath: 'C:/Văn phòng phẩm/Bình Thạnh 2026',
    folderExists: true,
    completenessPercent: 40,
    downloadCompletenessPercent: 0,
    biddingType: 'Chào hàng cạnh tranh',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Not_Applicable',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Failed', // Đã đóng thầu nhưng chưa có BBMT (Mandatory case 4)
      BaoCaoOpenDetail: 'Failed',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'bidOpenId',
        source: 'BaoCaoLotOpen API',
        reason: 'Đã đóng thầu nhưng chưa có BBMT.',
        suggestion: 'Hội đồng đấu thầu chưa mở hồ sơ trên hệ thống, vui lòng chạy lại Sync sau 30 phút.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 03:00:00', level: 'INFO', stage: 'Search', message: 'Bắt đầu kiểm tra thời hạn thầu tự động.' },
      { timestamp: '2026-06-13 03:00:02', level: 'INFO', stage: 'TBMT', message: 'Xác nhận thầu đã đóng vào lúc: 13/06/2026 03:00.' },
      { timestamp: '2026-06-13 03:05:00', level: 'ERROR', stage: 'BBMT', message: 'Gặp lỗi khi tìm kiếm mã thầu mở rộng. Lý do: Biên bản mở thầu chưa xuất hiện trên MSC cổng thông tin.' }
    ],
    retryCount: 2,
    lastSyncTime: '2026-06-13 03:05:00',
    lastMessage: 'Lỗi đồng bộ hồ sơ mở thầu (Biên bản mở thầu chưa phát hành)'
  },
  {
    notifyNo: 'IB2600035541',
    title: 'Gói thầu số 5: Xây dựng đường gom kết nối hạ tầng giao thông khu công nghiệp công nghệ cao Amata Long Thành',
    investor: 'Ban quản lý dự án công trình giao thông tỉnh Đồng Nai',
    lifecycle: 'Có KQLCNT', // Có kết quả chọn nhà thầu nhưng chưa có hợp đồng
    scrapeStatus: 'COMPLETED',
    budget: 32000000000,
    publishDate: '20/04/2026',
    closeTime: getOffsetTimeISO(-300),
    openTime: getOffsetTimeISO(-299),
    folderPath: 'D:/DongNai_Projects/Amata_Highway_05',
    folderExists: true,
    completenessPercent: 90,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Success',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success', // Có KQLCNT (Mandatory case 5)
      Contract: 'Missing' // chưa có hợp đồng
    },
    missingFields: [
      {
        field: 'contractInfo',
        source: 'Contract API',
        reason: 'Gói thầu đã công bố kết quả trúng thầu nhưng chưa được chủ đầu tư upload thông tin hợp đồng thương thảo.',
        suggestion: 'Theo dõi hợp đồng định kỳ mỗi tuần.'
      }
    ],
    bidders: [
      {
        no: 1,
        taxCode: '0314561234',
        name: 'Tổng công ty 319 - Bộ Quốc Phòng',
        bidPrice: 31850000000,
        discountRate: 0.5,
        finalPrice: 31690750000,
        bidValidityDays: 120,
        bidSecurity: '500,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '210 ngày'
      },
      {
        no: 2,
        taxCode: '0100481923',
        name: 'Công ty Cổ phần Tập đoàn Đèo Cả',
        bidPrice: 31500000000,
        discountRate: 0,
        finalPrice: 31500000000,
        bidValidityDays: 120,
        bidSecurity: '500,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '180 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Tập đoàn Đèo Cả',
    winningPrice: 31500000000,
    logs: [
      { timestamp: '2026-06-10 14:00:20', level: 'INFO', stage: 'Search', message: 'Bắt đầu tiến trình kiểm tra sau mở thầu.' },
      { timestamp: '2026-06-10 14:00:22', level: 'INFO', stage: 'KQLCNT', message: 'Phát hiện quyết định trúng thầu số 491/QD-BQL. Đơn vị trúng: Tập đoàn Đèo Cả. Giá trúng thầu: 31,500,000,000 VND.' },
      { timestamp: '2026-06-10 14:00:25', level: 'WARN', stage: 'Contract', message: 'API hợp đồng báo trạng thái NULL. Chưa ký kết hợp đồng thương thảo chính thức.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-10 14:00:25',
    lastMessage: 'Đã cập nhật kết quả trúng thầu thành công, đang chờ ký kết hợp đồng'
  },
  {
    notifyNo: 'IB2600022199',
    title: 'Mua sắm thiết bị khoa y, hóa chất phân tích xét nghiệm máy ly tâm kỹ thuật cao phục vụ công tác nghiên cứu phòng thí nghiệm sinh học',
    investor: 'Đại học Quốc gia TP.HCM',
    lifecycle: 'Có thông tin hợp đồng', // Có hợp đồng ký kết chính thức (Mandatory case 6)
    scrapeStatus: 'COMPLETED',
    budget: 9350000000,
    publishDate: '01/04/2026',
    closeTime: getOffsetTimeISO(-1200),
    openTime: getOffsetTimeISO(-1199.5),
    folderPath: 'D:/Lab_VNU/LyTam_Biotech_2026',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Success',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success',
      Contract: 'Success'
    },
    missingFields: [],
    bidders: [
      {
        no: 1,
        taxCode: '0310248591',
        name: 'Công ty Cổ phần Thiết bị Khoa học và Công nghệ Alpha',
        bidPrice: 9120000000,
        discountRate: 1,
        finalPrice: 9028800000,
        bidValidityDays: 90,
        bidSecurity: '150,000,000 VND',
        securityValidityDays: 120,
        executionTimeText: '60 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Thiết bị Khoa học và Công nghệ Alpha',
    winningPrice: 9028800000,
    contractSignDate: '15/05/2026',
    logs: [
      { timestamp: '2026-06-01 10:20:10', level: 'INFO', stage: 'Search', message: 'Kiểm tra trạng thái hợp đồng hàng tuần.' },
      { timestamp: '2026-06-01 10:20:14', level: 'INFO', stage: 'Contract', message: 'Tải thành công tệp hợp đồng đã ký đóng dấu điện tử. Số hợp đồng: 124/HDBK-VNU.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-01 10:20:14',
    lastMessage: 'Đồng bộ hoàn tất thành công - Có thông tin hợp đồng trọn gói'
  },
  {
    notifyNo: 'IB2600088190',
    title: 'Xây dựng tuyến đường bê tông nông thôn liên xã Bình Mỹ - Tân Bình huyện Bắc Tân Uyên',
    investor: 'Ủy ban nhân dân huyện Bắc Tân Uyên, Bình Dương',
    lifecycle: 'Không xác định',
    scrapeStatus: 'COMPLETED',
    budget: 6200000000,
    publishDate: '14/04/2026',
    closeTime: getOffsetTimeISO(-800),
    openTime: getOffsetTimeISO(-799),
    folderPath: null,
    folderExists: false,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Not_Applicable',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [],
    logs: [
      { timestamp: '2026-05-10 11:30:00', level: 'INFO', stage: 'Search', message: 'Quét trạng thái định kỳ.' },
      { timestamp: '2026-05-10 11:30:04', level: 'WARN', stage: 'TBMT', message: 'Phát hiện quyết định hủy thầu số 891/QD-UBND do điều chỉnh quy hoạch xây dựng địa phương.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-05-10 11:30:04',
    lastMessage: 'Trạng thái gói thầu chưa xác định'
  },
  {
    notifyNo: 'IBXXXXINVALID',
    title: 'Gói thầu lỗi thử nghiệm nhập liệu mã số thông báo mời thầu sai định dạng quy chuẩn',
    investor: 'Nhà đầu tư Thử nghiệm',
    lifecycle: 'Không xác định',
    scrapeStatus: 'FAILED', // Lỗi Search do notifyNo sai (Mandatory case 8)
    budget: 150000000,
    publishDate: '01/06/2026',
    closeTime: getOffsetTimeISO(200),
    openTime: getOffsetTimeISO(200.5),
    folderPath: null,
    folderExists: false,
    completenessPercent: 10,
    downloadCompletenessPercent: 0,
    biddingType: 'Mua sắm trực tiếp',
    apiCoverage: {
      Search: 'Failed',
      TBMT: 'Skipped',
      HSMT: 'Skipped',
      Clarification: 'Skipped',
      Petition: 'Skipped',
      Conference: 'Skipped',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'notifyId',
        source: 'Search API',
        reason: 'Sai định dạng mã thông báo mời thầu (notifyNo must start with IB followed by numbers).',
        suggestion: 'Chỉnh sửa lại số TBMT chính xác trong phần quản trị hàng đợi.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 04:00:00', level: 'ERROR', stage: 'Search', message: 'Không thể phân tích hoặc tìm kiếm số TBMT: IBXXXXINVALID. Định dạng không được hệ thống hỗ trợ.' }
    ],
    retryCount: 5,
    lastSyncTime: '2026-06-13 04:00:00',
    lastMessage: 'Lỗi đồng bộ: Mã TBMT không tồn tại hoặc sai định dạng'
  },
  {
    notifyNo: 'IB2600055231',
    title: 'Thi công xây lắp nâng cấp cơ sở hạ tầng mạng điện đường và hệ thống thoát nước liên xã Thạnh Mỹ Tây',
    investor: 'Hội đồng nhân dân huyện Châu Phú, An Giang',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 1250000000,
    publishDate: '01/06/2026',
    closeTime: getOffsetTimeISO(35), // Còn 35 giờ nữa
    openTime: getOffsetTimeISO(35.5),
    folderPath: 'Z:/Local_Server_Backup/AnGiang_Project_ThanhMy', // Folder không tồn tại (Mandatory case 9)
    folderExists: false,
    completenessPercent: 95,
    downloadCompletenessPercent: 0,
    biddingType: 'Đấu thầu rộng rãi',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Not_Applicable',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'folderPath',
        source: 'Local Storage Manager',
        reason: 'folderPath: thư mục đích Z:/Local_Server_Backup/AnGiang_Project_ThanhMy không tồn tại trên hệ thống máy trạm hoặc máy chủ lưu trữ.',
        suggestion: 'Tạo thư mục trước hoặc cấu hình lại đường dẫn lưu trữ ở màn thêm gói thầu.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 02:30:10', level: 'INFO', stage: 'Search', message: 'Quét thành công thông tin TBMT.' },
      { timestamp: '2026-06-13 02:30:12', level: 'WARN', stage: 'Local Folder Check', message: 'Cảnh báo: Đường dẫn cục bộ "Z:/Local_Server_Backup/AnGiang_Project_ThanhMy" không thể kết nối hoặc không tìm thấy thư mục.' }
    ],
    retryCount: 1,
    lastSyncTime: '2026-06-13 02:30:12',
    lastMessage: 'Đồng bộ thầu thành công nhưng lỗi thư mục đích lưu trữ file'
  },
  {
    notifyNo: 'IB2600067431',
    title: 'Cung cấp linh kiện thay thế Máy chụp cắt lớp vi tính CT Scanner đa dãy đầu thu Bệnh viện Đa khoa tỉnh Bình Định',
    investor: 'Bệnh viện Đa khoa tỉnh Bình Định',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 3100000000,
    publishDate: '08/06/2026',
    closeTime: getOffsetTimeISO(48),
    openTime: getOffsetTimeISO(48.5),
    folderPath: '\\\\192.168.1.150\\Public_MSC_Folder\\BinhDinh_CTScanner',
    folderExists: true,
    completenessPercent: 80,
    downloadCompletenessPercent: 45, // Tải lỗi 1 số file (Mandatory case 10)
    biddingType: 'Mua sắm trực tiếp qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'fileRefs',
        source: 'File Downloader Process',
        reason: 'Tải xuống tệp "CT_Scanner_Technical_Spec_Part_2.pdf" thất bại liên tục (HTTP 502 Bad Gateway từ máy chủ lưu trữ cổng đấu thầu quốc gia).',
        suggestion: 'Mở lại hàng đợi tải file và bấm thử lại thủ công.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-12 18:00:20', level: 'INFO', stage: 'Search', message: 'Kiểm tra tệp tin thầu.' },
      { timestamp: '2026-06-12 18:01:05', level: 'ERROR', stage: 'Download', message: 'Tệp CT_Scanner_Technical_Spec_Part_2.pdf: Tải thất bại. Code 502, máy chủ MSC quá tải.' }
    ],
    retryCount: 3,
    lastSyncTime: '2026-06-12 18:01:05',
    lastMessage: 'Đồng bộ metadata thành công nhưng tải file đính kèm gặp lỗi'
  },
  {
    notifyNo: 'IB2600021487',
    title: 'Xây dựng cải tạo nâng cấp Trụ sở Huyện ủy và khối các cơ quan đoàn thể hành chính quận Bình Thạnh năm 2026',
    investor: 'Ban Quản lý Dự án Đầu tư Xây dựng khu vực quận Bình Thạnh',
    lifecycle: 'Đã đóng thầu',
    scrapeStatus: 'PARTIAL', // Đồng bộ 1 phần do thiếu KQLCNT (Mandatory case 11)
    budget: 15400000000,
    publishDate: '10/05/2026',
    closeTime: getOffsetTimeISO(-120), // Đã đóng thầu 5 ngày trước
    openTime: getOffsetTimeISO(-119.5),
    folderPath: 'D:/BinhThanh_Projects/Tru_So_HuyenUy_2026',
    folderExists: true,
    completenessPercent: 75,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Missing', // Không tìm thấy KQLCNT do chủ thầu chưa công bố
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'inputResultId',
        source: 'KQLCNT API',
        reason: 'Chưa có kết quả phê duyệt LCNT mặc dù đã hoàn tất chấm thầu sơ bộ.',
        suggestion: 'Hệ thống tự động thực hiện scan lại mỗi 12 tiếng để dò kết quả.'
      }
    ],
    bidders: [
      {
        no: 1,
        taxCode: '0314412233',
        name: 'Công ty Cổ phần Xây dựng và Phát triển Hạ tầng đô thị Việt Nam',
        bidPrice: 15100000000,
        discountRate: 0,
        finalPrice: 15100000000,
        bidValidityDays: 120,
        bidSecurity: '250,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '180 ngày'
      },
      {
        no: 2,
        taxCode: '0100223591',
        name: 'Tổng công ty Sông Đà - CTCP',
        bidPrice: 14950000000,
        discountRate: 1,
        finalPrice: 14800500000,
        bidValidityDays: 120,
        bidSecurity: '250,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '150 ngày'
      }
    ],
    logs: [
      { timestamp: '2026-06-12 11:20:01', level: 'INFO', stage: 'Search', message: 'Quét tự động định kỳ.' },
      { timestamp: '2026-06-12 11:20:05', level: 'INFO', stage: 'BBMT', message: 'Biên bản mở thầu đã parse thành công. Có 2 nhà thầu.' },
      { timestamp: '2026-06-12 11:20:07', level: 'WARN', stage: 'KQLCNT', message: 'Không thể tìm thấy kết quả lựa chọn nhà thầu. Cổng thông tin trả về 404 cho mã gói thầu bốc tách.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-12 11:20:07',
    lastMessage: 'Đồng bộ một phần thành công: Chờ công bố quyết định LCNT từ phía CĐT'
  },
  {
    notifyNo: 'IB2600022987',
    title: 'Cung cấp hóa chất khử trùng Cloramin B và trang bị bảo hộ chống dịch cho các trạm y tế phường xã trên địa bàn',
    investor: 'Trung tâm Kiểm soát Bệnh tật CDC TP.HCM',
    lifecycle: 'Có thông tin hợp đồng',
    scrapeStatus: 'COMPLETED',
    budget: 1850000000,
    publishDate: '12/03/2026',
    closeTime: getOffsetTimeISO(-2000),
    openTime: getOffsetTimeISO(-1999.5),
    folderPath: '\\\\192.168.1.150\\Public_MSC_Folder\\CDC_ChloramineB',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Nguồn vốn ngân sách nhà nước',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success',
      Contract: 'Success'
    },
    missingFields: [],
    bidders: [
      {
        no: 1,
        taxCode: '0310245648',
        name: 'Công ty Cổ phần Thiết bị Y tế Tân Sơn',
        bidPrice: 1820000000,
        discountRate: 0.5,
        finalPrice: 1810900000,
        bidValidityDays: 60,
        bidSecurity: '30,000,000 VND',
        securityValidityDays: 90,
        executionTimeText: '30 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Thiết bị Y tế Tân Sơn',
    winningPrice: 1810900000,
    contractSignDate: '10/04/2026',
    logs: [
      { timestamp: '2026-04-15 09:00:20', level: 'INFO', stage: 'Contract', message: 'Tải thành công tệp hợp đồng đã ký kết hai bên.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-04-15 09:00:20',
    lastMessage: 'Hoàn thành tuyệt đối: Đồng bộ trọn vẹn cả hợp đồng dịch bệnh'
  },
  {
    notifyNo: 'IB2600054230',
    title: 'Gói thầu số 10: Xây dựng cầu Rạch Đỉa quận 7 và san lấp mặt bằng hai đầu đường dẫn kết nối',
    investor: 'Ban quản lý công trình hạ tầng đô thị TP.HCM',
    lifecycle: 'Có KQLCNT',
    scrapeStatus: 'COMPLETED',
    budget: 89000000000,
    publishDate: '10/04/2026',
    closeTime: getOffsetTimeISO(-600),
    openTime: getOffsetTimeISO(-599.5),
    folderPath: 'D:/Cầu Rạch Đỉa/Xây Lắp 10',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Success',
      Conference: 'Success',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success',
      Contract: 'Not_Applicable'
    },
    missingFields: [],
    bidders: [
      {
        no: 1,
        taxCode: '0304124991',
        name: 'Công ty Cổ phần Xây dựng và Kinh doanh Địa ốc Hòa Bình',
        bidPrice: 87500000000,
        discountRate: 1.5,
        finalPrice: 86187500000,
        bidValidityDays: 150,
        bidSecurity: '1,500,000,000 VND',
        securityValidityDays: 180,
        executionTimeText: '360 ngày'
      },
      {
        no: 2,
        taxCode: '0100492812',
        name: 'Tổng công ty xây dựng công trình giao thông 6 (Cienco6)',
        bidPrice: 86500000000,
        discountRate: 0,
        finalPrice: 86500000000,
        bidValidityDays: 150,
        bidSecurity: '1,500,000,000 VND',
        securityValidityDays: 180,
        executionTimeText: '300 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Xây dựng và Kinh doanh Địa ốc Hòa Bình',
    winningPrice: 86187500000,
    logs: [
      { timestamp: '2026-05-20 08:30:10', level: 'INFO', stage: 'KQLCNT', message: 'Phát hiện tệp tin quyết định trúng thầu.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-05-20 08:30:10',
    lastMessage: 'Đồng bộ kết quả đấu thầu thành công'
  },
  {
    notifyNo: 'IB2600099881',
    title: 'Gói thầu số 01: Tư vấn lập thiết kế bản vẽ thi công và dự toán nhà điều hành kết hợp phòng khám vắc xin hiện đại',
    investor: 'Trung tâm Y tế huyện Long Thành, Đồng Nai',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 450000000,
    publishDate: '12/06/2026',
    closeTime: getOffsetTimeISO(2.5), // Còn 2.5 tiếng (đỏ)
    openTime: getOffsetTimeISO(3),
    folderPath: 'E:/Downloads_LongThanh/Vaccine_Design_01',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Not_Applicable',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 00:30:00', level: 'INFO', stage: 'Search', message: 'Tự động bốc tách dự toán thầu.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-13 00:30:00',
    lastMessage: 'Đồng bộ metadata thành công, chuẩn bị đóng thầu'
  },
  {
    notifyNo: 'IB2600023456',
    title: 'Xây lắp nhà học hiệu bộ và cải tạo sửa chữa sân trường, hệ thống thoát nước ngập úng Trường Tiểu học Bình Trị Đông B',
    investor: 'Ban quản lý dự án khu vực quận Bình Tân',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 5120000000,
    publishDate: '10/06/2026',
    closeTime: getOffsetTimeISO(18), // Còn 18 tiếng (vàng)
    openTime: getOffsetTimeISO(18.5),
    folderPath: 'C:/BinhTan_Projects/PrimarySchool_BinhTriDong',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [],
    logs: [
      { timestamp: '2026-06-11 09:00:00', level: 'INFO', stage: 'Search', message: 'Tải thông tin trường tiểu học thành công.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-11 09:00:00',
    lastMessage: 'Đang mời thầu - Đầy đủ hồ sơ'
  },
  {
    notifyNo: 'IB2600088888',
    title: 'Cung cấp lắp đặt hệ thống đèn chiếu sáng đô thị tiết kiệm năng lượng thích ứng khí hậu cho các trục lộ chính Quận 12',
    investor: 'Ủy ban nhân dân Quận 12, TP.HCM',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'COMPLETED',
    budget: 15100000000,
    publishDate: '08/06/2026',
    closeTime: getOffsetTimeISO(70), // Còn gần 3 ngày (bình thường)
    openTime: getOffsetTimeISO(70.5),
    folderPath: 'C:/Downloads/LED_lighting_Q12',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Success',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [],
    logs: [
      { timestamp: '2026-06-10 16:30:10', level: 'INFO', stage: 'Search', message: 'Scan thành công các mục làm rõ thầu.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-10 16:30:10',
    lastMessage: 'Đang mời thầu - Tốc độ đồng bộ bình thường'
  },
  {
    notifyNo: 'IB2600054231',
    title: 'Trang bị nâng cấp phần mềm cơ sở dữ liệu đất đai tổng thể và số hóa bản đồ địa chính kỹ thuật các phường',
    investor: 'Văn phòng Đăng ký Đất đai thành phố Đà Nẵng',
    lifecycle: 'Có KQLCNT',
    scrapeStatus: 'COMPLETED',
    budget: 12500000000,
    publishDate: '05/04/2026',
    closeTime: getOffsetTimeISO(-1500),
    openTime: getOffsetTimeISO(-1499.5),
    folderPath: 'D:/DaNang_LandManagement/Database_Upgrade_01',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi đấu thầu qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Not_Applicable',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success',
      Contract: 'Skipped'
    },
    missingFields: [],
    bidders: [
      {
        no: 1,
        taxCode: '0400123591',
        name: 'Công ty Cổ phần Công nghệ và Giải pháp Hệ thống thông tin FPT',
        bidPrice: 12380000000,
        discountRate: 0,
        finalPrice: 12380000000,
        bidValidityDays: 120,
        bidSecurity: '200,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '120 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Công nghệ và Giải pháp Hệ thống thông tin FPT',
    winningPrice: 12380000000,
    logs: [
      { timestamp: '2026-04-30 09:30:00', level: 'INFO', stage: 'KQLCNT', message: 'Lấy kết quả từ quyết định trúng thầu điện tử.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-04-30 09:30:00',
    lastMessage: 'Đồng bộ kết quả thành công - Nhà thầu FPT trúng tuyển'
  },
  {
    notifyNo: 'IB2600078192',
    title: 'Gói thầu số 1: Xây lắp nhà hiệu bộ, hội trường và nhà tập đa năng Trường THPT Chuyên Quốc học Huế',
    investor: 'Sở Giáo dục và Đào tạo tỉnh Thừa Thiên Huế',
    lifecycle: 'Có thông tin hợp đồng',
    scrapeStatus: 'COMPLETED',
    budget: 24500000000,
    publishDate: '10/03/2026',
    closeTime: getOffsetTimeISO(-2200),
    openTime: getOffsetTimeISO(-2199.5),
    folderPath: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
    folderExists: true,
    completenessPercent: 100,
    downloadCompletenessPercent: 100,
    biddingType: 'Đấu thầu rộng rãi',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Success',
      HSMT: 'Success',
      Clarification: 'Success',
      Petition: 'Not_Applicable',
      Conference: 'Success',
      BaoCaoLotOpen: 'Success',
      BaoCaoOpenDetail: 'Success',
      KQLCNT: 'Success',
      Contract: 'Success'
    },
    missingFields: [],
    bidders: [
      {
        no: 1,
        taxCode: '3300124231',
        name: 'Công ty Cổ phần Xây dựng Thừa Thiên Huế',
        bidPrice: 24150000000,
        discountRate: 0.5,
        finalPrice: 24029250000,
        bidValidityDays: 120,
        bidSecurity: '400,000,000 VND',
        securityValidityDays: 150,
        executionTimeText: '270 ngày'
      }
    ],
    winningContractor: 'Công ty Cổ phần Xây dựng Thừa Thiên Huế',
    winningPrice: 24029250000,
    contractSignDate: '20/04/2026',
    logs: [
      { timestamp: '2026-04-25 10:45:00', level: 'INFO', stage: 'Contract', message: 'Parse dữ liệu luồng hợp đồng thành công.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-04-25 10:45:00',
    lastMessage: 'Đồng bộ thành công và đã thu hồi đầy đủ metadata hợp đồng'
  },
  {
    notifyNo: 'IB2600021990',
    title: 'Cung cấp tủ trang bị cứu thương, thiết bị phục hồi chức năng và tập vật lý trị liệu phục vụ công tác xã hội năm học 2026',
    investor: 'Sở Lao động - Thương binh và Xã hội TP.HCM',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'QUEUED', // Đang xếp hàng đợi scrape
    budget: 1890000000,
    publishDate: '12/06/2026',
    closeTime: getOffsetTimeISO(45),
    openTime: getOffsetTimeISO(45.5),
    folderPath: 'C:/Downloads/Sở LDTBXH/Medical_Cabinet_2026',
    folderExists: true,
    completenessPercent: 0,
    downloadCompletenessPercent: 0,
    biddingType: 'Chào hàng cạnh tranh',
    apiCoverage: {
      Search: 'Skipped',
      TBMT: 'Skipped',
      HSMT: 'Skipped',
      Clarification: 'Skipped',
      Petition: 'Skipped',
      Conference: 'Skipped',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'TBMT',
        source: 'Queue Manager',
        reason: 'Hồ sơ đang ở trong hàng đợi Scrape metadata ban đầu.',
        suggestion: 'Hệ thống sẽ chạy xử lý gói thầu này ở lượt chạy monitor tự động tiếp theo.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 04:30:00', level: 'INFO', stage: 'Queue', message: 'Tạo hàng đợi scrape thành công, trạng thái chuẩn bị bắt đầu.' }
    ],
    retryCount: 0,
    lastSyncTime: null,
    lastMessage: 'Đang nằm trong hàng đợi Scrape tự động'
  },
  {
    notifyNo: 'IB2600030045',
    title: 'Gói thầu số 4: Sửa chữa bảo dưỡng định kỳ hệ thống máy nén khí áp lực cao nhà máy thủy điện Trị An năm 2026',
    investor: 'Công ty Thủy điện Trị An',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'SEARCHING', // Đang trong quá trình tìm kiếm thông tin
    budget: 1250000000,
    publishDate: '11/06/2026',
    closeTime: getOffsetTimeISO(40),
    openTime: getOffsetTimeISO(40.5),
    folderPath: 'D:/ThuyDienTriAn/Compressor_Repair_2026',
    folderExists: true,
    completenessPercent: 20,
    downloadCompletenessPercent: 0,
    biddingType: 'Đấu thầu qua mạng',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Skipped',
      HSMT: 'Skipped',
      Clarification: 'Skipped',
      Petition: 'Skipped',
      Conference: 'Skipped',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'TBMT',
        source: 'Scraper Engine',
        reason: 'Hệ thống đang tích cực thực hiện parse API Search nâng nâng cao.',
        suggestion: 'Chờ tiến trình hoàn tất bốc tách.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 04:54:10', level: 'INFO', stage: 'Search', message: 'Gửi yêu cầu POST lên cổng thông tin MSC để kiểm tra tính bảo mật.' }
    ],
    retryCount: 0,
    lastSyncTime: '2026-06-13 04:54:10',
    lastMessage: 'Đang thực hiện phân tích luồng tìm kiếm cổng MSC'
  },
  {
    notifyNo: 'IB2600078512',
    title: 'Ủy thác mua sắm trang thiết bị vật tư y tế chống sốt xuất huyết vùng bùng phát dịch Cần Thơ giai đoạn lâm sàng',
    investor: 'Sở Y tế thành phố Cần Thơ',
    lifecycle: 'Đang mời thầu',
    scrapeStatus: 'FETCHING_TBMT', // Đang fetch TBMT
    budget: 1950000000,
    publishDate: '12/06/2026',
    closeTime: getOffsetTimeISO(150),
    openTime: getOffsetTimeISO(150.5),
    folderPath: 'D:/Medical_CanTho/Dengue_Fever_2026',
    folderExists: true,
    completenessPercent: 35,
    downloadCompletenessPercent: 0,
    biddingType: 'Mua sắm khẩn cấp',
    apiCoverage: {
      Search: 'Success',
      TBMT: 'Failed', // Đang retry
      HSMT: 'Skipped',
      Clarification: 'Skipped',
      Petition: 'Skipped',
      Conference: 'Skipped',
      BaoCaoLotOpen: 'Skipped',
      BaoCaoOpenDetail: 'Skipped',
      KQLCNT: 'Skipped',
      Contract: 'Skipped'
    },
    missingFields: [
      {
        field: 'budget',
        source: 'TBMT API',
        reason: 'Hệ thống đang tải dở dang luồng dữ liệu TBMT thô.',
        suggestion: 'Tiến trình sẽ kích hoạt giải mã sau khi tải xong.'
      }
    ],
    bidders: [],
    logs: [
      { timestamp: '2026-06-13 04:54:30', level: 'INFO', stage: 'Search', message: 'Mã số thầu hợp lệ.' },
      { timestamp: '2026-06-13 04:55:00', level: 'WARN', stage: 'TBMT', message: 'Tốc độ phản hồi từ MSC chậm (Timeout 15 giây). Đang thử thiết lập kết nối lại lần thứ 2...' }
    ],
    retryCount: 1,
    lastSyncTime: '2026-06-13 04:55:00',
    lastMessage: 'Đang tải thông tin chi tiết tờ trình mời thầu (TBMT) từ API'
  }
];
