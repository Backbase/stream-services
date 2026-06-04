# Investment Package - Used API Endpoints

This document lists all Investment Service API endpoints that are actively used in the `investment-core` package.

Generated API clients live under `com.backbase.investment.api.service.v1` (reactive WebClient) and `com.backbase.investment.api.service.sync.v1` (blocking RestTemplate for multipart). All paths below are under `/service-api/v2/`.

---

## Asset Universe API (AssetUniverseApi + InvestmentRestAssetUniverseService)

### Markets
- `GET /service-api/v2/asset/markets/{code}/` - Get market by code
- `POST /service-api/v2/asset/markets/` - Create market
- `PUT /service-api/v2/asset/markets/{code}/` - Update market

### Assets
- `GET /service-api/v2/asset/assets/{asset_identifier}/` - Get asset by identifier (ISIN_Market_Currency)
- `POST /service-api/v2/asset/assets/` - Create asset with optional logo (`InvestmentRestAssetUniverseService`, multipart/form-data)
- `PATCH /service-api/v2/asset/assets/{asset_identifier}/` - Patch asset with optional logo (`InvestmentRestAssetUniverseService`, multipart/form-data)
- `GET /service-api/v2/asset/assets/` - List assets with expand (`market`, `latest_price`) for intraday price generation

### Asset Categories
- `GET /service-api/v2/asset/asset-categories/` - List asset categories
- `POST /service-api/v2/asset/asset-categories/` - Create asset category with optional image (`InvestmentRestAssetUniverseService`, multipart/form-data)
- `PATCH /service-api/v2/asset/asset-categories/{uuid}/` - Partial update asset category with optional image (`InvestmentRestAssetUniverseService`, multipart/form-data)

### Asset Category Types
- `GET /service-api/v2/asset/asset-category-types/` - List asset category types
- `POST /service-api/v2/asset/asset-category-types/` - Create asset category type
- `PUT /service-api/v2/asset/asset-category-types/{uuid}/` - Update asset category type

### Market Special Days
- `GET /service-api/v2/asset/market-special-days/` - List market special days
- `POST /service-api/v2/asset/market-special-days/` - Create market special day
- `PUT /service-api/v2/asset/market-special-days/{uuid}/` - Update market special day

### Asset Prices
- `GET /service-api/v2/asset/prices-close/` - List asset close prices
- `POST /service-api/v2/asset/prices-close/bulk-create/` - Bulk create asset close prices (returns async bulk group results)
- `POST /service-api/v2/asset/prices-intraday/bulk-create/` - Bulk create intraday asset prices (returns async bulk group results)

---

## Client API (ClientApi)

- `POST /service-api/v2/account/clients/` - Create client
- `GET /service-api/v2/account/clients/` - List clients (with filters: internalUserId)
- `GET /service-api/v2/account/clients/{uuid}/` - Get client by UUID
- `PATCH /service-api/v2/account/clients/{uuid}/` - Patch client (partial update; used in upsert flow)
- `PUT /service-api/v2/account/clients/{uuid}/` - Update client (full update; public API on `InvestmentClientService`)

---

## Investment Products API (InvestmentProductsApi + InvestmentRestProductPortfolioService)

- `GET /service-api/v2/products/portfolio/` - List portfolio products
- `POST /service-api/v2/products/portfolio/` - Create portfolio product (JSON via `InvestmentProductsApi`)
- `PUT /service-api/v2/products/portfolio/{uuid}/` - Update portfolio product (JSON via `updatePortfolioProduct`)
- `PATCH /service-api/v2/products/portfolio/{uuid}/` - Patch portfolio product with optional image (`InvestmentRestProductPortfolioService`, multipart/form-data when `ingestImages` is enabled)

---

## Portfolio API (PortfolioApi)

- `GET /service-api/v2/portfolios/` - List portfolios (with filters: externalId)
- `POST /service-api/v2/portfolios/` - Create portfolio
- `PATCH /service-api/v2/portfolios/{uuid}/` - Patch portfolio

---

## Portfolio Trading Accounts API (PortfolioTradingAccountsApi)

- `GET /service-api/v2/portfolio-trading-accounts/` - List portfolio trading accounts (filter: externalAccountId)
- `POST /service-api/v2/portfolio-trading-accounts/` - Create portfolio trading account
- `PATCH /service-api/v2/portfolio-trading-accounts/{uuid}/` - Patch portfolio trading account

---

## Financial Advice API (FinancialAdviceApi + InvestmentRestModelPortfolioService)

