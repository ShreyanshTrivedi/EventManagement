package com.campus.event.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public class RegisterEventRequest {
    @NotNull
    private Long eventId;
    @Email
    @NotBlank
    private String email;
    @NotBlank
    private String fullName;
    private Map<String, Object> answers;

    public Long getEventId() { return eventId; }
    public void setEventId(Long eventId) { this.eventId = eventId; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }
    public Map<String, Object> getAnswers() { return answers; }
    public void setAnswers(Map<String, Object> answers) { this.answers = answers; }
}
