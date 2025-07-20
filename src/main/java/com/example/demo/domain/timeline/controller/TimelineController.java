package com.example.demo.domain.timeline.controller;

import com.example.demo.domain.timeline.UserTimeline;
import com.example.demo.domain.timeline.service.TimelineService;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/timeline")
@RequiredArgsConstructor
public class TimelineController {

    private final TimelineService timelineService;

    @GetMapping("/{followerId}")
    public ResponseEntity<List<UserTimeline>> getTimelineByfollowerId(
            @PathVariable("followerId")UUID followerId,
            @RequestParam(value = "last", required = false) Long lastTimestamp
    ) {
        if (lastTimestamp == null) {
            return ResponseEntity.ok(timelineService.getLatestTimeline(followerId));
        } else {
            return ResponseEntity.ok(timelineService.getTimelineBefore(followerId, lastTimestamp));
        }
    }
}
