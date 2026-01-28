# Investment Package - Used API Endpoints

This document lists all Investment Service API endpoints that are actively used in the `investment-core` package.

## Asset Universe API (AssetUniverseApi)

### Markets
- `GET /service-api/v2/asset/markets/{code}` - Get market by code
- `POST /service-api/v2/asset/markets` - Create market  
- `PUT /service-api/v2/asset/markets/{code}` - Update market

### Assets
- `GET /service-api/v2/asset/assets/{assetIdentifier}` - Get asset by identifier (ISIN_Market_Currency)
- `POST /service-api/v2/asset/assets` - Create asset (via CustomIntegrationApiService)
- `PATCH /service-api/v2/asset/assets/{uuid}` - Patch asset (via InvestmentRestAssetUniverseService.patchAsset - for logo upload)
- `GET /service-api/v2/asset/assets` - List assets with response spec (for intraday prices)

### Asset Categories
- `GET /service-api/v2/asset/asset-categories` - List asset categories
- `POST /service-api/v2/asset/asset-categories` - Create asset category
- `PUT /service-api/v2/asset/asset-categories/{uuid}` - Update asset category
- `PATCH /service-api/v2/asset/asset-categories/{uuid}/` - Partial update asset category (for image upload)

### Asset Category Types
- `GET /service-api/v2/asset/asset-category-types` - List asset category types
- `POST /service-api/v2/asset/asset-category-types` - Create asset category type
- `PUT /service-api/v2/asset/asset-category-types/{uuid}` - Update asset category type

### Market Special Days
- `GET /service-api/v2/asset/market-special-days` - List market special days
- `POST /service-api/v2/asset/market-special-days` - Create market special day
- `PUT /service-api/v2/asset/market-special-days/{uuid}` - Update market special day

### Asset Prices
- `GET /service-api/v2/asset/assets/{assetIdentifier}/prices/close` - List asset close prices
- `POST /service-api/v2/asset/assets/{assetIdentifier}/prices/close` - Create asset close prices (batch)
- `POST /service-api/v2/asset/assets/{assetIdentifier}/prices/intraday` - Create intraday asset prices

## Client API (ClientApi)

- `POST /service-api/v2/client/clients` - Create client
- `GET /service-api/v2/client/clients` - List clients (with filters: internalUserId)
- `GET /service-api/v2/client/clients/{uuid}` - Get client by UUID
- `PATCH /service-api/v2/client/clients/{uuid}` - Patch client (partial update)
- `PUT /service-api/v2/client/clients/{uuid}` - Update client (full update)

## Investment Products API (InvestmentProductsApi)

- `GET /service-api/v2/investment-product/portfolio-products` - List portfolio products
- `POST /service-api/v2/investment-product/portfolio-products` - Create portfolio product
- `PATCH /service-api/v2/investment-product/portfolio-products/{uuid}` - Patch portfolio product

## Portfolio API (PortfolioApi)

- `GET /service-api/v2/portfolio/portfolios` - List portfolios (with filters: externalId)
- `POST /service-api/v2/portfolio/portfolios` - Create portfolio
- `PATCH /service-api/v2/portfolio/portfolios/{uuid}` - Patch portfolio

## Financial Advice API (FinancialAdviceApi)

### Model Portfolios
- `GET /service-api/v2/financial-advice/model-portfolios` - List model portfolios (with filters: name, riskLevel)
- `POST /service-api/v2/financial-advice/model-portfolios` - Create model portfolio (via CustomIntegrationApiService)
- `PATCH /service-api/v2/financial-advice/model-portfolios/{uuid}` - Patch model portfolio (via CustomIntegrationApiService)

## Allocations API (AllocationsApi)

- `GET /service-api/v2/portfolio/portfolios/{portfolioUuid}/allocations` - List portfolio allocations
- `DELETE /service-api/v2/portfolio/portfolios/{portfolioUuid}/allocations/{valuationDate}` - Delete portfolio allocation by valuation date
- `POST /service-api/v2/portfolio/portfolios/{portfolioUuid}/allocations` - Create portfolio allocation (via CustomIntegrationApiService)

## Payments API (PaymentsApi)

- `GET /service-api/v2/payment/deposits` - List deposits (with filters: portfolio UUID)
- `POST /service-api/v2/payment/deposits` - Create deposit

## Investment API (InvestmentApi)

- `GET /service-api/v2/investment/orders` - List orders (with filters: assetKey)
- `POST /service-api/v2/investment/orders` - Create order

## Content API (ContentApi) - News/Content Management

- `GET /service-api/v2/content/entries` - List content entries
- `POST /service-api/v2/content/entries` - Create content entry
- `PATCH /service-api/v2/content/entries/{uuid}/` - Patch content entry (for thumbnail upload)

## Async Bulk Groups API (AsyncBulkGroupsApi)

- `GET /service-api/v2/async/bulk-groups/{uuid}` - Get bulk group status

---

## Summary by Service

### InvestmentAssetUniverseService
- Uses: AssetUniverseApi (markets, assets, categories, category types, special days, prices)
- Uses: InvestmentRestAssetUniverseService (multipart uploads for logos)
- Uses: CustomIntegrationApiService (asset creation with custom logic)

### InvestmentClientService  
- Uses: ClientApi (create, list, get, patch, update clients)

### InvestmentPortfolioService
- Uses: InvestmentProductsApi (portfolio products)
- Uses: PortfolioApi (portfolios)
- Uses: PaymentsApi (deposits)

### InvestmentModelPortfolioService
- Uses: FinancialAdviceApi (model portfolios)
- Uses: CustomIntegrationApiService (model portfolio creation/patching)

### InvestmentPortfolioAllocationService
- Uses: AllocationsApi (portfolio allocations)
- Uses: InvestmentApi (orders)
- Uses: AssetUniverseApi (asset prices)

### InvestmentAssetPriceService
- Uses: AssetUniverseApi (asset close prices)

### InvestmentIntradayAssetPriceService
- Uses: AssetUniverseApi (list assets, intraday prices)

### InvestmentRestNewsContentService
- Uses: ContentApi (news/content entries)

### AsyncTaskService
- Uses: AsyncBulkGroupsApi (bulk operation status)

### WorkDayService
- No external API calls (utility service)

---

## Notes

1. **Custom Integration API Service**: This is a custom wrapper service (marked as deprecated since 8.6.0) that handles multipart/form-data requests for:
   - Asset creation (POST /service-api/v2/asset/assets)
   - Model portfolio creation/patching with image uploads
   - Portfolio allocation creation

2. **REST Template Services**: Two services handle multipart uploads that generated API clients can't handle properly:
   - `InvestmentRestAssetUniverseService` - For asset and asset category logo uploads
   - `InvestmentRestNewsContentService` - For content entry thumbnail uploads

3. **Upsert Pattern**: Most services implement an upsert pattern:
   - Try to GET/LIST existing entity
   - If found (or 200 OK), PATCH/PUT to update
   - If not found (404), POST to create

4. **Endpoint Patterns**:
   - All endpoints follow the pattern: `/service-api/v2/{domain}/{resource}`
   - Multipart upload endpoints use PATCH with `multipart/form-data` content type
   - Most list endpoints support pagination and filtering

5. **Error Handling**: All services handle WebClientResponseException with special logic for 404 (Not Found) responses to implement upsert patterns.
