package com.mitsuki.qrcodefilereciver.qrcode

import android.graphics.Matrix
import android.graphics.Path
import android.graphics.Rect

/**
 * 仅展示用
 * 在最终结果的时候
 * 将会化身多矩阵，并且能够完成点击事件命中判断返回指定选项的只能
 */
class CodeMark {
    var rotation: Int = 0 //这是屏幕方向
    private val analysisRect: Rect = Rect() //这是分析分辨率
    private val previewRect: Rect = Rect()  //这是预览分辨率

    //二维码位置标识综合路径
    private val mMetaPath = Path() //原始二维码所在路径
    private val mMappingMatrix = Matrix() //从解析视图映射到预览视图中二维码路径需要的变换
    private val mMappingPath = Path() //预览视图二维码所在路径
    private val mShowMatrix = Matrix() ///从预览视图映射到实际View中二维码路径需要的变换

    internal val finalCode: MutableList<Rect> = arrayListOf()

    //确定解析视图的大小
    fun analysisRect(w: Int, h: Int) {
        when (rotation) {
            0, 180 -> analysisRect.set(0, 0, w, h)
            90, 270 -> analysisRect.set(0, 0, h, w)
        }
    }

    //确定预览视图的大小
    fun previewRect(w: Int, h: Int) {
        when (rotation) {
            0, 180 -> previewRect.set(0, 0, w, h)
            90, 270 -> previewRect.set(0, 0, h, w)
        }
    }

    fun reset() {
        mMetaPath.reset()
    }

    //修改这段逻辑可调整 展示形式
    fun path(rect: Rect) {
        mMetaPath.addRect(
            rect.left.toFloat(),
            rect.top.toFloat(),
            rect.right.toFloat(),
            rect.bottom.toFloat(),
            Path.Direction.CW
        )
    }

    //将解析视图的坐标映射到预览视图的坐标
    fun previewMapping() {
//        if (isFinal) {
//            //final做矩阵转换
//            finalCode.forEach {
//                rectMapping(analysisRect, previewRect, it, it)
//            }
//        } else {
            mapping(
                analysisRect.width(),
                analysisRect.height(),
                previewRect.width(),
                previewRect.height(),
                mMetaPath,
                mMappingPath,
                mMappingMatrix
            )
//        }
    }

    //将预览视图的坐标映射到实际显示View的坐标上
    fun showMapping(path: Path, w: Int, h: Int) {
        mapping(
            previewRect.width(),
            previewRect.height(),
            w,
            h,
            mMappingPath,
            path,
            mShowMatrix
        )
    }

    fun showMapping(view: Rect, rect: Rect) {
        rectMapping(previewRect, view, rect, rect)
    }

    //映射逻辑
    private fun mapping(
        fromWitdh: Int,
        fromHeight: Int,
        toWitdh: Int,
        toHeight: Int,
        fromPath: Path,
        toPath: Path,
        matrix: Matrix,
    ) {
        val wsc = toWitdh.toFloat() / fromWitdh
        val hsc = toHeight.toFloat() / fromHeight

        matrix.reset()
        if (wsc >= hsc) {
            matrix.setScale(wsc, wsc)
            matrix.postTranslate(0F, (toHeight - fromHeight * wsc) / 2F)
        } else {
            matrix.setScale(hsc, hsc)
            matrix.postTranslate((toWitdh - fromWitdh * hsc) / 2F, 0F)
        }

        toPath.reset()
        toPath.set(fromPath)
        toPath.transform(matrix)
    }

    private fun rectMapping(
        fromRect: Rect,
        toRect: Rect,
        input: Rect,
        output: Rect,
    ) {
        val wsc = toRect.width().toFloat() / fromRect.width()
        val hsc = toRect.height().toFloat() / fromRect.height()

        if (wsc >= hsc) {
            val l = input.left * wsc
            val t = input.top * wsc
            val r = input.right * wsc
            val b = input.bottom * wsc
            val d = (fromRect.height() * wsc - toRect.height()) / 2
            output.set(l.toInt(), (t - d).toInt(), r.toInt(), (b - d).toInt())
        } else {
            val l = input.left * hsc
            val t = input.top * hsc
            val r = input.right * hsc
            val b = input.bottom * hsc
            val d = (fromRect.width() * hsc - toRect.width()) / 2
            output.set((l - d).toInt(), t.toInt(), (r - d).toInt(), b.toInt())
        }
    }

}