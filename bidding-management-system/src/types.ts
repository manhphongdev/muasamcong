/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

// Import Status
export type ImportStatus = 'PENDING' | 'VALIDATING' | 'IMPORTED' | 'DUPLICATED' | 'INVALID' | 'FAILED';

// Scrape Sync Status
export type ScrapeStatus = 
  | 'QUEUED' 
  | 'SEARCHING' 
  | 'FETCHING_TBMT' 
  | 'FETCHING_BBMT' 
  | 'FETCHING_KQLCNT' 
  | 'FETCHING_CONTRACT' 
  | 'NORMALIZING' 
  | 'COMPLETED' 
  | 'PARTIAL' 
  | 'FAILED';

// Download Status
export type DownloadStatus = 'WAITING' | 'DOWNLOADING' | 'DOWNLOADED' | 'SKIPPED_EXISTING' | 'FAILED';

// Monitor Status
export type MonitorStatus = 'IDLE' | 'RUNNING' | 'PAUSED' | 'STOPPING' | 'FAILED';

// Package Lifecycle State
export type PackageLifecycle = 
  | 'Đang mời thầu'
  | 'Đã đóng thầu'
  | 'Đã mở thầu'
  | 'Có KQLCNT'
  | 'Có thông tin hợp đồng'
  | 'Không xác định';

// Api Names
export type ApiStageName = 
  | 'Search' 
  | 'TBMT' 
  | 'HSMT' 
  | 'Clarification' 
  | 'Petition' 
  | 'Conference' 
  | 'BaoCaoLotOpen' 
  | 'BaoCaoOpenDetail' 
  | 'KQLCNT' 
  | 'Contract';

// Api cell state
export type ApiState = 'Success' | 'Missing' | 'Not_Applicable' | 'Failed' | 'Skipped';

export interface ApiCoverage {
  apiName: ApiStageName;
  status: ApiState;
  message?: string;
  updatedAt?: string;
}

export interface BidderOpening {
  no: number;
  taxCode: string;
  name: string;
  bidPrice: number;
  discountRate: number; // percent e.g., 5 for 5%
  finalPrice: number;
  bidValidityDays: number | null; // e.g., 90
  bidSecurity: string; // e.g., "30,000,000 VND"
  securityValidityDays: number | null; // e.g., 120
  executionTimeText: string;
}

export interface ScrapingLog {
  timestamp: string;
  level: 'INFO' | 'WARN' | 'ERROR';
  stage: string;
  message: string;
}

export interface PackageData {
  notifyNo: string; // e.g., "IB2600047379"
  title: string;
  investor: string;
  lifecycle: PackageLifecycle;
  scrapeStatus: ScrapeStatus;
  budget: number;
  closeTime: string | null; // ISO string
  openTime: string | null; // ISO string
  publishDate: string; // dd/MM/yyyy
  folderPath: string | null;
  folderExists: boolean;
  completenessPercent: number;
  downloadCompletenessPercent: number;
  documentTotal?: number;
  documentDownloaded?: number;
  documentFailed?: number;
  documentPending?: number;
  biddingType: string;
  apiCoverage: Record<ApiStageName, ApiState>;
  missingFields: { field: string; source: string; reason: string; suggestion: string }[];
  bidders: BidderOpening[];
  winningContractor?: string;
  winningPrice?: number;
  bidUrl?: string | null;
  contractSignDate?: string;
  logs: ScrapingLog[];
  retryCount: number;
  lastSyncTime: string | null;
  lastMessage: string;
}

export interface ImportJobItem {
  id: string; // STT / ID
  input: string;
  notifyNo: string | null;
  status: ImportStatus;
  isDuplicate: boolean;
  folderPath: string | null;
  message: string;
}

export interface ImportJob {
  id: string; // Job ID (e.g., IMP-001)
  startedAt: string;
  endedAt: string | null;
  triggeredBy: string;
  status: 'COMPLETED' | 'FAILED' | 'RUNNING';
  totalRows: number;
  validCount: number;
  duplicatedCount: number;
  invalidCount: number;
  importedCount: number;
  failedCount: number;
  items: ImportJobItem[];
}

export interface SyncPendingItemResult {
  notifyNo: string;
  success: boolean;
  message: string;
  syncItemId: number | null;
  contractId: number | null;
  contractInfoId: number | null;
  documentFoundThisRun: number | null;
  documentNewThisRun: number | null;
  documentExistingThisRun: number | null;
  documentTotal: number | null;
  documentSuccess: number | null;
  documentFailed: number | null;
  documentSuccessRate: number | null;
}

export interface DocumentFileItem {
  id: number;
  fileId: string;
  fileName: string;
  fileType: string;
  downloadStatus: string;
  storagePath: string | null;
  fileSize: number | null;
  errorMessage: string | null;
}

export interface DocumentSummary {
  notifyNo: string;
  total: number;
  pending: number;
  downloading: number;
  success: number;
  failed: number;
  successRate: number;
  byFileType: Record<string, number>;
  files: DocumentFileItem[];
}

export interface DownloadJobItem {
  id: string; // File ID
  notifyNo: string;
  packageName: string;
  fileType: 'HSMT' | 'BBMT PDF' | 'KQLCNT decision' | 'Evaluation report' | 'Contract document' | 'Clarification attachment' | 'Petition attachment' | 'Goods Excel' | 'Contractor CSV';
  fileName: string;
  sourceUrl: string;
  targetFolder: string;
  status: DownloadStatus;
  progressPercent: number;
  sizeBytes: number;
  attempt: number;
  lastError: string | null;
}

export interface MonitorRun {
  runId: string;
  type: 'Import' | 'Scrape' | 'Download' | 'Monitor';
  startedAt: string;
  endedAt: string | null;
  durationSeconds: number;
  totalPackagesCount: number;
  successCount: number;
  partialCount: number;
  failedCount: number;
  status: 'COMPLETED' | 'PARTIAL' | 'FAILED' | 'RUNNING';
  triggeredBy: 'Manual' | 'Cron_30m' | 'System';
}
