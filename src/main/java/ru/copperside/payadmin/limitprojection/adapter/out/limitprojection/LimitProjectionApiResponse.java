package ru.copperside.payadmin.limitprojection.adapter.out.limitprojection;

record LimitProjectionApiResponse<T>(T data, Object meta, Object error, String timestamp) {
}
