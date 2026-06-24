# Sync Flow

This document describes the current bid package sync flow in the codebase.

## Main Entrypoints

### Scheduled Sync

```text
BidPackageSyncScheduler.tick()
-> SyncJobService.runScheduledIfDue()
-> SyncJobServiceImpl.run(true)
```

The scheduler delay is configured by `sync.bid-package.fixed-delay-ms` in the YAML config (`application.yaml` plus the active profile file). `SyncJobServiceImpl` uses YAML config to decide whether the sync system is enabled, and uses `SyncJob.running` to prevent overlapping runs.

### Sync System Config

```text
GET /bid-package-sync-system
-> BidPackageSyncSystemController.getConfig()
```

The sync system is configured from YAML and executed by the scheduler. There is no manual run-now endpoint and no API endpoint for changing scheduler config.

```yaml
sync:
  bid-package:
    enabled: true
    fixed-delay-ms: 60000
    document-download-limit: 50
```

### Manual Single Package Sync

```text
POST /bid-packages/sync/{notifyNo}
-> BidPackageController.syncByNotifyNo()
-> SyncItemService.syncByNotifyNo(notifyNo)
-> BidPackageSyncPipeline.sync(item)
```

This syncs one `notifyNo`. If no `SyncItem` exists yet, one is created with `PENDING` status before syncing.

### Manual Queue Sync

```text
POST /bid-packages/sync-pending
-> SyncItemService.syncPending()

POST /bid-packages/refresh-success
-> SyncItemService.refreshSuccess()
```

`syncPending()` syncs `PENDING` and `FAILED` items. `refreshSuccess()` re-syncs items that were previously `SUCCESS`.

## Sync Job Flow

`SyncJobServiceImpl.run()` is the job-level orchestration.

```text
run()
-> load YAML config and SyncJob runtime state
-> skip if disabled / already running
-> load active SyncSource rows
-> mark SyncJob running
-> importActiveSyncSources(activeSyncSources)
-> syncItemService.syncPending()
-> syncItemService.refreshSuccess()
-> documentDownloadWorkerService.downloadPending(configuredLimit)
-> exportWorkerService.exportSuccessfulPackages()
-> update SyncJob totals/status
```

### Data Written By Job Flow

`SyncJob` is updated with:

```text
running
startedAt
endedAt
lastRunAt
lastStatus
lastError
totalItems
successItems
failedItems
```

## Import Flow

`BidPackageImportServiceImpl.importFolders()` imports folders from active root paths.

```text
SyncSource.path
-> SyncStorageService.listDirectories(path)
-> extract notifyNo from folder name using IB\d{10}
-> create SyncItem if notifyNo does not exist
```

### Imported SyncItem Fields

```text
notifyNo
folderName
syncSource
sourcePath
sourceParentPath
sourceOrder
syncStatus = PENDING
```

Existing `notifyNo` values are skipped.

## Queue Flow

`SyncItemServiceImpl` manages sync queues.

```text
syncPending()
-> findSyncQueue(PENDING, FAILED)
-> syncItems(items)

refreshSuccess()
-> findRefreshQueue(SUCCESS)
-> syncItems(items)
```

`syncItems()` loops each item and calls `BidPackageSyncPipeline.sync(item)`.

## Sync One Item Flow

`BidPackageSyncPipeline.sync(item)` is the main business flow for one bid package.

```text
SyncItem
-> mark PROCESSING
-> PortalSearch.resolve(notifyNo)
-> build PortalSyncContext
-> ensure ProcurementPlan and Contract
-> TbmtSyncService.sync(context)
-> BidOpeningSyncService.sync(context), if available
-> BiddingResultSyncService.sync(context), if available
-> sync clarification and petition documents
-> resolve bid status
-> mark SUCCESS or FAILED
-> return BidPackageSyncPendingItemResult
```

### Processing State

Before sync starts:

```text
SyncItem.syncStatus = PROCESSING
SyncItem.lastAttemptedAt = now
SyncItem.lastError = null
```

### Success State

When the pipeline succeeds:

```text
SyncItem.syncStatus = SUCCESS
SyncItem.lastSyncedAt = now
SyncItem.lastError = null
```

### Failure State

When the pipeline fails hard:

```text
SyncItem.syncStatus = FAILED
SyncItem.lastError = exception message
```

## Portal Flow

All portal clients use `PortalJson` to POST JSON and parse the response body into `JsonNode`.

```text
Portal*.fetch(...)
-> PortalJson.postJson(uri, body)
-> HttpClient.send(...)
-> ObjectMapper.readTree(response.body())
-> JsonNode
```

### PortalJson Error Handling

```text
HTTP 403 / 429 -> PortalBlockedException
HTTP non-2xx -> PortalHttpException
IO / parse / interrupted -> PortalRequestException
```

## PortalSearch Metadata Flow

`PortalSearch` is the metadata resolver for a bid package.

