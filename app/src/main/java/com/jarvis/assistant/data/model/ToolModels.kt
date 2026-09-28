package com.jarvis.assistant.data.model

data class ToolDefinition(
    val name: String,
    val description: String,
    val parameters: Map<String, String> = emptyMap()
)

data class ToolExecutionResult(
    val toolName: String,
    val success: Boolean,
    val message: String,
    val data: Any? = null
)
