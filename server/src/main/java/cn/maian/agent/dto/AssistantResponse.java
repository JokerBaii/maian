package cn.maian.agent.dto;

import java.util.List;

public record AssistantResponse(String reply, String executionMode, List<Action> actions, List<String> steps, List<Task> tasks, boolean continuation, boolean conversation) {
    public AssistantResponse(String reply, String executionMode, List<Action> actions, List<String> steps, List<Task> tasks, boolean continuation) {
        this(reply, executionMode, actions, steps, tasks, continuation, false);
    }
    public AssistantResponse(String reply, String executionMode, List<Action> actions, List<String> steps, List<Task> tasks) {
        this(reply, executionMode, actions, steps, tasks, false);
    }
    public AssistantResponse(String reply, String executionMode, List<Action> actions, List<String> steps) {
        this(reply, executionMode, actions, steps, List.of());
    }
    public record Action(String kind, String title, String description, String url) {}
    public record Task(String operation, java.util.Map<String, String> parameters) {}
}
