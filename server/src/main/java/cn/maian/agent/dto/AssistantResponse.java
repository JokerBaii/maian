package cn.maian.agent.dto;

import java.util.List;

public record AssistantResponse(String reply, String executionMode, List<Action> actions, List<String> steps) {
    public record Action(String kind, String title, String description, String url) {}
}
