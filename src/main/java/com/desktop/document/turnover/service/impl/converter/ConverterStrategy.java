package com.desktop.document.turnover.service.impl.converter;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConverterStrategy {

    private final Map<TypeFromDocs, ConverterService> converterStrategyMap;

    public ConverterService getConverterService(TypeFromDocs fromDocs) {
        return converterStrategyMap.get(fromDocs);
    }
}
