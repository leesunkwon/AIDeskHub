package com.kotlinsun.aideskhub.ai

class GeminiAnswerPlanner {
    fun buildPrompt(question: String, localContext: String): String {
        return """
            너는 기관 안내를 담당하는 AI 비서입니다.
            앱 내부에 저장된 로컬 데이터만 근거로 답변하세요.

            [사용자 질문]
            $question

            [로컬 검색 결과]
            $localContext
        """.trimIndent()
    }
}