```text
notifyNo
-> POST smart/search
-> find matching JSON object
-> PortalSearchPayloadMapper.toBidApiParams(match)
-> ResolvedBidDetail(detailUrl, BidApiParams)
```

In the main metadata sync flow, `BidPackageSyncPipeline.sync(item)` calls `PortalSearch.resolve(notifyNo)` once and passes `PortalSyncContext` to TBMT, bid opening, and bidding result services. Partial sync endpoints were removed, so these child services only accept `PortalSyncContext`.

`BidApiParams` contains the IDs needed by other portal endpoints:

```text
notifyNo
id
notifyId
inputResultId
bidOpenId
techReqId
bidPreNotifyResultId
bidPreOpenId
processApply
bidMode
bidForm
planNo
stepCode
isInternet
```

These fields drive the rest of the portal calls:

```text
notifyId -> PortalTbmt
bidOpenId / bidPreOpenId -> PortalBidOpening
inputResultId -> PortalBiddingResult
notifyNo + processApply -> PortalDocument
planNo -> ProcurementPlan
```

## TBMT Flow

```text
TbmtSyncServiceImpl.sync(context)
-> PortalTbmt.fetchTbmt(params.notifyId)
-> TbmtPayloadMapper.toPayload(root)
-> upsert Investor
-> upsert ProcurementPlan
-> upsert ContractInfo
-> upsert Bidding
-> upsert BidOpening completion date if present
```

### TBMT Payload

`TbmtPayload` is built from the portal response and used by the service instead of reading raw JSON fields directly.

Important field groups:

```text
ContractInfo fields: bidName, bidPrice, bidEstimatePrice, bidValidityPeriod, contractPeriod, businessStatus
Bidding fields: bidCloseAt, bidOpenAt, feeValue, guaranteeValue, internet
Investor fields: investorCode, investorName, oldInvestorName, mergeInvestorDate
ProcurementPlan fields: planNo, planName
BidOpening field: bidOpeningCompletedAt
```

### Entities Written

```text
Investor
ProcurementPlan
ContractInfo
Bidding
BidOpening
Contract.bidUrl
```

## Bid Opening Flow

```text
BidOpeningSyncServiceImpl.sync(context)
-> validate notifyId and bidOpenId/bidPreOpenId
-> PortalBidOpening.fetchLotOpenDetail(params)
-> BidOpeningPayloadMapper.contractors(root)
-> upsert BidOpening
-> upsert Contractor
-> upsert BiddingContractor
```

### Bid Opening Payload

Each contractor row is mapped into `BidOpeningContractorPayload`:

```text
contractorCode
contractorName
bidPrice
discountRate
bidPriceAfterDiscount
bidGuaranteeAmount
bidGuaranteeValidityPeriod
contractExecutionTime
```

### Optional Flow

If bid opening params cannot be resolved, `BidPackageSyncPipeline` treats this as unavailable data and skips bid opening sync for that item.

Current skip condition:

```text
IllegalStateException message starts with "Cannot resolve bid opening params:"
```

## Bidding Result Flow

```text
BiddingResultSyncServiceImpl.sync(context)
-> validate inputResultId
-> PortalBiddingResult.fetchBiddingResult(params.inputResultId)
-> BiddingResultPayloadMapper.mainPayload(root)
-> upsert BiddingResultSummary
-> enqueue bidding result documents
-> upsert Contractor
-> upsert BiddingResult per contractor
-> sync BiddingResultGoods rows
```

### Bidding Result Payloads

`BiddingResultPayloadMapper` builds:

```text
BiddingResultSummaryPayload
BiddingResultContractorPayload
BiddingResultGoodsPayload
```

Summary payload fields:

```text
resultVersion
notifyVersion
resultStatus
publicDate
decisionNo
decisionDate
decisionAgency
decisionFileId
decisionFileName
evalReportFileInfo
hasWinner
```

Contractor payload fields:

```text
contractorCode
contractorName
taxCode
bidResult
winningPrice
reason
lotPrice
lotFinalPrice
adjustedPrice
evalPrice
techScore
discountRate
contractPeriod
contractPeriodUnit
contractPeriodText
contractExecutionTime
otherContent
```

Goods payload fields:

```text
contractorCode
notifyNo
bidName
lotNo
lotName
goodsName
goodsCode
goodsLabel
yearManufacture
origin
manufacturer
technicalFeatures
unit
quantity
hsCode
winningUnitPrice
amount
deliveryTime
sortOrder
rawItem
```

### Optional Flow

If `inputResultId` cannot be resolved, `BidPackageSyncPipeline` treats this as unavailable data and skips bidding result sync for that item.

Current skip condition:

```text
IllegalStateException message starts with "Cannot resolve inputResultId:"
```

## Document Flow

Documents are handled by `BiddingDocumentServiceImpl`.

### Bidding Result Documents

During KQLCNT sync:

```text
BiddingResultSyncServiceImpl
-> biddingDocumentService.enqueueBiddingResultFiles(contract, main)
```

This enqueues:

