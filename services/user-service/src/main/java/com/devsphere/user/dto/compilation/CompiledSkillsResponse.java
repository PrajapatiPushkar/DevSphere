package com.devsphere.user.dto.compilation;

import java.util.ArrayList;
import java.util.List;

public class CompiledSkillsResponse {

    private List<CompiledSkillItemResponse> items = new ArrayList<>();

    public CompiledSkillsResponse() {
    }

    public CompiledSkillsResponse(List<CompiledSkillItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }

    public List<CompiledSkillItemResponse> getItems() {
        return items;
    }

    public void setItems(List<CompiledSkillItemResponse> items) {
        this.items = items != null ? items : new ArrayList<>();
    }
}
