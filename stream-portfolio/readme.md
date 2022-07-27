NOTE:
```
    This is an initial implementation. 
    Ingestioin works only on clear DB. Improvement is required!
```

# Portfolio Ingestion
Main core functionality is in `portfolio-core`
The bootstrap task config is in `portfolio-bootstrap-task`

#Bootstrap Ingestion Configuration
```
bootstrap:
  wealthBundles:
    - regions:
        - region:
            name: Asia
            code: 142
          countries:
            - name: Azerbaijan
              code: AZ
        - region:
            name: Europe
            code: 150
          countries:
            - name: Belgium
              code: BE
            - name: Ukraine
              code: UA
      assetClasses:
        - assetClass:
            name: Equity1
            code: assetClasses1
          subAssetClasses:
            - name: Technology Sector 11
              code: subAssetClasses11
            - name: Technology Sector 12
              code: subAssetClasses12
        - assetClass:
            name: Equity2
            code: assetClasses2
          subAssetClasses:
            - name: Technology Sector 21
              code: subAssetClasses21
            - name: Technology Sector 22
              code: subAssetClasses22
      instruments:
        - instrument:
            id: externalId
            name: Apple
            instrumentNameLong: Apple
            code: US0378331005
            instrumentCodeCustom: US0378331005
            price:
              amount: 123.24
              currencyCode: USD
            subAssetClassCode: subAssetClasses11
            country: UA
            ticker: AAPL
            exchange: NASDAQ
            keyStatistics:
              exchangeRate: 1.21
              changeInNetAssetValue: 7.81
              fundStatus: open-ended fund
              totalAssets: 134.3
              trailingTwelveMonthsYield: 1.4
              oneYearReturn: 68.97
              effectiveDuration: 4.51
              portfolioAssetsAllocation: Equity
              turnover: 30
              creditRating: AAA
              priceToEarningsRatio: 37.59
              estPriceToEarningsRatio: 31.06
              priceToEarningsToGrowthRatio: 3.09
              sharesOutstanding: 17.002
              priceToBookRatio: 32.07
              earningsPerShare: 1.39
              dividendYield: 0.66
              lastDividendReported: 0.2
              nextEarningsAnnouncement: 2021-01-26
              coupon: 5.63
              maturity: 2028-01-04
              yieldToMaturity: 0
              duration: 7.63
              couponFrequency: annually
              unrealizedPLPct: 4.14
              unrealizedPL: 4.14
              accruedInterestOnBond: 4.14
              additionalKeyStatistics:
                purchasePrice: 118.34
                totalAssets: 50
                coupon: false
            additions:
              someKey: someValue
          history-prices:
            - price:
                amount: 123.24
                currencyCode: USD
              date: 2020-01-01
              priceType: OPEN
            - price:
                amount: 125.123
                currencyCode: USD
              date: 2020-02-01
              priceType: CLOSE
            - price:
                amount: 110.99
                currencyCode: USD
              date: 2020-07-02
              priceType: CLOSE
            - price:
                amount: 115.99
                currencyCode: USD
              date: 2020-06-02
              priceType: OPEN
            - price:
                amount: 117.99
                currencyCode: USD
              date: 2020-05-02
              priceType: OPEN
            - price:
                amount: 126.99
                currencyCode: USD
              date: 2020-04-02
              priceType: CLOSE
            - price:
                amount: 122.99
                currencyCode: USD
              date: 2020-03-02
              priceType: OPEN
      portfolios:
        - portfolio:
            code: portfolioExternalId1
            name: 15 Years Long Strategy
            alias: 15 Years Long Strategy
            iban: CY 17 002 00128 0000001200527600
            riskLevel: riskLevel
            valuation:
              amount: 456231857.22
              currencyCode: EUR
            ytdPerformance: 2.3
            ytdPerformanceValue:
              amount: 10493332.72
              currencyCode: EUR
            mtdPerformance: -5.6
            mtdPerformanceValue:
              amount: -25548984
              currencyCode: EUR
            inValue:
              amount: 10342875
              currencyCode: EUR
            outValue:
              amount: 5500000
              currencyCode: EUR
            netValue:
              amount: 4842875
              currencyCode: EUR
            managers:
              - id: p1fejk-3tji5o495803jr-43
                name: Kevin Clark
                link: link
            attorneys:
              - id: p1klsjr3904-43nf389-rej2189
                name: John Doe
                link: link
            additions:
              someKey: someValue
          subPortfolios:
            - code: subPortfolioId1
              name: Equity 15Y Long Strategy
              valuation:
                amount: 205304335.75
                currencyCode: EUR
              performance: 33.4
              percentOfParent: 45
          allocations:
            - allocationType: BY_CURRENCY
              classifierType: CURRENCY
              classifierName: USD
              allocationMaxPct: 5
              allocationMinPct: 0
              allocationPct: 3
              valuation:
                amount: 30.06
                currencyCode: EUR
              allocations:
                - classifierType: ASSET_CLASS
                  classifierName: Bonds
                  allocationPct: 5.62
                  valuation:
                    amount: 77.06
                    currencyCode: EUR
                - classifierType: ASSET_CLASS
                  classifierName: Cash
                  allocationPct: 89.82
                  valuation:
                    amount: 99.99
                    currencyCode: EUR
                - classifierType: ASSET_CLASS
                  classifierName: Equity
                  allocationPct: 89.82
                  valuation:
                    amount: 99.99
                    currencyCode: EUR
                - classifierType: ASSET_CLASS
                  classifierName: Real Estate
                  allocationPct: 79.82
                  valuation:
                    amount: 79.99
                    currencyCode: EUR
            - allocationType: BY_CURRENCY
              classifierType: CURRENCY
              classifierName: JPY
              allocationMaxPct: 5
              allocationMinPct: 0
              allocationPct: 3
              valuation:
                amount: 30.06
                currencyCode: JPY
              allocations:
                - classifierType: ASSET_CLASS
                  classifierName: Bonds
                  allocationPct: 5.62
                  valuation:
                    amount: 77.06
                    currencyCode: JPY
                - classifierType: ASSET_CLASS
                  classifierName: Cash
                  allocationPct: 89.82
                  valuation:
                    amount: 99.99
                    currencyCode: JPY
                - classifierType: ASSET_CLASS
                  classifierName: Equity
                  allocationPct: 89.82
                  valuation:
                    amount: 99.99
                    currencyCode: JPY
                - classifierType: ASSET_CLASS
                  classifierName: Real Estate
                  allocationPct: 79.82
                  valuation:
                    amount: 79.99
                    currencyCode: JPY
          hierarchies:
            - itemType: SUB_PORTFOLIO
              externalId: subPortfolioId1
              name: Equity 15Y Long Strategy
              percentOfParent: 55
              percentOfPortfolio: 5
              performance: 10
              unrealizedPL:
                currencyCode: EUR
                amount: 50
              accruedInterest:
                currencyCode: EUR
                amount: 50
              valuation:
                currencyCode: EUR
                amount: 50
              items:
                - itemType: ASSET_CLASS
                  externalId: assetClasses1
                  name: Bond
                  percentOfParent: 55
                  percentOfPortfolio: 5
                  performance: 10
                  unrealizedPL:
                    currencyCode: EUR
                    amount: 50
                  accruedInterest:
                    currencyCode: EUR
                    amount: 50
                  valuation:
                    currencyCode: EUR
                    amount: 50
                  items:
                    - itemType: REGION
                      externalId: 150
                      name: Europe
                      percentOfParent: 55
                      percentOfPortfolio: 5
                      performance: 10
                      unrealizedPL:
                        currencyCode: EUR
                        amount: 50
                      accruedInterest:
                        currencyCode: EUR
                        amount: 50
                      valuation:
                        currencyCode: EUR
                        amount: 50
                      items:
                        - itemType: COUNTRY
                          externalId: UA
                          name: Ukraine
                          percentOfParent: 55
                          percentOfPortfolio: 5
                          performance: 10
                          unrealizedPL:
                            currencyCode: EUR
                            amount: 50
                          accruedInterest:
                            currencyCode: EUR
                            amount: 50
                          valuation:
                            currencyCode: EUR
                            amount: 50
          cumulative-performances:
            - dateFrom: 2021-02-02T01:35:00.000-05:00
              dateTo: 2021-02-02T01:40:00.000-05:00
              valuePct: 121
            - dateFrom: 2021-03-01T01:30:00.000-05:00
              dateTo: 2021-03-02T01:30:00.000-05:00
              valuePct: 6
            - dateFrom: 2021-04-02T01:30:00.000-05:00
              dateTo: 2021-04-02T01:30:00.000-05:00
              valuePct: 3
            - dateFrom: 2021-08-02T01:30:00.000-05:00
              dateTo: 2021-08-02T01:30:00.000-05:00
              valuePct: 9
            - dateFrom: 2021-07-02T01:30:00.000-05:00
              dateTo: 2021-07-02T01:30:00.000-05:00
              valuePct: 33
            - dateFrom: 2021-01-02T01:30:00.000-05:00
              dateTo: 2021-01-02T01:30:00.000-05:00
              valuePct: 1
            - dateFrom: 2021-05-02T01:30:00.000-05:00
              dateTo: 2021-05-02T01:30:00.000-05:00
              valuePct: 50
          benchmark:
            name: Dow Jones .DJI
          valuations:
            - dateFrom: 2021-07-01T01:30:00.000-05:00
              dateTo: 2021-07-01T01:30:00.000-05:00
              granularity: DAILY
              valuation:
                amount: 30.06
                currencyCode: EUR
              valuePct: 12
            - dateFrom: 2021-07-02T01:30:00.000-05:00
              dateTo: 2021-07-02T01:35:00.000-05:00
              granularity: DAILY
              valuation:
                amount: 90.06
                currencyCode: EUR
              valuePct: 9
            - dateFrom: 2021-07-05T01:30:00.000-05:00
              dateTo: 2021-07-05T01:40:00.000-05:00
              granularity: DAILY
              valuation:
                amount: 90.06
                currencyCode: EUR
              valuePct: 9
            - dateFrom: 2021-07-02T01:30:00.000-05:00
              dateTo: 2021-07-02T01:30:00.000-05:00
              granularity: MONTHLY
              valuation:
                amount: 7.06
                currencyCode: EUR
              valuePct: 7
            - dateFrom: 2021-07-02T01:30:00.000-05:00
              dateTo: 2021-07-02T01:30:00.000-05:00
              granularity: WEEKLY
              valuation:
                amount: 20.06
                currencyCode: EUR
              valuePct: 2
            - dateFrom: 2021-07-02T01:30:00.000-05:00
              dateTo: 2021-07-02T01:30:00.000-05:00
              granularity: QUARTERLY
              valuation:
                amount: 50.06
                currencyCode: EUR
              valuePct: 5
            - dateFrom: 2021-06-02T01:30:00.000-05:00
              dateTo: 2021-06-02T01:30:00.000-05:00
              granularity: QUARTERLY
              valuation:
                amount: 78.03
                currencyCode: EUR
              valuePct: 5
            - dateFrom: 2021-06-02T01:30:00.000-05:00
              dateTo: 2021-06-02T01:30:00.000-05:00
              granularity: MONTHLY
              valuation:
                amount: 70.06
                currencyCode: EUR
              valuePct: 75
            - dateFrom: 2021-03-02T01:30:00.000-05:00
              dateTo: 2021-03-02T01:30:00.000-05:00
              granularity: MONTHLY
              valuation:
                amount: 40.06
                currencyCode: EUR
              valuePct: 42
      positions:
        - portfolioId: portfolioExternalId1
          subPortfolioId: subPortfolioId1
          position:
            externalId: 1
            instrumentId: externalId
            absolutePerformance:
              amount: 44.12
              currencyCode: EUR
            relativePerformance: 1.2
            purchasePrice:
              amount: 124.18
              currencyCode: EUR
            unrealizedPLPct: 4.14
            unrealizedPL:
              amount: 34.12
              currencyCode: EUR
            accruedInterest:
              amount: 12.45
              currencyCode: EUR
            quantity: 187
            valuation:
              amount: 132.11
              currencyCode: EUR
            costPrice:
              amount: 145.11
              currencyCode: EUR
            costExchangeRate:
              amount: 1.23
              currencyCode: EUR
            percentAssetClass: 187
            percentPortfolio: 187
            percentParent: 187
            positionType: Bond
            additions:
              someKey: someValue
          transaction-categories:
            - key: purchase
              alias: Purchase
              description: Description of Purchase
          transactions:
            - transactionId: transactionId
              transactionDate: 2021-03-24T19:22:00.000-01:00
              valueDate: 2021-03-29T11:46:00.000-01:00
              transactionCategory: purchase
              exchange: NASDAQ
              orderType: Market Order
              counterpartyName: Executive Brokers
              counterpartyAccount: R3904N-R0328592-323-4
              quantity: 55
              price:
                amount: 129.22
                currencyCode: EUR
              amount:
                amount: 7107.1
                currencyCode: EUR
              amountGross:
                amount: 7183.22
                currencyCode: EUR
              fxRate:
                amount: 1.22
                currencyCode: EUR
              localTaxes:
                amount: 32.04
                currencyCode: EUR
              localFees:
                amount: 7.03
                currencyCode: EUR
              foreignTaxes:
                amount: 12.03
                currencyCode: EUR
              foreignFees:
                amount: 25.02
                currencyCode: EUR
              officialCode: APPL
              ISIN: US4930248325
              balanceAsset:
                amount: 25.02
                currencyCode: EUR
              balanceAmount:
                amount: 25.02
                currencyCode: EUR
              statusId: an5fke68fik54l
              statusName: Pending
              statusAbbr: O
              notes: notes
      aggregate-portfolios:
        - id: arrangementExternalId_1
          name: 15 Years Long Strategy
          portfoliosCount: 1
          riskLevel: Medium-low
          valuation:
            amount: 456231857.22
            currencyCode: EUR
          ytdPerformance: 2.3
          ytdPerformanceValue:
            amount: 10493332.72
            currencyCode: EUR
          mtdPerformance: -5.6
          mtdPerformanceValue:
            amount: -25548984
            currencyCode: EUR
          inValue:
            amount: 10342875
            currencyCode: EUR
          outValue:
            amount: 5500000
            currencyCode: EUR
          netValue:
            amount: 4842875
            currencyCode: EUR
          portfolios:
            - portfolioExternalId1
          allocations:
            - name: Bond
              percentage: 45
            - name: Cash
              percentage: 55
          managers:
            - id: 1fejk-3tji5o495803jr-41
              name: Kevin Clark
              link: link
          attorneys:
            - id: 1klsjr3904-43nf389-rej2187
              name: John Doe
              link: link
          additions:
            someKey: someValue
```