### Model Portfolios
- `GET /service-api/v2/advice-engines/model-portfolio/model_portfolios/` - List model portfolios (with filters: name, riskLevel)
- `POST /service-api/v2/advice-engines/model-portfolio/model_portfolios/` - Create model portfolio with optional image (`InvestmentRestModelPortfolioService`, multipart/form-data)
- `PUT /service-api/v2/advice-engines/model-portfolio/model_portfolios/{uuid}/` - Update model portfolio with optional image (`InvestmentRestModelPortfolioService`, multipart/form-data; used for upsert when image/data changed)

---

## Allocations API (AllocationsApi)

- `GET /service-api/v2/portfolios/{portfolio_uuid}/allocations/` - List portfolio allocations
- `DELETE /service-api/v2/portfolios/{portfolio_uuid}/allocations/{valuation_date}/` - Delete portfolio allocation by valuation date
- `POST /service-api/v2/portfolios/{portfolio_uuid}/allocations/` - Create portfolio allocation

---

## Payments API (PaymentsApi)

- `GET /service-api/v2/deposits/` - List deposits (with filters: portfolio UUID)
- `POST /service-api/v2/deposits/` - Create deposit

---

## Investment / Broker API (InvestmentApi)

- `GET /service-api/v2/broker/orders/` - List orders (with filters: assetKey, portfolio)
- `POST /service-api/v2/broker/orders/` - Create order

---

## Content API (ContentApi) - News, Documents, Tags

Uses blocking `com.backbase.investment.api.service.sync.v1.ContentApi` for tag/entry CRUD and `ApiClient.invokeAPI` for multipart document/entry uploads.

### Content Entry Tags (market news)
- `GET /service-api/v2/content/entry-tags/` - List content entry tags
- `POST /service-api/v2/content/entry-tags/` - Create content entry tag
- `PATCH /service-api/v2/content/entry-tags/{code}/` - Patch content entry tag

### Content Entries (market news)
- `GET /service-api/v2/content/entries/` - List content entries
- `POST /service-api/v2/content/entries/` - Create content entry
- `PATCH /service-api/v2/content/entries/{uuid}/` - Patch content entry with optional thumbnail (`InvestmentRestNewsContentService`, multipart/form-data)

### Content Document Tags
- `GET /service-api/v2/content/document-tags/` - List document tags
- `POST /service-api/v2/content/document-tags/` - Create document tag
- `PATCH /service-api/v2/content/document-tags/{code}/` - Patch document tag

### Content Documents
- `GET /service-api/v2/content/documents/` - List content documents
- `POST /service-api/v2/content/documents/` - Create content document with file (`InvestmentRestDocumentContentService`, multipart/form-data)
- `PATCH /service-api/v2/content/documents/{uuid}/` - Patch content document with file (`InvestmentRestDocumentContentService`, multipart/form-data)

---

## Currency API (CurrencyApi)

- `GET /service-api/v2/currencies/` - List currencies
- `POST /service-api/v2/currencies/` - Create currency
- `PUT /service-api/v2/currencies/{code}/` - Update currency

---

## Risk Assessment API (RiskAssessmentApi)

### Client Risk Assessments
- `GET /service-api/v2/account/clients/{client_uuid}/risk-assessments/` - List risk assessments for client
- `POST /service-api/v2/account/clients/{client_uuid}/risk-assessments/` - Create risk assessment
- `PATCH /service-api/v2/account/clients/{client_uuid}/risk-assessments/{uuid}/` - Patch risk assessment

### Risk Questionnaire
- `GET /service-api/v2/risk/questions/` - List risk questions
- `POST /service-api/v2/risk/questions/` - Create risk question
- `PATCH /service-api/v2/risk/questions/{uuid}/` - Patch risk question
- `GET /service-api/v2/risk/choices/` - List risk choices
- `POST /service-api/v2/risk/choices/` - Create risk choice
- `PATCH /service-api/v2/risk/choices/{uuid}/` - Patch risk choice

---

## Async Bulk Groups API (AsyncBulkGroupsApi)

- `GET /service-api/v2/bulkgroup/{uuid}/` - Get bulk group status (poll after bulk price creation)

---

## Summary by Service

### InvestmentAssetUniverseService
- Uses: `AssetUniverseApi` (markets, assets lookup, categories list, category types, special days)
- Uses: `InvestmentRestAssetUniverseService` (multipart create/patch for assets and asset categories with logos/images)

### InvestmentClientService
- Uses: `ClientApi` (create, list, get, patch, update clients)

### InvestmentPortfolioProductService
- Uses: `InvestmentProductsApi` (list, create, update portfolio products)
- Uses: `InvestmentRestProductPortfolioService` (multipart patch when product images are ingested)
- Uses: `InvestmentModelPortfolioService` (model portfolio upsert for product linkage)

