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

## Integration API (CustomIntegrationApiService)

**Note:** This service is deprecated since 8.6.0 and uses integration-api endpoints instead of service-api.

### Assets
- `POST /integration-api/v2/asset/assets/` - Create asset (custom implementation)

### Model Portfolios
- `POST /integration-api/v2/advice-engines/model-portfolio/model_portfolios/` - Create model portfolio (with optional image upload via multipart/form-data)
- `PATCH /integration-api/v2/advice-engines/model-portfolio/model_portfolios/{uuid}/` - Patch model portfolio (with optional image upload via multipart/form-data)

### Portfolio Allocations
- `POST /integration-api/v2/portfolios/{portfolio_uuid}/allocations/` - Create portfolio allocation

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

1. **Custom Integration API Service**: This is a custom wrapper service (marked as deprecated since 8.6.0) that uses `/integration-api/v2/` endpoints instead of `/service-api/v2/` endpoints. It handles:
   - Asset creation via POST `/integration-api/v2/asset/assets/`
   - Model portfolio creation/patching with optional image uploads via multipart/form-data
   - Portfolio allocation creation

2. **REST Template Services**: Two services handle multipart uploads that generated API clients can't handle properly:
   - `InvestmentRestAssetUniverseService` - For asset and asset category logo uploads
   - `InvestmentRestNewsContentService` - For content entry thumbnail uploads

3. **Upsert Pattern**: Most services implement an upsert pattern:
   - Try to GET/LIST existing entity
   - If found (or 200 OK), PATCH/PUT to update
   - If not found (404), POST to create

4. **Endpoint Patterns**:
   - Service API endpoints follow: `/service-api/v2/{domain}/{resource}`
   - Integration API endpoints follow: `/integration-api/v2/{domain}/{resource}` (used by CustomIntegrationApiService)
   - Multipart upload endpoints use PATCH or POST with `multipart/form-data` content type
   - Most list endpoints support pagination and filtering

5. **Error Handling**: All services handle WebClientResponseException with special logic for 404 (Not Found) responses to implement upsert patterns.

---

## Total Endpoint Count

### Service API Endpoints: 42
- Asset Universe API: 17 endpoints
- Client API: 5 endpoints
- Investment Products API: 3 endpoints
- Portfolio API: 3 endpoints
- Financial Advice API: 3 endpoints
- Allocations API: 3 endpoints
- Content API: 3 endpoints
- Payments API: 2 endpoints
- Investment API: 2 endpoints
- Async Bulk Groups API: 1 endpoint

### Integration API Endpoints: 4
- Asset creation: 1 endpoint
- Model portfolios: 2 endpoints
- Portfolio allocations: 1 endpoint

### Grand Total: 46 unique API endpoints

By HTTP Method:
- GET: 17 endpoints
- POST: 19 endpoints (15 service-api + 4 integration-api)
- PATCH: 8 endpoints (7 service-api + 1 integration-api)
- PUT: 5 endpoints
- DELETE: 1 endpoint
