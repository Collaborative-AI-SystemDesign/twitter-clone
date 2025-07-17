package com.example.demo.domain.tweet;

import com.example.demo.domain.BaseEntity;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.cassandra.core.mapping.Column;
import org.springframework.data.cassandra.core.mapping.PrimaryKey;
import org.springframework.data.cassandra.core.mapping.Table;

import java.util.UUID;

@Table("tweets")
@Getter
@Setter
@NoArgsConstructor
public class Tweet extends BaseEntity {

    /**
     * 트윗 고유 식별자 (Primary Key)
     * - UUID로 글로벌 고유성 보장
     * - 파티션 키로 사용되어 특정 트윗에 직접 접근 가능
     */
    @PrimaryKey
    @Column("tweet_id")
    private UUID tweetId;

    /**
     * 트윗 작성자 ID
     * - 어떤 사용자가 이 트윗을 작성했는지 식별
     * - Fan-out 과정에서 author 정보로 활용
     */
    @Column("user_id")
    private UUID userId;

    /**
     * 트윗 내용
     * - 실제 사용자가 작성한 텍스트 내용
     * - 최대 길이 제한은 애플리케이션 레벨에서 검증
     */
    @Column("tweet_text")
    private String tweetText;

    /**
     * 트윗 생성 생성자
     * @param userId 트윗 작성자 ID
     * @param tweetText 트윗 내용
     */
    public Tweet(UUID userId, String tweetText) {
        this.tweetId = UUID.randomUUID(); // 자동으로 고유 ID 생성
        this.userId = userId;
        this.tweetText = tweetText;
    }
}
