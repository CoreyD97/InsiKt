package com.coreyd97.insikt.view.shared

import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.LayoutManager
import java.awt.Rectangle
import javax.swing.JPanel
import javax.swing.JViewport
import javax.swing.Scrollable
import javax.swing.SwingConstants

/**
 * http://www.camick.com/java/source/ScrollablePanel.java
 */
class ScrollablePanel @JvmOverloads constructor(layout: LayoutManager? = FlowLayout()) :
    JPanel(layout), Scrollable, SwingConstants {
    private var scrollableHeight: ScrollableSizeHint? = ScrollableSizeHint.NONE
    private var scrollableWidth: ScrollableSizeHint? = ScrollableSizeHint.NONE
    private var horizontalBlock: IncrementInfo? = null
    private var horizontalUnit: IncrementInfo? = null
    private var verticalBlock: IncrementInfo? = null
    private var verticalUnit: IncrementInfo? = null
    /**
     * Constuctor for specifying the LayoutManager of the panel.
     *
     * @param layout the LayountManger for the panel
     */
    /**
     * Default constructor that uses a FlowLayout
     */
    init {
        val block = IncrementInfo(IncrementType.PERCENT, 100)
        val unit = IncrementInfo(IncrementType.PERCENT, 10)

        setScrollableBlockIncrement(SwingConstants.HORIZONTAL, block)
        setScrollableBlockIncrement(SwingConstants.VERTICAL, block)
        setScrollableUnitIncrement(SwingConstants.HORIZONTAL, unit)
        setScrollableUnitIncrement(SwingConstants.VERTICAL, unit)
    }

    /**
     * Get the height ScrollableSizeHint enum
     *
     * @return the ScrollableSizeHint enum for the height
     */
    fun getScrollableHeight(): ScrollableSizeHint? {
        return scrollableHeight
    }

    /**
     * Set the ScrollableSizeHint enum for the height. The enum is used to determine the boolean value
     * that is returned by the getScrollableTracksViewportHeight() method. The valid groups are:
     *
     *
     * ScrollableSizeHint.NONE - return "false", which causes the height of the panel to be used when
     * laying out the children ScrollableSizeHint.FIT - return "true", which causes the height of the
     * viewport to be used when laying out the children ScrollableSizeHint.STRETCH - return "true"
     * when the viewport height is greater than the height of the panel, "false" otherwise.
     *
     * @param scrollableHeight as represented by the ScrollableSizeHint enum.
     */
    fun setScrollableHeight(scrollableHeight: ScrollableSizeHint?) {
        this.scrollableHeight = scrollableHeight
        revalidate()
    }

    /**
     * Get the width ScrollableSizeHint enum
     *
     * @return the ScrollableSizeHint enum for the width
     */
    fun getScrollableWidth(): ScrollableSizeHint? {
        return scrollableWidth
    }

    /**
     * Set the ScrollableSizeHint enum for the width. The enum is used to determine the boolean value
     * that is returned by the getScrollableTracksViewportWidth() method. The valid groups are:
     *
     *
     * ScrollableSizeHint.NONE - return "false", which causes the width of the panel to be used when
     * laying out the children ScrollableSizeHint.FIT - return "true", which causes the width of the
     * viewport to be used when laying out the children ScrollableSizeHint.STRETCH - return "true"
     * when the viewport width is greater than the width of the panel, "false" otherwise.
     *
     * @param scrollableWidth as represented by the ScrollableSizeHint enum.
     */
    fun setScrollableWidth(scrollableWidth: ScrollableSizeHint?) {
        this.scrollableWidth = scrollableWidth
        revalidate()
    }

    /**
     * Get the block IncrementInfo for the specified orientation
     *
     * @return the block IncrementInfo for the specified orientation
     */
    fun getScrollableBlockIncrement(orientation: Int): IncrementInfo? {
        return if (orientation == SwingConstants.HORIZONTAL) horizontalBlock else verticalBlock
    }

    /**
     * Specify the information needed to do block scrolling.
     *
     * @param orientation specify the scrolling orientation. Must be either: SwingContants.HORIZONTAL
     * or SwingContants.VERTICAL.
     * @param amount      a value used with the IncrementType to determine the scrollable amount
     * @paran type  specify how the amount parameter in the calculation of the scrollable amount.
     * Valid groups are: IncrementType.PERCENT - treat the amount as a % of the viewport size
     * IncrementType.PIXEL - treat the amount as the scrollable amount
     */
    fun setScrollableBlockIncrement(orientation: Int, type: IncrementType?, amount: Int) {
        val info = IncrementInfo(type, amount)
        setScrollableBlockIncrement(orientation, info)
    }

    /**
     * Specify the information needed to do block scrolling.
     *
     * @param orientation specify the scrolling orientation. Must be either: SwingContants.HORIZONTAL
     * or SwingContants.VERTICAL.
     * @param info        An IncrementInfo object containing information of how to calculate the
     * scrollable amount.
     */
    fun setScrollableBlockIncrement(orientation: Int, info: IncrementInfo) {
        when (orientation) {
            SwingConstants.HORIZONTAL -> horizontalBlock = info
            SwingConstants.VERTICAL -> verticalBlock = info
            else -> throw IllegalArgumentException("Invalid orientation: " + orientation)
        }
    }

    /**
     * Get the unit IncrementInfo for the specified orientation
     *
     * @return the unit IncrementInfo for the specified orientation
     */
    fun getScrollableUnitIncrement(orientation: Int): IncrementInfo? {
        return if (orientation == SwingConstants.HORIZONTAL) horizontalUnit else verticalUnit
    }

    /**
     * Specify the information needed to do unit scrolling.
     *
     * @param orientation specify the scrolling orientation. Must be either: SwingContants.HORIZONTAL
     * or SwingContants.VERTICAL.
     * @param amount      a value used with the IncrementType to determine the scrollable amount
     * @paran type  specify how the amount parameter in the calculation of the scrollable amount.
     * Valid groups are: IncrementType.PERCENT - treat the amount as a % of the viewport size
     * IncrementType.PIXEL - treat the amount as the scrollable amount
     */
    fun setScrollableUnitIncrement(orientation: Int, type: IncrementType?, amount: Int) {
        val info = IncrementInfo(type, amount)
        setScrollableUnitIncrement(orientation, info)
    }

    /**
     * Specify the information needed to do unit scrolling.
     *
     * @param orientation specify the scrolling orientation. Must be either: SwingContants.HORIZONTAL
     * or SwingContants.VERTICAL.
     * @param info        An IncrementInfo object containing information of how to calculate the
     * scrollable amount.
     */
    fun setScrollableUnitIncrement(orientation: Int, info: IncrementInfo) {
        when (orientation) {
            SwingConstants.HORIZONTAL -> horizontalUnit = info
            SwingConstants.VERTICAL -> verticalUnit = info
            else -> throw IllegalArgumentException("Invalid orientation: " + orientation)
        }
    }

    override fun getPreferredScrollableViewportSize(): Dimension? {
        return getPreferredSize()
    }

    override fun getScrollableUnitIncrement(
        visible: Rectangle, orientation: Int, direction: Int
    ): Int {
        when (orientation) {
            SwingConstants.HORIZONTAL -> return getScrollableIncrement(
                horizontalUnit!!,
                visible.width
            )

            SwingConstants.VERTICAL -> return getScrollableIncrement(verticalUnit!!, visible.height)
            else -> throw IllegalArgumentException("Invalid orientation: " + orientation)
        }
    }

    //  Implement Scrollable interface
    override fun getScrollableBlockIncrement(
        visible: Rectangle, orientation: Int, direction: Int
    ): Int {
        when (orientation) {
            SwingConstants.HORIZONTAL -> return getScrollableIncrement(
                horizontalBlock!!, visible.width
            )

            SwingConstants.VERTICAL -> return getScrollableIncrement(
                verticalBlock!!,
                visible.height
            )

            else -> throw IllegalArgumentException("Invalid orientation: " + orientation)
        }
    }

    protected fun getScrollableIncrement(info: IncrementInfo, distance: Int): Int {
        if (info.increment == IncrementType.PIXELS) {
            return info.amount
        } else {
            return distance * info.amount / 100
        }
    }

    override fun getScrollableTracksViewportWidth(): Boolean {
        if (scrollableWidth == ScrollableSizeHint.NONE) {
            return false
        }

        if (scrollableWidth == ScrollableSizeHint.FIT) {
            return true
        }

        //  STRETCH sizing, use the greater of the panel or viewport width
        if (parent is JViewport) {
            return (parent.width > getPreferredSize().width)
        }

        return false
    }

    override fun getScrollableTracksViewportHeight(): Boolean {
        if (scrollableHeight == ScrollableSizeHint.NONE) {
            return false
        }

        if (scrollableHeight == ScrollableSizeHint.FIT) {
            return true
        }

        //  STRETCH sizing, use the greater of the panel or viewport height
        if (parent is JViewport) {
            return (parent.height > getPreferredSize().height)
        }

        return false
    }

    enum class ScrollableSizeHint {
        NONE,
        FIT,
        STRETCH
    }

    enum class IncrementType {
        PERCENT,
        PIXELS
    }

    /**
     * Helper class to hold the information required to calculate the scroll amount.
     */
    class IncrementInfo(val increment: IncrementType?, val amount: Int) {
        override fun toString(): String {
            return "ScrollablePanel[" +
                    this.increment + ", " +
                    amount + "]"
        }
    }
}