### InvestmentPortfolioService
- Uses: `PortfolioApi` (portfolios)
- Uses: `PaymentsApi` (deposits)
- Uses: `PortfolioTradingAccountsApi` (portfolio trading accounts)

### InvestmentModelPortfolioService
- Uses: `FinancialAdviceApi` (list model portfolios)
- Uses: `InvestmentRestModelPortfolioService` (multipart create/update model portfolios with images)

### InvestmentPortfolioAllocationService
- Uses: `AllocationsApi` (portfolio allocations)
- Uses: `InvestmentApi` (broker orders)
- Uses: `AssetUniverseApi` (list close prices for allocation generation)

### InvestmentAssetPriceService
- Uses: `AssetUniverseApi` (list close prices, bulk create close prices)

### InvestmentIntradayAssetPriceService
- Uses: `AssetUniverseApi` (list assets with expand, bulk create intraday prices)

### InvestmentCurrencyService
- Uses: `CurrencyApi` (list, create, update currencies)

### InvestmentRiskAssessmentService
- Uses: `RiskAssessmentApi` (client risk assessments)

### InvestmentRiskQuestionaryService
- Uses: `RiskAssessmentApi` (risk questions and choices)

### InvestmentRestNewsContentService
- Uses: `ContentApi` (entry tags, content entries)
- Uses: `ApiClient` (multipart thumbnail patch on entries)

### InvestmentRestDocumentContentService
- Uses: `ContentApi` (document tags, list documents)
- Uses: `ApiClient` (multipart create/patch documents)

### AsyncTaskService
- Uses: `AsyncBulkGroupsApi` (bulk operation status after price ingestion)

### WorkDayService
- No external API calls (utility service)

---

## Notes

1. **No Integration API usage**: `CustomIntegrationApiService` and `/integration-api/v2/` endpoints were removed. Create/update operations that previously used the integration API now use `/service-api/v2/` via generated clients or RestTemplate `ApiClient` wrappers.

2. **REST Template Services** (blocking `ApiClient`, `multipart/form-data`): Used where generated reactive clients cannot serialize multipart correctly:
   - `InvestmentRestAssetUniverseService` - Asset and asset category logos/images
   - `InvestmentRestModelPortfolioService` - Model portfolio images (create POST, update PUT)
   - `InvestmentRestProductPortfolioService` - Portfolio product images (patch)
   - `InvestmentRestNewsContentService` - Content entry thumbnails
   - `InvestmentRestDocumentContentService` - Content document file uploads

3. **Bulk price ingestion**: Close and intraday prices are submitted via bulk-create endpoints, which return async bulk group UUIDs polled by `AsyncTaskService`.

4. **Upsert pattern**: Most services implement an upsert pattern:
   - Try to GET/LIST existing entity
   - If found (or 200 OK), PATCH/PUT to update when data changed
   - If not found (404 or empty list), POST to create

5. **Endpoint path conventions** (current OpenAPI spec):
   - Portfolios: `/service-api/v2/portfolios/` (not `/portfolio/portfolios`)
   - Portfolio products: `/service-api/v2/products/portfolio/` (not `/investment-product/portfolio-products`)
   - Clients: `/service-api/v2/account/clients/`
   - Orders: `/service-api/v2/broker/orders/`
   - Deposits: `/service-api/v2/deposits/`
   - Async status: `/service-api/v2/bulkgroup/{uuid}/`
   - Trailing slashes are used on most resource paths in the generated clients

6. **Error handling**: Services handle `WebClientResponseException` with special logic for 404 (Not Found) to implement upsert patterns. Retry with backoff is applied for 409/503 on selected asset-universe operations.

---

## Total Endpoint Count

### Service API Endpoints: 59

| API area | Count |
|----------|-------|
| Asset Universe | 17 |
| Client | 5 |
| Investment Products | 4 |
| Portfolio | 3 |
| Portfolio Trading Accounts | 3 |
| Financial Advice (model portfolios) | 3 |
| Allocations | 3 |
| Payments | 2 |
| Broker (orders) | 2 |
| Content (entries + documents + tags) | 12 |
| Currency | 3 |
| Risk Assessment | 9 |
| Async Bulk Groups | 1 |

### Integration API Endpoints: 0

`CustomIntegrationApiService` is no longer present in `investment-core`.

### Grand Total: 59 unique API endpoints (path + HTTP method)

By HTTP method:
- GET: 24
- POST: 22
- PATCH: 11
- PUT: 8
- DELETE: 1
