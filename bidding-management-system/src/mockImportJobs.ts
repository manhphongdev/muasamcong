/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { ImportJob } from './types';

export const mockImportJobs: ImportJob[] = [
  {
    id: 'IMP-20260613-01',
    startedAt: '2026-06-13 09:00:22',
    endedAt: '2026-06-13 09:01:05',
    triggeredBy: 'Manual Paste',
    status: 'COMPLETED',
    totalRows: 6,
    validCount: 4,
    duplicatedCount: 1,
    invalidCount: 1,
    importedCount: 4,
    failedCount: 0,
    items: [
      {
        id: '1',
        input: 'IB2600047379',
        notifyNo: 'IB2600047379',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: '\\\\192.168.1.150\\Public_MSC_Folder\\Water_DongNai_02',
        message: 'Import thành công. Sẵn sàng đồng bộ.'
      },
      {
        id: '2',
        input: 'IB2600026487',
        notifyNo: 'IB2600026487',
        status: 'DUPLICATED',
        isDuplicate: true,
        folderPath: 'D:/DuAn_IT_Hanoi/GoiIT_MayTram_02',
        message: 'Gói thầu đã có trong cơ sở dữ liệu giám sát.'
      },
      {
        id: '3',
        input: 'IB2600099112',
        notifyNo: 'IB2600099112',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'E:/CongAn_ThuDuc/Cam_AI_Traffic_2026',
        message: 'Import thành công. Đã cấu hình giám sát khẩn cấp.'
      },
      {
        id: '4',
        input: 'IBXXXXINVALID',
        notifyNo: null,
        status: 'INVALID',
        isDuplicate: false,
        folderPath: null,
        message: 'Sai định dạng mã số TBMT cổng quốc gia.'
      },
      {
        id: '5',
        input: 'IB2600010492',
        notifyNo: 'IB2600010492',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'C:/Văn phòng phẩm/Bình Thạnh 2026',
        message: 'Import thành công.'
      },
      {
        id: '6',
        input: 'IB2600035541',
        notifyNo: 'IB2600035541',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/DongNai_Projects/Amata_Highway_05',
        message: 'Import thành công.'
      }
    ]
  },
  {
    id: 'IMP-20260612-02',
    startedAt: '2026-06-12 14:15:00',
    endedAt: '2026-06-12 14:15:30',
    triggeredBy: 'CSV Drag & Drop',
    status: 'COMPLETED',
    totalRows: 3,
    validCount: 3,
    duplicatedCount: 0,
    invalidCount: 0,
    importedCount: 3,
    failedCount: 0,
    items: [
      {
        id: '1',
        input: 'IB2600022199',
        notifyNo: 'IB2600022199',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/Lab_VNU/LyTam_Biotech_2026',
        message: 'Import thành công.'
      },
      {
        id: '2',
        input: 'IB2600088190',
        notifyNo: 'IB2600088190',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: null,
        message: 'Import thành công (Chưa gán thư mục cục bộ).'
      },
      {
        id: '3',
        input: 'IB2600055231',
        notifyNo: 'IB2600055231',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'Z:/Local_Server_Backup/AnGiang_Project_ThanhMy',
        message: 'Import thành công.'
      }
    ]
  },
  {
    id: 'IMP-20260611-03',
    startedAt: '2026-06-11 10:00:10',
    endedAt: '2026-06-11 10:01:20',
    triggeredBy: 'Folder Watcher Scan',
    status: 'COMPLETED',
    totalRows: 4,
    validCount: 3,
    duplicatedCount: 1,
    invalidCount: 0,
    importedCount: 3,
    failedCount: 0,
    items: [
      {
        id: '1',
        input: 'IB2600021487',
        notifyNo: 'IB2600021487',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/BinhThanh_Projects/Tru_So_HuyenUy_2026',
        message: 'Import từ bóc tách thư mục thành công.'
      },
      {
        id: '2',
        input: 'IB2600022987',
        notifyNo: 'IB2600022987',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: '\\\\192.168.1.150\\Public_MSC_Folder\\CDC_ChloramineB',
        message: 'Import thành công.'
      },
      {
        id: '3',
        input: 'IB2600022199',
        notifyNo: 'IB2600022199',
        status: 'DUPLICATED',
        isDuplicate: true,
        folderPath: 'D:/Lab_VNU/LyTam_Biotech_2026',
        message: 'Trùng lặp với gói thầu đã giám sát.'
      },
      {
        id: '4',
        input: 'IB2600054230',
        notifyNo: 'IB2600054230',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/Cầu Rạch Đỉa/Xây Lắp 10',
        message: 'Import thành công.'
      }
    ]
  },
  {
    id: 'IMP-20260610-04',
    startedAt: '2026-06-10 16:30:00',
    endedAt: null,
    triggeredBy: 'Manual Input Form',
    status: 'FAILED',
    totalRows: 1,
    validCount: 0,
    duplicatedCount: 0,
    invalidCount: 1,
    importedCount: 0,
    failedCount: 1,
    items: [
      {
        id: '1',
        input: 'NOTHING_HERE_JUST_TEXT',
        notifyNo: null,
        status: 'INVALID',
        isDuplicate: false,
        folderPath: null,
        message: 'Không tìm thấy số TBMT nào sau khi bóc tách chuỗi.'
      }
    ]
  },
  {
    id: 'IMP-20260608-05',
    startedAt: '2026-06-08 08:30:00',
    endedAt: '2026-06-08 08:31:00',
    triggeredBy: 'Excel Bulk Sheet',
    status: 'COMPLETED',
    totalRows: 5,
    validCount: 5,
    duplicatedCount: 0,
    invalidCount: 0,
    importedCount: 5,
    failedCount: 0,
    items: [
      {
        id: '1',
        input: 'IB2600099881',
        notifyNo: 'IB2600099881',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'E:/Downloads_LongThanh/Vaccine_Design_01',
        message: 'Thêm mới thành công.'
      },
      {
        id: '2',
        input: 'IB2600023456',
        notifyNo: 'IB2600023456',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'C:/BinhTan_Projects/PrimarySchool_BinhTriDong',
        message: 'Thêm mới thành công.'
      },
      {
        id: '3',
        input: 'IB2600088888',
        notifyNo: 'IB2600088888',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'C:/Downloads/LED_lighting_Q12',
        message: 'Thêm mới thành công.'
      },
      {
        id: '4',
        input: 'IB2600054231',
        notifyNo: 'IB2600054231',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/DaNang_LandManagement/Database_Upgrade_01',
        message: 'Thêm mới thành công.'
      },
      {
        id: '5',
        input: 'IB2600078192',
        notifyNo: 'IB2600078192',
        status: 'IMPORTED',
        isDuplicate: false,
        folderPath: 'D:/Hue_School_Projects/QuocHoc_MultisportHouse',
        message: 'Thêm mới thành công.'
      }
    ]
  }
];
