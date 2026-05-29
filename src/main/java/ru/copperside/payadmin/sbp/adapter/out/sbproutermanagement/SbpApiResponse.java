package ru.copperside.payadmin.sbp.adapter.out.sbproutermanagement;

record SbpApiResponse<T>(T data, Object meta, Object error, String timestamp) {
}
