package ru.copperside.payadmin.limit.adapter.out.limitmanagement;

record LimitManagementApiResponse<T>(T data, Object meta, Object error, String timestamp) {
}
