package ru.copperside.payadmin.merchant.adapter.out.merchantscore;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MerchantsCoreCount(long total) {
}
