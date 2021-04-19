package com.viatom.lpble.ecg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.TextView
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import com.viatom.lpble.R
import com.viatom.lpble.data.entity.*
import com.viatom.lpble.ext.createFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

/**
 * author: wujuan
 * created on: 2021/4/16 12:31
 * description:
 */



object ReportUtil {
    @SuppressLint("SetTextI18n")
    fun setupRecordInfo(
        reportView: View,
        record: RecordEntity,
        report: ReportEntity,
        user: UserEntity?,
        position: Int
    ) {

        // 时间与时长
        val tv_value_measure_date_time = reportView.findViewById<TextView>(R.id.val_start_time).apply {
            text =
                record.getProcessTime(
                    position,
                    "yyyy-MM-dd HH:mm:ss"
                )
        }
        val tv_value_recording_duration =
            reportView.findViewById<TextView>(R.id.tv_value_recording_duration).apply {
                text =
                    "" + record.getProcessDuration(position)
                        .toString() + "s"
            }

        // 心率+分析结果+Note
        val tv_label_record_hr = reportView.findViewById<TextView>(R.id.tv_label_record_hr).apply {
            report.hr.let {
                if (it.isEmpty()) return@let
                text = if (it.toInt() in 31 until 300) "$it bpm" else "--"
            }
        }

        //时间
        val pattern = "yyyy-M-d HH:mm:ss"
        reportView.findViewById<TextView>(R.id.total_time).apply {
            text = "${record.duration}s"
        }

        reportView.findViewById<TextView>(R.id.val_start_time).apply {
            text = getTimeStr(pattern, record.createTime)
        }

        reportView.findViewById<TextView>(R.id.val_end_time).apply {
            text = getTimeStr(pattern, record.createTime + record.duration)
        }

        reportView.findViewById<TextView>(R.id.val_ai_get_time).apply {
            report.sendTime.let {
                text = it.substring(0, 10.coerceAtMost(it.length))
            }

        }

        //用户信息
        user?.let {
            reportView.findViewById<TextView>(R.id.val_name).apply {
                text = user.name

            }
            reportView.findViewById<TextView>(R.id.val_gender).apply {
                text = user.gender
            }
            reportView.findViewById<TextView>(R.id.val_birthday).apply {
                user.birthday.let {
                    text = it.substring(0, 10.coerceAtMost(it.length))
                }
            }

            reportView.findViewById<TextView>(R.id.val_height).apply {
                text = user.height
            }

            reportView.findViewById<TextView>(R.id.val_weight).apply {
                text = user.weight
            }

        }



    }


    fun saveBitmap2Pdf(context: Context, dir: String, fileName: String, bitmaps: ArrayList<Bitmap>): File? {
        if (bitmaps.isEmpty()) {
            return null
        }
        context.createFile(dir, fileName)?.let{
            val marginVertical = 45f
            val document = Document(PageSize.A4, 0F, 0F, marginVertical, marginVertical)
            try {
                val out = FileOutputStream(it)
                val writer: PdfWriter = PdfWriter.getInstance(document, out)
                val res = context.resources
                val bmp = BitmapFactory.decodeResource(res, R.mipmap.report_logo)
                val baos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                val headerFooter = ItextPdfHeaderFooter(BaseFont.createFont(), baos.toByteArray())
                writer.setPageEvent(headerFooter)
                document.open()
                //            document.setPageSize(PageSize.A4);
                for ( bitmap in bitmaps) {
                    val stream = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                    val byteArray = stream.toByteArray()
                    addImage(document, byteArray)

                    stream.close()
                }
                bitmaps.clear()
                document.newPage()
                document.close()
                out.close()
            } catch (e: DocumentException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return it
        }
        return null

    }


    @Throws(DocumentException::class, IOException::class)
    private fun addImage(document: Document, byteArray: ByteArray): Image{
        return Image.getInstance(byteArray).apply {
            scalePercent(47f)
            document.add(this)
        }
    }


    fun getTimeStr(pattern: String, time: Long): String{
        val simpleDateFormat = SimpleDateFormat(pattern, Locale.getDefault())
        return simpleDateFormat.format(time)
    }

}