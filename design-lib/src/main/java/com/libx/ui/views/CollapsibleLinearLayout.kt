package com.libx.ui.views

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import com.libx.ui.R

class CollapsibleLinearLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private var slideAnimator: ValueAnimator? = null
    var isExpanded = true
        private set

    private var isMoving: Boolean = false
    var animDuration: Long

    private val defaultClickListener = OnClickListener {
        if (isExpanded)
            collapse()
        else
            expand()
    }

    private var listener: OnExpandChangeListener? = null
    val contentView = LinearLayout(context).apply {
        id = View.generateViewId()
        layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
    }

    init {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.CollapsibleLinearLayout)
        isExpanded = typedArray.getBoolean(R.styleable.CollapsibleLinearLayout_expanded, false)
        animDuration = typedArray.getInteger(
            R.styleable.CollapsibleLinearLayout_animationDuration,
            DEFAULT_ANIM_DURATION
        ).toLong()
        typedArray.recycle()

        addView(contentView)

        contentView.postDelayed({
            expand(0, true)
        }, 200)
    }

    override fun onFinishInflate() {
        super.onFinishInflate()

        //Move all child views to containerView
        for (i in 0 until childCount) {
            val child = getChildAt(i) ?: continue
            if (child.id == contentView.id) continue

            removeView(child)
            contentView.addView(child)
        }

        initClickListeners()
    }

    private fun slideAnimator(start: Int, end: Int): ValueAnimator {
        return ValueAnimator.ofInt(start, end).apply {
            addUpdateListener { valueAnimator ->
                val value = valueAnimator.animatedValue as Int
                val layoutParams = contentView.layoutParams
                layoutParams?.height = value
                contentView.layoutParams = layoutParams
            }
        }
    }

    fun expand(timeAnim: Long = animDuration) {
        expand(timeAnim, false)
    }

    private fun expand(timeAnim: Long, initial: Boolean) {
        if (isMoving) return
        listener?.onExpandChanged(true)
        isMoving = true
        contentView.visibility = View.VISIBLE

        contentView.measure(
            MeasureSpec.makeMeasureSpec(contentView.width, MeasureSpec.EXACTLY),
            MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED)
        )

        val targetHeight = contentView.measuredHeight

        slideAnimator = slideAnimator(0, targetHeight).apply {
            onAnimationEnd {
                isExpanded = true
                isMoving = false
                if (initial) {
                    collapse()
                }
            }
            duration = timeAnim
            start()
        }
    }

    fun collapse(timeAnim: Long = animDuration) {
        if (isMoving) return
        listener?.onExpandChanged(false)
        isMoving = true
        val finalHeight = contentView.height

        slideAnimator = slideAnimator(finalHeight, 0).apply {
            onAnimationEnd {
                contentView.visibility = View.GONE
                isExpanded = false
                isMoving = false
            }
            duration = timeAnim
            start()
        }
    }

    fun removeOnExpandChangeListener() {
        this.listener = null
    }

    fun setOnExpandChangeListener(expandChangeListener: OnExpandChangeListener) {
        listener = expandChangeListener
    }

    fun setOnExpandChangeListener(expandChangeUnit: (Boolean) -> Unit) {
        listener = object : OnExpandChangeListener {
            override fun onExpandChanged(isExpanded: Boolean) {
                expandChangeUnit(isExpanded)
            }
        }
    }

    private fun ValueAnimator.onAnimationEnd(onAnimationEnd: () -> Unit) {
        addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator) {
            }

            override fun onAnimationEnd(animation: Animator) {
                onAnimationEnd()
            }

            override fun onAnimationStart(animation: Animator) {
            }

            override fun onAnimationCancel(animation: Animator) {
            }
        })
    }

    private fun initClickListeners() {
        val views = getViewsByTag(contentView, "expand_or_collapse")
        views.forEach {
            it.setOnClickListener(defaultClickListener)
        }
    }

    private fun getViewsByTag(root: ViewGroup, tag: String): ArrayList<View> {
        val views = ArrayList<View>()
        val childCount = root.childCount
        for (i in 0 until childCount) {
            val child = root.getChildAt(i)
            if (child is ViewGroup) {
                views.addAll(getViewsByTag(child, tag))
            }

            val tagObj = child.tag
            if (tagObj != null && tagObj == tag) {
                views.add(child)
            }

        }
        return views
    }

    override fun onSaveInstanceState(): Parcelable? {
        val superState = super.onSaveInstanceState()
        superState?.let {
            val customViewSavedState = ExpandedCardSavedState(superState)
            customViewSavedState.isExpanded = isExpanded
            return customViewSavedState
        }
        return superState
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        val customViewSavedState = state as ExpandedCardSavedState
        isExpanded = customViewSavedState.isExpanded
        super.onRestoreInstanceState(customViewSavedState.superState)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (!isExpanded) {
            collapse(timeAnim = 0)
        }
    }

    private class ExpandedCardSavedState : BaseSavedState {

        internal var isExpanded: Boolean = false

        constructor(superState: Parcelable) : super(superState)

        private constructor(source: Parcel) : super(source) {
            isExpanded = source.readInt() == 1
        }

        override fun writeToParcel(out: Parcel, flags: Int) {
            super.writeToParcel(out, flags)
            out.writeInt(if (isExpanded) 1 else 0)
        }

        companion object CREATOR : Parcelable.Creator<ExpandedCardSavedState> {
            override fun createFromParcel(source: Parcel): ExpandedCardSavedState {
                return ExpandedCardSavedState(source)
            }

            override fun newArray(size: Int): Array<ExpandedCardSavedState?> {
                return arrayOfNulls(size)
            }
        }

        override fun describeContents(): Int {
            return 0
        }
    }

    interface OnExpandChangeListener {
        fun onExpandChanged(isExpanded: Boolean)
    }

    companion object {
        const val DEFAULT_ANIM_DURATION = 350
    }
}