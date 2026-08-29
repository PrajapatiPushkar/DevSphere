package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledSummaryResponse;

public class PublicSummaryResponse {

    private String text;

    public PublicSummaryResponse() {
    }

    public PublicSummaryResponse(String text) {
        this.text = text;
    }

    public PublicSummaryResponse(CompiledSummaryResponse compiled) {
        if (compiled != null) {
            this.text = compiled.getText();
        }
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }
}
