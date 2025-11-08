package com.coreyd97.insikt.view.shared

import javax.swing.JTextPane
import javax.swing.text.*

class WrappedTextPane : JTextPane {
    constructor(styledDocument: StyledDocument?) {
        this.editorKit = WrapEditorKit()
    }

    constructor() {
        this.editorKit = WrapEditorKit()
    }

    internal inner class WrapEditorKit : StyledEditorKit() {
        var factory: ViewFactory = WrapColumnFactory()

        override fun getViewFactory(): ViewFactory {
            return factory
        }
    }

    internal inner class WrapColumnFactory : ViewFactory {
        override fun create(elem: Element): View {
            val kind = elem.name
            if (kind != null) {
                if (kind == AbstractDocument.ContentElementName) {
                    return WrapLabelView(elem)
                } else if (kind == AbstractDocument.ParagraphElementName) {
                    return ParagraphView(elem)
                } else if (kind == AbstractDocument.SectionElementName) {
                    return BoxView(elem, View.Y_AXIS)
                } else if (kind == StyleConstants.ComponentElementName) {
                    return ComponentView(elem)
                } else if (kind == StyleConstants.IconElementName) {
                    return IconView(elem)
                }
            }

            // default to text display
            return LabelView(elem)
        }
    }

    internal class WrapLabelView(elem: Element) : LabelView(elem) {
        override fun getMinimumSpan(axis: Int): Float {
            when (axis) {
                X_AXIS -> return 0f
                Y_AXIS -> return super.getMinimumSpan(axis)
                else -> throw IllegalArgumentException("Invalid axis: " + axis)
            }
        }
    }
}
