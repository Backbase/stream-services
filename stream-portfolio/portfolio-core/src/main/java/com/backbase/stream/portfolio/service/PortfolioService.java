package com.backbase.stream.portfolio.service;

import com.backbase.stream.portfolio.model.WealthBundle;

import reactor.core.publisher.Flux;

public interface PortfolioService {

	Flux<WealthBundle> ingestWealthBundles(Flux<WealthBundle> wealthBundles);
}
