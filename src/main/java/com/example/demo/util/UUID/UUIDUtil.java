package com.example.demo.util.UUID;

import java.util.UUID;

public class UUIDUtil {

    /**
     * 랜덤 UUID 생성
     */
    public static UUID generate() {
        return UUID.randomUUID();
    }

    /**
     * UUID 문자열로부터 UUID 객체 생성
     */
    public static UUID fromString(String id) {
        return UUID.fromString(id);
    }

    /**
     * UUID 객체를 문자열로 변환
     */
    public static String toString(UUID uuid) {
        return uuid.toString();
    }

    /**
     * 대시 없는 UUID 문자열 생성 (예: MongoDB ObjectId 느낌)
     */
    public static String generateWithoutDash() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 대시 있는 UUID 문자열을 대시 없는 형식으로 변환
     */
    public static String removeDash(String uuidStr) {
        return uuidStr.replace("-", "");
    }

    /**
     * 대시 없는 UUID 문자열을 UUID 객체로 변환
     */
    public static UUID fromDashless(String dashless) {
        return UUID.fromString(
                dashless.replaceFirst(
                        "(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})",
                        "$1-$2-$3-$4-$5"
                )
        );
    }
}
