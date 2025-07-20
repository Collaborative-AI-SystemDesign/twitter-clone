package com.example.demo.domain.timeline.controller;

import com.example.demo.domain.timeline.UserTimeline;
import com.example.demo.domain.timeline.response.TimelineResponse;
import com.example.demo.domain.timeline.service.TimelineService;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
    public ResponseEntity<TimelineResponse> getTimelineByfollowerId(
            @PathVariable("followerId")UUID followerId,
            @RequestParam(value = "last", required = false) Long lastTimestamp
    ) {
        // 커서가 없다면 최신 목록 조회하고 바로 반환
        if (lastTimestamp == null) {
            List<UserTimeline> timelines = timelineService.getLatestTimeline(followerId);

            return ResponseEntity.ok(new TimelineResponse(
                    timelines,
                    null
            ));
        }

        // 커서가 있을 경우
        List<UserTimeline> timelines = timelineService.getTimelineBefore(followerId, lastTimestamp);

        Long nextCursor = null;
        if (!timelines.isEmpty()) {
            LocalDateTime lastCreatedAt = timelines.get(timelines.size() - 1).getKey().getCreatedAt();
            nextCursor = lastCreatedAt.atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        return ResponseEntity.ok(new TimelineResponse(timelines, nextCursor));
    }
}
