package com.devsphere.user.dto.compilation;

public class CompiledSummaryResponse {

    private String text;

    public CompiledSummaryResponse() {
    }

    public CompiledSummaryResponse(String text) {
        this.text = text;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
