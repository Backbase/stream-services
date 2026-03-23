package com.backbase.stream.investment;

import java.util.List;

public interface InvestmentDataValue {

    long getTotalProcessedValues();

    default <T> int getSize(List<T> list) {
        return list != null ? list.size() : 0;
    }

}