```text
decisionFileId / decisionFileName -> KQLCNT_DECISION
evalReportFileInfo -> E_HSDT_EVAL_REPORT
```

### Clarification And Petition Documents

During item sync:

```text
PortalDocument.fetchClarifications(notifyNo, processApply)
-> BiddingDocumentService.enqueueClarificationFiles(contract, root)

PortalDocument.fetchPetitions(notifyNo, processApply)
-> BiddingDocumentService.enqueuePetitionFiles(contract, root)
```

These create/update:

```text
BiddingDocument
BidClarification
BidClarificationContent
BidPetition
BidPetitionContent
```

Clarification/petition sync errors are logged and treated as empty document stats, not as hard item failures.

### Document Statuses

```text
PENDING
DOWNLOADING
SUCCESS
FAILED
```

## Document Download Worker Flow

Document downloads are separated from metadata sync. `BidPackageSyncPipeline.sync(item)` only enqueues document metadata.

```text
Sync job metadata phase
-> BiddingDocument rows with downloadStatus = PENDING
-> DocumentDownloadWorkerService.downloadPending(batchSize)
-> BiddingDocumentService.downloadPending(batchSize)
```

Manual endpoint:

```text
POST /documents/download-pending?limit=10
-> DocumentDownloadWorkerService.downloadPending(limit)
```

Current worker implementation delegates to `BiddingDocumentService.downloadPending(...)`. A later transaction refactor should split claim/download/update so network and file I/O do not run inside a long transaction.

## File Download Internals

```text
BiddingDocumentService.downloadPending(contract, sourcePath, 50)
-> FileService.ensureGatewayReady()
-> FileService.download(FileDownloadRequest)
-> SyncStorageService.write(...)
-> update BiddingDocument storagePath/fileSize/downloadedAt/status
```

`FileServiceImpl` does not call the muasamcong portal directly. It calls the local Windows gateway:

```text
GET {gateway.url}/health
GET {gateway.url}/download?fileId=...&fileName=...
```

The gateway then calls the external download agent and streams the file back to the application.

## Export Worker Flow

Generated file export is separated from metadata sync. `BidPackageSyncPipeline.sync(item)` does not export files.

```text
ExportWorkerService.exportSuccessfulPackages()
-> load SUCCESS SyncItem rows
-> AutoDownloadExportService.exportGeneratedFiles(notifyNo, sourcePath)
```

There is no standalone export controller. The sync job invokes `ExportWorkerService` directly after metadata sync and document download. Current export worker uses successful `SyncItem` rows as the export queue. There is not yet a dedicated export status column/entity.

## Export Internals

For each package:

```text
AutoDownloadExportService.exportGeneratedFiles(notifyNo, sourcePath)
```

This exports generated files into the configured auto-download folder structure.

## Bid Status Flow

After sync steps complete, `BidPackageSyncPipeline` resolves lifecycle status:

```text
BidStatusResolver.resolveStatus(
    hasContractInformation,
    hasContractorSelectionResult,
    bidClosingTime,
    bidOpenTime
)
```

Current sync item flow passes:

```text
hasContractInformation = false
hasContractorSelectionResult = biddingResult != null && biddingResult.hasContractorSelectionResult()
bidClosingTime = TbmtIngestResult.bidClosingTime
bidOpenTime = TbmtIngestResult.bidOpenTime
```

When status changes:

```text
Contract.bidStatus is updated
ContractStatusHistory row is inserted
source = SYNC_PACKAGE
```

## Tracking Flow

Tracking is read-only and separate from sync.

```text
GET /bid-packages/tracking
-> SyncItemService.searchTracking(search, status, kpiFilter, pageable)
-> BidPackageTrackingReader.searchTracking(...)
-> SyncItemRepository.searchTracking(...)
-> load related Contract/ContractInfo/Bidding/BiddingResult/BiddingDocument data
-> build BidPackageTrackingDto
```

Tracking does not call the portal, does not sync data, and does not perform SMB filesystem checks. `folderExists` is derived from whether a source path is present so the list page stays responsive even when SMB is slow or unavailable.

## Current Refactor State

The portal payload layer now uses builder DTOs for these flows:

```text
PortalSearchPayloadMapper -> BidApiParams
TbmtPayloadMapper -> TbmtPayload
BidOpeningPayloadMapper -> BidOpeningContractorPayload
BiddingResultPayloadMapper -> BiddingResultSummaryPayload
BiddingResultPayloadMapper -> BiddingResultContractorPayload
BiddingResultPayloadMapper -> BiddingResultGoodsPayload
```

`SyncItemServiceImpl` now manages queue-level operations, manual sync entrypoint, and folder path updates. It delegates one-item sync orchestration to `BidPackageSyncPipeline` and tracking reads to `BidPackageTrackingReader`.

## Recommended Next Code Split

The next split is the document download transaction boundary:

```text
BiddingDocumentServiceImpl.downloadPending(...)
-> claim pending documents in a short DB transaction
-> download files outside DB transaction
-> update document success/failed in a new DB transaction
```
