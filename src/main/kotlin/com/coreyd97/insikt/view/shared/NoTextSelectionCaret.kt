package com.coreyd97.insikt.view.shared

import javax.swing.text.DefaultCaret
import javax.swing.text.JTextComponent

class NoTextSelectionCaret(component: JTextComponent) : DefaultCaret() {
    init {
        setBlinkRate(component.caret.blinkRate)
        component.setHighlighter(null)
    }
}
