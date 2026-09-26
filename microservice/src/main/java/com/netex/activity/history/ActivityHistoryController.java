package com.netex.activity.history;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/activity")
public class ActivityHistoryController {

    private final ActivityHistoryRepository repository;

    public ActivityHistoryController(ActivityHistoryRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public ActivityHistoryResponse recent() {
        return repository.recent();
    }
}
