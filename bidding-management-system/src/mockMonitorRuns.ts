/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import { MonitorRun } from './types';

export const mockMonitorRuns: MonitorRun[] = [
  {
    runId: 'RUN-20260613-06',
    type: 'Monitor',
    startedAt: '2026-06-13 04:50:00',
    endedAt: null, // Still running
    durationSeconds: 300,
    totalPackagesCount: 20,
    successCount: 16,
    partialCount: 2,
    failedCount: 2,
    status: 'RUNNING',
    triggeredBy: 'Cron_30m'
  },
  {
    runId: 'RUN-20260613-05',
    type: 'Download',
    startedAt: '2026-06-13 04:00:00',
    endedAt: '2026-06-13 04:05:12',
    durationSeconds: 312,
    totalPackagesCount: 8,
    successCount: 6,
    partialCount: 1,
    failedCount: 1,
    status: 'PARTIAL',
    triggeredBy: 'System'
  },
  {
    runId: 'RUN-20260613-04',
    type: 'Scrape',
    startedAt: '2026-06-13 03:00:00',
    endedAt: '2026-06-13 03:08:45',
    durationSeconds: 525,
    totalPackagesCount: 15,
    successCount: 13,
    partialCount: 1,
    failedCount: 1,
    status: 'PARTIAL',
    triggeredBy: 'Cron_30m'
  },
  {
    runId: 'RUN-20260613-03',
    type: 'Import',
    startedAt: '2026-06-13 01:22:00',
    endedAt: '2026-06-13 01:23:40',
    durationSeconds: 100,
    totalPackagesCount: 5,
    successCount: 5,
    partialCount: 0,
    failedCount: 0,
    status: 'COMPLETED',
    triggeredBy: 'Manual'
  },
  {
    runId: 'RUN-20260612-02',
    type: 'Monitor',
    startedAt: '2026-06-12 23:30:00',
    endedAt: '2026-06-12 23:44:20',
    durationSeconds: 860,
    totalPackagesCount: 20,
    successCount: 18,
    partialCount: 1,
    failedCount: 1,
    status: 'COMPLETED',
    triggeredBy: 'Cron_30m'
  },
  {
    runId: 'RUN-20260612-01',
    type: 'Scrape',
    startedAt: '2026-06-12 18:00:00',
    endedAt: '2026-06-12 18:15:30',
    durationSeconds: 930,
    totalPackagesCount: 18,
    successCount: 15,
    partialCount: 2,
    failedCount: 1,
    status: 'FAILED',
    triggeredBy: 'Cron_30m'
  }
];
