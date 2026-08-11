package com.staticum.mientreno.ui.routines

data class RoutineEditorState(
    val id: Long = 0,
    val name: String = "",
    val description: String = "",
    val blocks: List<DraftBlock> = emptyList(),
    val isEditing: Boolean = false,
    val isSaved: Boolean = false
) {
    val isValid: Boolean
        get() = name.isNotBlank() && blocks.isNotEmpty() && blocks.all { it.items.isNotEmpty() }
}
