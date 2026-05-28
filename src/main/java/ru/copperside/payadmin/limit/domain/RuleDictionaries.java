package ru.copperside.payadmin.limit.domain;

import java.util.List;

public record RuleDictionaries(
        List<DictionaryItem> operationFamilies,
        List<OperationType> operationTypes,
        List<DictionaryItem> paymentSystems,
        List<DictionaryItem> issuerCountries,
        List<DictionaryItem> issuerBanks,
        List<DictionaryItem> bins,
        List<DictionaryItem> cardTypes,
        List<DictionaryItem> cardLevels,
        List<String> directions,
        List<String> operationSelectorTypes,
        List<String> attributeSelectorTypes,
        List<String> targetTypes,
        List<String> metrics,
        List<String> periods
) {
}
