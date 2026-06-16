package ru.copperside.payadmin.gos.adapter.out.gosadapter;

/**
 * Downstream request bodies for j-gos-adapter. {@code merchId} is intentionally absent here — it is
 * carried in the {@code TCB.Header-Merch-Id} header rather than the body.
 */
final class GosAdapterRequests {

    record ExtractBody(String beneficiaryType, String inn, String name) {
    }

    record ExtractStatusBody(String extractRequestId) {
    }

    private GosAdapterRequests() {
    }
}
