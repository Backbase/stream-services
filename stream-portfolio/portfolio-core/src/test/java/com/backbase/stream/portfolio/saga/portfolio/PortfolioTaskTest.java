package com.backbase.stream.portfolio.saga.portfolio;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.backbase.stream.portfolio.model.WealthBundle;

class PortfolioTaskTest {

    @Test
    void getName() {
        PortfolioTask portfolioTask = new PortfolioTask(new WealthBundle());
        assertNotNull(portfolioTask.getName());
    }

}