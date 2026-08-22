package com.amarmohan.core.keyboard

import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

fun Modifier.moveFocusOnEnter(
    focusManager: FocusManager,
    direction: FocusDirection? = FocusDirection.Next
): Modifier {
    return this.onPreviewKeyEvent { keyEvent ->
        if ((keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) && keyEvent.type == KeyEventType.KeyDown) {
            if (direction != null) {
                focusManager.moveFocus(direction)
            } else {
                focusManager.clearFocus()
            }
            true
        } else {
            false
        }
    }
}
