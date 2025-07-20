package com.example.demo.domain.timeline.response;

import com.example.demo.domain.timeline.UserTimeline;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TimelineResponse {

    private List<UserTimeline> timelines;

    private Long nextCursor;

    public TimelineResponse(List<UserTimeline> timelines, Long nextCursor) {
        this.timelines = timelines;
        this.nextCursor = nextCursor;
    }
}
