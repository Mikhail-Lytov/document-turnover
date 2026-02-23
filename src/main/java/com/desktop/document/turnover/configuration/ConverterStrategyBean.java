package com.desktop.document.turnover.configuration;

import com.desktop.document.turnover.domain.enums.TypeFromDocs;
import com.desktop.document.turnover.service.api.converter.ConverterService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class ConverterStrategyBean {

    private final List<ConverterService> converterServices;


    @Bean
    public Map<TypeFromDocs, ConverterService> converterServiceMap() {

        Map<TypeFromDocs, ConverterService> converterServiceMap = new HashMap<>();
        for (ConverterService converterService : converterServices) {
            converterService.getTypes().forEach(type -> {
                converterServiceMap.put(type, converterService);
            });
        }
        return converterServiceMap;
    }
}
