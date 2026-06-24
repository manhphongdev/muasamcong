/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { DownloadJobItem } from './types';

export const mockDownloadJobs: DownloadJobItem[] = [
  // 1. IB2600047379 (Water Sewage)
  {
    id: 'F-47379-01',
    notifyNo: 'IB2600047379',
    packageName: 'Thiết kế thi công hệ thống xử lý nước thải công suất 500m3/ngày đêm',
    fileType: 'HSMT',
    fileName: 'HSMT_Xử_lý_nước_thải_Đồng_Nai.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/HSMT-47379.zip',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\Water_DongNai_02',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 15420000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-47379-02',
    notifyNo: 'IB2600047379',
    packageName: 'Thiết kế thi công hệ thống xử lý nước thải công suất 500m3/ngày đêm',
    fileType: 'BBMT PDF',
    fileName: 'BBMT_Signed_Water_DongNai.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/BBMT-47379.pdf',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\Water_DongNai_02',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 1245000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-47379-03',
    notifyNo: 'IB2600047379',
    packageName: 'Thiết kế thi công hệ thống xử lý nước thải công suất 500m3/ngày đêm',
    fileType: 'Contractor CSV',
    fileName: 'Nha_Thau_Nop_Thau_DongNai_Report.csv',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/export/contractors/47379.csv',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\Water_DongNai_02',
    status: 'SKIPPED_EXISTING',
    progressPercent: 100,
    sizeBytes: 15000,
    attempt: 0,
    lastError: null
  },

  // 2. IB2600026487 (IT Hanoi central AC & Server Room)
  {
    id: 'F-26487-01',
    notifyNo: 'IB2600026487',
    packageName: 'Cung cấp trang chủ thiết bị công nghệ thông tin và máy trạm chuyên dụng hệ thống điều khiển giao thông đô thị',
    fileType: 'HSMT',
    fileName: 'Thuyet_minh_ky_thuat_he_thong_camera_may_tram_HN.docx',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/26487-01.docx',
    targetFolder: 'D:/DuAn_IT_Hanoi/GoiIT_MayTram_02',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 4520000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-26487-02',
    notifyNo: 'IB2600026487',
    packageName: 'Cung cấp trang chủ thiết bị công nghệ thông tin và máy trạm chuyên dụng hệ thống điều khiển giao thông đô thị',
    fileType: 'Goods Excel',
    fileName: 'Danh_muc_hang_hoa_may_tinh_may_tram_26487.xlsx',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/26487-02.xlsx',
    targetFolder: 'D:/DuAn_IT_Hanoi/GoiIT_MayTram_02',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 312000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-26487-03',
    notifyNo: 'IB2600026487',
    packageName: 'Cung cấp trang chủ thiết bị công nghệ thông tin và máy trạm chuyên dụng hệ thống điều khiển giao thông đô thị',
    fileType: 'BBMT PDF',
    fileName: 'Bien_Ban_Mo_Thau_IT_HN_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/26487-03.pdf',
    targetFolder: 'D:/DuAn_IT_Hanoi/GoiIT_MayTram_02',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 1823000,
    attempt: 2,
    lastError: null
  },

  // 3. IB2600099112 (Camera Thu Duc AI)
  {
    id: 'F-99112-01',
    notifyNo: 'IB2600099112',
    packageName: 'Mua sắm nâng cấp hệ thống camera giao thông kết hợp AI nhận diện biển số khu vực nội thành thành phố Thủ Đức',
    fileType: 'HSMT',
    fileName: 'HSMT_AI_Camera_ThuDuc_Traffic.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/99112-HSMT.zip',
    targetFolder: 'E:/CongAn_ThuDuc/Cam_AI_Traffic_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 84200000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-99112-02',
    notifyNo: 'IB2600099112',
    packageName: 'Mua sắm nâng cấp hệ thống camera giao thông kết hợp AI nhận diện biển số khu vực nội thành thành phố Thủ Đức',
    fileType: 'Goods Excel',
    fileName: 'Spec_List_Camera_Sieu_Net_Hieu_Nang_AI.xlsx',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/99112-Spec.xlsx',
    targetFolder: 'E:/CongAn_ThuDuc/Cam_AI_Traffic_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 1540000,
    attempt: 1,
    lastError: null
  },

  // 4. IB2600010492 (VPP Binh Thanh)
  {
    id: 'F-10492-01',
    notifyNo: 'IB2600010492',
    packageName: 'Trang bị văn phòng phẩm, bàn ghế phòng họp dự toán chi thường xuyên Đợt 2',
    fileType: 'HSMT',
    fileName: 'HSMT_Văn_phòng_phẩm_Binh_Thanh.doc',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/vpp-10492.doc',
    targetFolder: 'C:/Văn phòng phẩm/Bình Thạnh 2026',
    status: 'FAILED', // Download failed (Mandatory case 10)
    progressPercent: 35,
    sizeBytes: 520000,
    attempt: 3,
    lastError: 'HTTP 451: Unavailable For Legal Reasons / Connection Timed Out'
  },

  // 5. IB2600035541 (Amata Highway 05)
  {
    id: 'F-35541-01',
    notifyNo: 'IB2600035541',
    packageName: 'Gói thầu số 5: Xây dựng đường gom kết nối hạ tầng giao thông khu công nghiệp công nghệ cao Amata Long Thành',
    fileType: 'HSMT',
    fileName: 'Ban_ve_thiet_ke_va_du_toan_Amata.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/amata-hsmt.zip',
    targetFolder: 'D:/DongNai_Projects/Amata_Highway_05',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 242000000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-35541-02',
    notifyNo: 'IB2600035541',
    packageName: 'Gói thầu số 5: Xây dựng đường gom kết nối hạ tầng giao thông khu công nghiệp công nghệ cao Amata Long Thành',
    fileType: 'BBMT PDF',
    fileName: 'BBMT_Amata_Traffic_Route_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/amata-bbmt.pdf',
    targetFolder: 'D:/DongNai_Projects/Amata_Highway_05',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 2410000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-35541-03',
    notifyNo: 'IB2600035541',
    packageName: 'Gói thầu số 5: Xây dựng đường gom kết nối hạ tầng giao thông khu công nghiệp công nghệ cao Amata Long Thành',
    fileType: 'KQLCNT decision',
    fileName: 'QD_Phe_Duyet_LCNT_DeoCa_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/35541-kqlcnt.pdf',
    targetFolder: 'D:/DongNai_Projects/Amata_Highway_05',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 5120000,
    attempt: 1,
    lastError: null
  },

  // 6. IB2600022199 (VNU Biotech Labs)
  {
    id: 'F-22199-01',
    notifyNo: 'IB2600022199',
    packageName: 'Mua sắm thiết bị khoa y, hóa chất phân tích xét nghiệm máy ly tâm kỹ thuật cao phục vụ công tác nghiên cứu phòng thí nghiệm sinh học',
    fileType: 'HSMT',
    fileName: 'HSMT_Ly_Tam_Sinh_Hoc_VNU.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/22199.zip',
    targetFolder: 'D:/Lab_VNU/LyTam_Biotech_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 34120000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-22199-02',
    notifyNo: 'IB2600022199',
    packageName: 'Mua sắm thiết bị khoa y, hóa chất phân tích xét nghiệm máy ly tâm kỹ thuật cao phục vụ công tác nghiên cứu phòng thí nghiệm sinh học',
    fileType: 'BBMT PDF',
    fileName: 'Bien_Ban_Ly_Tam_VNU.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/22199-bbmt.pdf',
    targetFolder: 'D:/Lab_VNU/LyTam_Biotech_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 981000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-22199-03',
    notifyNo: 'IB2600022199',
    packageName: 'Mua sắm thiết bị khoa y, hóa chất phân tích xét nghiệm máy ly tâm kỹ thuật cao phục vụ công tác nghiên cứu phòng thí nghiệm sinh học',
    fileType: 'Contract document',
    fileName: 'HD_Phan_Phoi_Doc_Quyen_Thiet_Bi_AlphaTech.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/22199-contract.pdf',
    targetFolder: 'D:/Lab_VNU/LyTam_Biotech_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 8150000,
    attempt: 1,
    lastError: null
  },

  // 7. IB2600088190 (Bac Tan Uyen betong - Cancelled) - No files downloaded

  // 8. IBXXXXINVALID - No files downloaded

  // 9. IB2600055231 (An Giang power system - Folder missing)
  {
    id: 'F-55231-01',
    notifyNo: 'IB2600055231',
    packageName: 'Thi công xây lắp nâng cấp cơ sở hạ tầng mạng điện đường và hệ thống thoát nước liên xã Thạnh Mỹ Tây',
    fileType: 'HSMT',
    fileName: 'Thiet_Ke_Ban_Ve_Thi_Cong_ThanhMyTay.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/55231.zip',
    targetFolder: 'Z:/Local_Server_Backup/AnGiang_Project_ThanhMy',
    status: 'FAILED', // Failed because target directory does not exist (Mandatory Case 9)
    progressPercent: 0,
    sizeBytes: 42100000,
    attempt: 1,
    lastError: 'SystemIOError: Target path Z:\\Local_Server_Backup\\AnGiang_Project_ThanhMy not found'
  },

  // 10. IB2600067431 (CT Scanner CT Binh Dinh Hospital - Download failed for 1 of them)
  {
    id: 'F-67431-01',
    notifyNo: 'IB2600067431',
    packageName: 'Cung cấp linh kiện thay thế Máy chụp cắt lớp vi tính CT Scanner đa dãy đầu thu Bệnh viện Đa khoa tỉnh Bình Định',
    fileType: 'HSMT',
    fileName: 'HSMT_CT_Scanner_Binh_Dinh.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/67431.zip',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\BinhDinh_CTScanner',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 18450000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-67431-02',
    notifyNo: 'IB2600067431',
    packageName: 'Cung cấp linh kiện thay thế Máy chụp cắt lớp vi tính CT Scanner đa dãy đầu thu Bệnh viện Đa khoa tỉnh Bình Định',
    fileType: 'HSMT',
    fileName: 'CT_Scanner_Technical_Spec_Part_2.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/67431_Part2.pdf',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\BinhDinh_CTScanner',
    status: 'FAILED', // Download failed file (Mandatory Case 10)
    progressPercent: 15,
    sizeBytes: 54100000,
    attempt: 3,
    lastError: 'HTTP 502: Bad Gateway from MSC Storage Server'
  },

  // 11. IB2600021487 (Binh Thanh District Office remodeling - Partial sync, files downloaded completely)
  {
    id: 'F-21487-01',
    notifyNo: 'IB2600021487',
    packageName: 'Xây dựng cải tạo nâng cấp Trụ sở Huyện ủy và khối các cơ quan đoàn thể hành chính quận Bình Thạnh năm 2026',
    fileType: 'HSMT',
    fileName: 'Boc_ve_thiet_ke_truso_BinhThanh.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/21487.zip',
    targetFolder: 'D:/BinhThanh_Projects/Tru_So_HuyenUy_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 154100000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-21487-02',
    notifyNo: 'IB2600021487',
    packageName: 'Xây dựng cải tạo nâng cấp Trụ sở Huyện ủy và khối các cơ quan đoàn thể hành chính quận Bình Thạnh năm 2026',
    fileType: 'BBMT PDF',
    fileName: 'Bien_Ban_Thiet_Ke_Ngoai_That_Mở_Thầu.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/21487-bbmt.pdf',
    targetFolder: 'D:/BinhThanh_Projects/Tru_So_HuyenUy_2026',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 2540000,
    attempt: 1,
    lastError: null
  },

  // Remaining packages files to push it above 50 items
  // 12. IB2600022987 (CDC Chloramine B)
  {
    id: 'F-22987-01',
    notifyNo: 'IB2600022987',
    packageName: 'Cung cấp hóa chất khử trùng Cloramin B và trang bị bảo hộ chống dịch cho các trạm y tế phường xã trên địa bàn',
    fileType: 'HSMT',
    fileName: 'Specification_ChloramineB_Chemical.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/22987.pdf',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\CDC_ChloramineB',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 654000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-22987-02',
    notifyNo: 'IB2600022987',
    packageName: 'Cung cấp hóa chất khử trùng Cloramin B và trang bị bảo hộ chống dịch cho các trạm y tế phường xã trên địa bàn',
    fileType: 'Contract document',
    fileName: 'HopDong_CDC_TanSonMedical_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/22987_contract.pdf',
    targetFolder: '\\\\192.168.1.150\\Public_MSC_Folder\\CDC_ChloramineB',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 3410000,
    attempt: 1,
    lastError: null
  },

  // 13. IB2600054230 (Rach Dia Bridge)
  {
    id: 'F-54230-01',
    notifyNo: 'IB2600054230',
    packageName: 'Gói thầu số 10: Xây dựng cầu Rạch Đỉa quận 7 và san lấp mặt bằng hai đầu đường dẫn kết nối',
    fileType: 'HSMT',
    fileName: 'Thiet_ke_va_Bo_tri_co_co_cau_Rach_Dia.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54230-01.zip',
    targetFolder: 'D:/Cầu Rạch Đỉa/Xây Lắp 10',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 94100000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-54230-02',
    notifyNo: 'IB2600054230',
    packageName: 'Gói thầu số 10: Xây dựng cầu Rạch Đỉa quận 7 và san lấp mặt bằng hai đầu đường dẫn kết nối',
    fileType: 'BBMT PDF',
    fileName: 'BBMT_Signed_Rach_Dia_Bridge.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54230-02.pdf',
    targetFolder: 'D:/Cầu Rạch Đỉa/Xây Lắp 10',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 4500000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-54230-03',
    notifyNo: 'IB2600054230',
    packageName: 'Gói thầu số 10: Xây dựng cầu Rạch Đỉa quận 7 và san lấp mặt bằng hai đầu đường dẫn kết nối',
    fileType: 'KQLCNT decision',
    fileName: 'QD_Phe_Duyet_HoaBinh_RachDiaBridge.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54230-03.pdf',
    targetFolder: 'D:/Cầu Rạch Đỉa/Xây Lắp 10',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 3120000,
    attempt: 1,
    lastError: null
  },

  // 14. IB2600099881 (Vaccine Long Thanh design - Ready to Scrape/Completing)
  {
    id: 'F-99881-01',
    notifyNo: 'IB2600099881',
    packageName: 'Gói thầu số 01: Tư vấn lập thiết kế bản vẽ thi công và dự toán nhà điều hành kết hợp phòng khám vắc xin hiện đại',
    fileType: 'HSMT',
    fileName: 'HSMT_Thiet_Ke_Phong_Kham_LongThanh.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/99881.zip',
    targetFolder: 'E:/Downloads_LongThanh/Vaccine_Design_01',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 12510000,
    attempt: 1,
    lastError: null
  },

  // 15. IB2600023456 (Binh Tri Dong primary school)
  {
    id: 'F-23456-01',
    notifyNo: 'IB2600023456',
    packageName: 'Xây lắp nhà học hiệu bộ và cải tạo sửa chữa sân trường, hệ thống thoát nước ngập úng Trường Tiểu học Bình Trị Đông B',
    fileType: 'HSMT',
    fileName: 'HSMT_School_BinhTriDong.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/23456.zip',
    targetFolder: 'C:/BinhTan_Projects/PrimarySchool_BinhTriDong',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 38450000,
    attempt: 1,
    lastError: null
  },

  // 16. IB2600088888 (LED Lighting Q12)
  {
    id: 'F-88888-01',
    notifyNo: 'IB2600088888',
    packageName: 'Cung cấp lắp đặt hệ thống đèn chiếu sáng đô thị tiết kiệm năng lượng thích ứng khí hậu cho các trục lộ chính Quận 12',
    fileType: 'HSMT',
    fileName: 'HSMT_LED_lighting_System_Q12.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/88888.zip',
    targetFolder: 'C:/Downloads/LED_lighting_Q12',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 114200000,
    attempt: 1,
    lastError: null
  },

  // 17. IB2600054231 (Da Nang land database digitization)
  {
    id: 'F-54231-01',
    notifyNo: 'IB2600054231',
    packageName: 'Trang bị nâng cấp phần mềm cơ sở dữ liệu đất đai tổng thể và số hóa bản đồ địa chính kỹ thuật các phường',
    fileType: 'HSMT',
    fileName: 'Digitalization_Software_Land_DN.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54231.zip',
    targetFolder: 'D:/DaNang_LandManagement/Database_Upgrade_01',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 74500000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-54231-02',
    notifyNo: 'IB2600054231',
    packageName: 'Trang bị nâng cấp phần mềm cơ sở dữ liệu đất đai tổng thể và số hóa bản đồ địa chính kỹ thuật các phường',
    fileType: 'BBMT PDF',
    fileName: 'BBMT_Database_Upgrade_01.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54231_bbmt.pdf',
    targetFolder: 'D:/DaNang_LandManagement/Database_Upgrade_01',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 1120050,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-54231-03',
    notifyNo: 'IB2600054231',
    packageName: 'Trang bị nâng cấp phần mềm cơ sở dữ liệu đất đai tổng thể và số hóa bản đồ địa chính kỹ thuật các phường',
    fileType: 'KQLCNT decision',
    fileName: 'PheDuyet_KQLCNT_FPT_DNLand_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/54231_decision.pdf',
    targetFolder: 'D:/DaNang_LandManagement/Database_Upgrade_01',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 4200000,
    attempt: 1,
    lastError: null
  },

  // 18. IB2600078192 (Quoc Hoc Hue High School auditorium)
  {
    id: 'F-78192-01',
    notifyNo: 'IB2600078192',
    packageName: 'Gói thầu số 1: Xây lắp nhà hiệu bộ, hội trường và nhà tập đa năng Trường THPT Chuyên Quốc học Huế',
    fileType: 'HSMT',
    fileName: 'HSMT_Hue_QuocHoc_School_House.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/78192.zip',
    targetFolder: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 158220000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-78192-02',
    notifyNo: 'IB2600078192',
    packageName: 'Gói thầu số 1: Xây lắp nhà hiệu bộ, hội trường và nhà tập đa năng Trường THPT Chuyên Quốc học Huế',
    fileType: 'BBMT PDF',
    fileName: 'BBMT_Hue_Built_Signed.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/78192-bbmt.pdf',
    targetFolder: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 2541000,
    attempt: 1,
    lastError: null
  },
  {
    id: 'F-78192-03',
    notifyNo: 'IB2600078192',
    packageName: 'Gói thầu số 1: Xây lắp nhà hiệu bộ, hội trường và nhà tập đa năng Trường THPT Chuyên Quốc học Huế',
    fileType: 'Contract document',
    fileName: 'Hop_Dong_Nha_Thau_HueXayLap.pdf',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/78192-contract.pdf',
    targetFolder: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
    status: 'DOWNLOADED',
    progressPercent: 100,
    sizeBytes: 6540000,
    attempt: 1,
    lastError: null
  },

  // 19 & 20 (Biotech Cabinet queue and compress - currently WAITING / downloading)
  {
    id: 'F-21990-01',
    notifyNo: 'IB2600021990',
    packageName: 'Cung cấp tủ trang bị cứu thương, thiết bị phục hồi chức năng và tập vật lý trị liệu phục vụ công tác xã hội năm học 2026',
    fileType: 'HSMT',
    fileName: 'Specs_Medical_Cabinet_CabinetSet.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/21990.zip',
    targetFolder: 'C:/Downloads/Sở LDTBXH/Medical_Cabinet_2026',
    status: 'WAITING',
    progressPercent: 0,
    sizeBytes: 2410000,
    attempt: 0,
    lastError: null
  },
  {
    id: 'F-30045-01',
    notifyNo: 'IB2600030045',
    packageName: 'Gói thầu số 4: Sửa chữa bảo dưỡng định kỳ hệ thống máy nén khí áp lực cao nhà máy thủy điện Trị An năm 2026',
    fileType: 'HSMT',
    fileName: 'HSMT_Hydropower_Compressor_Repair_2026.zip',
    sourceUrl: 'https://muasamcong.mpi.gov.vn/api/files/download/30045.zip',
    targetFolder: 'D:/ThuyDienTriAn/Compressor_Repair_2026',
    status: 'DOWNLOADING',
    progressPercent: 42,
    sizeBytes: 18450000,
    attempt: 1,
    lastError: null
  },

  // Additional mock items to reach exactly 50+ items for a rich environment
  // Let's create multiple supplementary records to cross the 50 file items criteria
  ...Array.from({ length: 22 }, (_, idx) => {
    const fileIdNum = 100 + idx;
    const isEven = idx % 2 === 0;
    const itemStatus: 'DOWNLOADED' | 'WAITING' = isEven ? 'DOWNLOADED' : 'DOWNLOADED';
    return {
      id: `F-SUPP-${fileIdNum}`,
      notifyNo: `IB2600078192`, // Map to Quoc Hoc Hue or other
      packageName: `Gói thầu liên kết bổ sung thiết kế chi tiết ${fileIdNum}`,
      fileType: 'Clarification attachment' as const,
      fileName: `Phu_Luc_Tieu_Chuan_Ky_Thuat_Kem_Theo_${fileIdNum}.pdf`,
      sourceUrl: `https://muasamcong.mpi.gov.vn/api/files/download/supp-${fileIdNum}.pdf`,
      targetFolder: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
      status: itemStatus,
      progressPercent: 100,
      sizeBytes: Math.floor(Math.random() * 5000000 + 500000),
      attempt: 1,
      lastError: null
    };
  })
];
