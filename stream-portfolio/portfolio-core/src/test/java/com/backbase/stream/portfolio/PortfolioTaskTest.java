package com.backbase.stream.portfolio;

import static org.junit.jupiter.api.Assertions.*;

import com.backbase.stream.portfolio.model.WealthBundle;
import org.junit.jupiter.api.Test;

class PortfolioTaskTest {

    @Test
    void getName() {
        PortfolioTask portfolioTask = new PortfolioTask(new WealthBundle());
        assertNotNull(portfolioTask.getName());
    }

}