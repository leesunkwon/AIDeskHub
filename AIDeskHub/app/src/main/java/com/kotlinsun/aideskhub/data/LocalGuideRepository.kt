package com.kotlinsun.aideskhub.data

import com.kotlinsun.aideskhub.model.RouteGuide

class LocalGuideRepository {
    fun getSampleNotices(): List<String> = listOf(
        "방문 안내와 길 찾기를 도와드립니다.",
        "기관 운영 시간과 부서 연락처를 물어보세요.",
        "관리자 모드에서 안내 데이터를 직접 관리할 수 있습니다.",
    )

    fun getSampleRouteGuide(): RouteGuide = RouteGuide(
        destinationName = "학생 식당",
        answer = "학생 식당은 본관 1층 동쪽 복도 끝에 있습니다. 화면의 안내 순서대로 이동해 주세요.",
        routeSteps = listOf(
            "1층 로비에서 오른쪽 복도로 이동하세요.\n\n향후 이 영역에는 관리자가 등록한 길 안내 사진이 표시됩니다.",
            "엘리베이터 앞 표지판을 지나 동쪽 출입구 방향으로 이동하세요.",
            "복도 끝 유리문 왼쪽이 학생 식당 입구입니다.",
        ),
        keywords = listOf("학생 식당", "식당", "학식", "구내식당"),
    )
}
