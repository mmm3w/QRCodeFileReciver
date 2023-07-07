package com.mitsuki.qrcodefilereciver.qrcode

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View

class CodeIndicator @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var mData: CodeMark? = null

    private val ppp: Path = Path()
    private val mViewRect = Rect()


    fun setMark(data: CodeMark) {
        mData = data
        mData?.apply {
            finalCode.forEach {
                showMapping(mViewRect, it)
            }
        }
        postInvalidate()
    }

    override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
        super.onLayout(changed, left, top, right, bottom)
        mViewRect.set(0, 0, width, height)
    }

    private val mPaint = Paint()

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        mData?.apply {
            mPaint.color = 0xffff0000.toInt()
            mPaint.style = Paint.Style.STROKE
            mPaint.strokeWidth = 10F
            showMapping(ppp, width, height)
            canvas?.drawPath(ppp, mPaint)
        }
    }
}