package com.netex.addressbook.activity;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/activities")
public class AdminActivityController {

    private final ActivityHistoryClient history;

    public AdminActivityController(ActivityHistoryClient history) {
        this.history = history;
    }

    @GetMapping
    public ActivityHistoryResponse recent() {
        return history.recent();
    }
}
