package com.viatom.lpble.ecg

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import com.itextpdf.text.Document
import com.itextpdf.text.DocumentException
import com.itextpdf.text.Image
import com.itextpdf.text.PageSize
import com.itextpdf.text.pdf.BaseFont
import com.itextpdf.text.pdf.PdfWriter
import com.viatom.lpble.R
import com.viatom.lpble.ble.DataController.hr
import com.viatom.lpble.constants.Constant
import com.viatom.lpble.data.entity.*
import com.viatom.lpble.ext.createFile
import com.viatom.lpble.viewmodels.ReportDetailViewModel
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList

/**
 * author: wujuan
 * created on: 2021/4/16 12:31
 * description:
 */



object ReportUtil {
    const val Logs: String ="ReportUtil"

    fun inflaterReportView(context: Context,record: RecordEntity, report: ReportEntity, userEntity: UserEntity): ArrayList<View> {

        return ArrayList<View>().also { viewList ->
            val inflater = LayoutInflater.from(context)
            val params = RelativeLayout.LayoutParams(
                Constant.Report.A4_WIDTH,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            //添加基本信息
            inflater.inflate(R.layout.widget_report, null).also { reportView ->
                reportView.layoutParams = params

                setupRecordInfo(
                    reportView,
                    record,
                    report,
                    userEntity,
                    0
                )
                Log.d(Logs,"view: record info end" )
                viewList.add(reportView)
            }

            //添加结果和建议
            report.aiResultList?.let { aiResList ->
                for (item in aiResList) {

                    inflater.inflate(R.layout.report_diagnose_content_item, null)
                        .let { diagnoseView ->
                            diagnoseView.layoutParams = params
                            diagnoseView.findViewById<TextView>(R.id.diagnose).apply {
                                this.text = item.aiDiagnosis
                            }

                            diagnoseView.findViewById<TextView>(R.id.advice_content).apply {
                                this.text = item.phoneContent.replace("\\n", "\n")
                            }
                            viewList.add(diagnoseView)
                        }
                }

                Log.d(Logs,"view: aiResultList end" )
            }

            report.fragmentList?.let { frags ->
                //添加波形片段
                val scale = 6.0f
                val imgWidth = scale * 25 * 7.8f //25mm/s，每行7秒，每mm对应11.8f //39格子
                val imgHeight = scale * 10 * 6f //每mm对应11.8f*10mm/mV*每行2.5mV*9行//12格子

                frags.sortedBy { it.startPose.toInt() }.let { fragmentList ->
                    for (i in fragmentList.indices) {
                        fragmentList[i].let { frag ->

                            Log.d(Logs, "load fragment : $frag")

                            inflater.inflate(R.layout.report_list_item, null, false).also { itemView ->
                                itemView.findViewById<TextView>(R.id.name_val).apply {
                                    this.text = frag.name
                                }

                                itemView.findViewById<TextView>(R.id.hr_val).apply {
                                    this.text = "(${frag.hr}bpm)"
                                }

                                //填写片段起始时间
                                itemView.findViewById<TextView>(R.id.time_val).apply {
                                    val startPoint: Int = frag.startPose.toInt() / 2
                                    val time = startPoint / 125 + record.createTime / 1000
                                    val date = Date(TimeUnit.SECONDS.toMillis(time))

                                    Log.d(
                                        Logs,
                                        "record createTime $time"
                                    )
                                    val timeStr = SimpleDateFormat(
                                        "yyyy-MM-dd HH:mm:ss",
                                        Locale.getDefault()
                                    ).format(date)
                                    this.text = timeStr

                                    //获取当前片段对应的标记
                                    itemView.findViewById<RelativeLayout>(R.id.rl_wave)
                                        .let { waveLayout ->

                                            val layoutParams = RelativeLayout.LayoutParams(
                                                Constant.Report.A4_WIDTH, 390
                                            ).apply {
                                                addRule(
                                                    RelativeLayout.CENTER_HORIZONTAL,
                                                    RelativeLayout.TRUE
                                                )
                                            }

                                            FilterECGReportWave(
                                                context,
                                                record,
                                                report,
                                                imgWidth,
                                                imgHeight,
                                                0,
                                                frag,
                                                startPoint,
                                            ).apply {
                                                this.layoutParams = layoutParams
                                                this.invalidate()
                                                waveLayout.addView(this)
                                            }


                                        }


                                }


                                viewList.add(itemView)

                            }
                        }


                    }

                    Log.d(Logs,"view: fragmentList end" )
                }




            }

            //添加报告说明
            inflater.inflate(R.layout.report_tip_item, null).apply {
                this.layoutParams = params
                viewList.add(this)
                Log.d(Logs,"view: report_tip_item end" )
            }

        }
    }

    @SuppressLint("SetTextI18n")
    fun setupRecordInfo(
        reportView: View,
        record: RecordEntity,
        report: ReportEntity,
        user: UserEntity?,
        position: Int
    ) {
        Log.d(Logs, "start setup record Info...")
        //时间
        val pattern = "yyyy-M-d HH:mm:ss"
        reportView.findViewById<TextView>(R.id.total_time).apply {
            text = "${record.duration}s"
        }

        reportView.findViewById<TextView>(R.id.val_start_time).apply {
            text = getTimeStr(pattern, record.createTime)
        }

        reportView.findViewById<TextView>(R.id.val_end_time).apply {
            text = getTimeStr(pattern, record.createTime + record.duration * 1000)
        }

        // 心率+分析结果+Note
        reportView.findViewById<TextView>(R.id.tv_label_record_hr).apply {
            report.hr.let {
                if (it.isEmpty()) return@let
                text = if (it.toInt() in 31 until 300) "$it bpm" else "--"
            }
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

//            reportView.findViewById<TextView>(R.id.val_height).apply {
//                text = user.height
//            }
//
//            reportView.findViewById<TextView>(R.id.val_weight).apply {
//                text = user.weight
//            }

        }



    }

    fun makeRecordReport(
        context: Context,
        dir: String,
        fileName: String,
        reportView: ArrayList<View>
    ): File? {
        val bitmap: ArrayList<Bitmap> = convertView2Bitmap(reportView)
        return saveBitmap2Pdf(context, dir, fileName, bitmap)
    }

    private fun convertView2Bitmap(views: ArrayList<View>): ArrayList<Bitmap>{
        Log.d(Logs, "convertView2Bitmap views size:${views.size}")

        val bitmaps = ArrayList<Bitmap>()
        for (view in views) {
            view.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED)
            val width = view.measuredWidth.toFloat()
            val height = view.measuredHeight
            Log.d(Logs, "convertView2Bitmap width $width, height $height")
            Constant.Report.A4_HEIGHT = (Constant.Report.A4_WIDTH * height / width).toInt()
            view.measure(
                View.MeasureSpec.makeMeasureSpec(
                    Constant.Report.A4_WIDTH,
                    View.MeasureSpec.EXACTLY
                ),
                View.MeasureSpec.makeMeasureSpec(
                    Constant.Report.A4_HEIGHT,
                    View.MeasureSpec.EXACTLY
                )
            )
            val b = Bitmap.createBitmap(
                view.measuredWidth,
                view.measuredHeight,
                Bitmap.Config.ARGB_8888
            )
            val c = Canvas(b)
            view.layout(0, 0, view.measuredWidth, view.measuredHeight)
            //        view.layout(0, 0, view.getLayoutParams().width, view.getLayoutParams().height);
            try {
                view.draw(c)


            } catch (e: Exception) {
                Log.e("Exception e", "" + e.message)
            }
            bitmaps.add(b)
        }

        Log.d(Logs, "convertView2Bitmap bitmaps size:${bitmaps.size}")
        return bitmaps

    }


    fun  saveBitmap2Pdf(context: Context, dir: String, fileName: String, bitmaps: ArrayList<Bitmap>): File? {
        Log.d(Logs, "start saveBitmap2Pdf....$dir, $fileName, ${bitmaps.isNullOrEmpty()}")
        if (bitmaps.isEmpty()) {
            return null
        }
        context.createFile(dir, fileName)?.let{

            Log.d(Logs, "saveBitmap2Pdf....createFile ${it.absolutePath}")

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