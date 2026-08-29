package com.devsphere.user.dto.publicresume;

import com.devsphere.user.dto.compilation.CompiledSkillItemResponse;
import com.devsphere.user.dto.compilation.CompiledSkillsResponse;
import java.util.ArrayList;
import java.util.List;

public class PublicSkillsResponse {

    private List<PublicSkillItemResponse> items = new ArrayList<>();

    public PublicSkillsResponse() {
    }

    public PublicSkillsResponse(List<PublicSkillItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public PublicSkillsResponse(CompiledSkillsResponse compiled) {
        if (compiled != null && compiled.getItems() != null) {
            for (CompiledSkillItemResponse item : compiled.getItems()) {
                this.items.add(new PublicSkillItemResponse(item));
            }
        }
    }

    public List<PublicSkillItemResponse> getItems() {
        return items;
    }

    public void setItems(List<PublicSkillItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
