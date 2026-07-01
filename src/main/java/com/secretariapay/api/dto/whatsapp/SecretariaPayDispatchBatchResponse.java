package com.secretariapay.api.dto.whatsapp;

import java.util.ArrayList;
import java.util.List;

public class SecretariaPayDispatchBatchResponse {

    private int processed;
    private int sent;
    private int failed;
    private List<SecretariaPayMessageDispatchResult> results = new ArrayList<>();

    public int getProcessed() {
        return processed;
    }

    public SecretariaPayDispatchBatchResponse setProcessed(int processed) {
        this.processed = processed;
        return this;
    }

    public int getSent() {
        return sent;
    }

    public SecretariaPayDispatchBatchResponse setSent(int sent) {
        this.sent = sent;
        return this;
    }

    public int getFailed() {
        return failed;
    }

    public SecretariaPayDispatchBatchResponse setFailed(int failed) {
        this.failed = failed;
        return this;
    }

    public List<SecretariaPayMessageDispatchResult> getResults() {
        return results;
    }

    public SecretariaPayDispatchBatchResponse setResults(List<SecretariaPayMessageDispatchResult> results) {
        this.results = results;
        return this;
    }
}
