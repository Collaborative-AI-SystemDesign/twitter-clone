package com.example.demo.domain.timeline.service;

import com.example.demo.domain.timeline.UserTimeline;
import com.example.demo.domain.timeline.UserTimelineRepository;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TimelineService {

    private final UserTimelineRepository timelineRepository;

    /**
     * 최신 타임라인 20개 조회
     */
    public List<UserTimeline> getLatestTimeline(UUID followerId) {
        return timelineRepository.findLatestTimeline(followerId);
    }

    /**
     * 특정 시간 이전 타임라인 20개 조회 (커서 기반)
     */
    public List<UserTimeline> getTimelineBefore(UUID followerId, Long lastTimestamp) {
        LocalDateTime cursor = Instant.ofEpochMilli(lastTimestamp)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();

        return timelineRepository.findTimelineWithCursor(followerId, cursor);
    }
}
