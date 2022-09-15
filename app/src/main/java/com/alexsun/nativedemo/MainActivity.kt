package com.alexsun.nativedemo

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentUris
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import java.lang.Exception

class MainActivity : AppCompatActivity(), View.OnClickListener {

    private var imagePath: String? = null
    private var imageName: String? = null
    private var modelPath: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val btnSelect: Button = findViewById(R.id.btnSelect)
        btnSelect.setOnClickListener(this)
        val btnStart: Button = findViewById(R.id.btnStart)
        btnStart.setOnClickListener(this)
        val progressBar: ProgressBar = findViewById(R.id.progressBar)
        progressBar.isVisible = false

        val btn1: RadioButton  = findViewById(R.id.radioButton1)
        val btn2: RadioButton  = findViewById(R.id.radioButton2)
        val btn3: RadioButton  = findViewById(R.id.radioButton3)

        btn1.isChecked = true
        btn2.isEnabled = false
        btn3.isEnabled = false

        val extDir = getExternalFilesDir(null)
        if (extDir == null) {
            Toast.makeText(this, "无法找到目录", Toast.LENGTH_SHORT).show()
        } else {
            val modelPath = extDir.absolutePath + "/models"
            if (!ZipUtils.isPathExist(modelPath)) {
                Toast.makeText(this, "正在创建模型文件", Toast.LENGTH_LONG).show()
                try {
                    ZipUtils.UnZipAssetsFolder(this, "models.zip", modelPath)
                    Toast.makeText(this, "创建完成", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(this, "无法创建模型文件，请重新安装", Toast.LENGTH_SHORT).show()
                }
            }
            this.modelPath = modelPath
        }
    }

    @SuppressLint("SetTextI18n")
    private val getContent = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            Toast.makeText(this, "已经选择图片", Toast.LENGTH_SHORT).show()
            if (DocumentsContract.isDocumentUri(this, uri)) {
                val docId = DocumentsContract.getDocumentId(uri)
                if (uri.authority == "com.android.providers.media.documents") {
                    val id = docId.split(":").toTypedArray()[1]
                    val selection = MediaStore.Images.Media._ID + "=" + id
                    imagePath = getImagePath(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, selection)
                } else if ("com.android.providers.downloads.documents" == uri.authority) {
                    val contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), java.lang.Long.valueOf(docId))
                    imagePath = getImagePath(contentUri, null)
                }
            } else if ("content".equals(uri.scheme, ignoreCase = true)) {
                // 如果是 content 类型的 Uri，则使用普通方式处理
                imagePath = getImagePath(uri, null)
            } else if ("file".equals(uri.scheme, ignoreCase = true)) {
                // 如果是 file 类型的 Uri，直接获取图片路径即可
                imagePath = uri.path
            }
        } else {
            Toast.makeText(this, "没有选择图片", Toast.LENGTH_SHORT).show()
        }
        if (imagePath != null) {
            val textView: TextView = findViewById(R.id.textSelectImg)
            val arr = imagePath!!.split("/")
            imageName = arr[arr.size - 1]
            textView.text = imageName
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onClick(v: View?) {
        when(v?.id) {
            R.id.btnSelect -> {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 1)
                } else {
                    openAlbum()
                }
            }
            R.id.btnStart -> {
                if (imagePath == null || imageName == null) {
                    Toast.makeText(this, "您还没有选择图片", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "任务已经开始，请耐心等待，这可能持续几分钟时间", Toast.LENGTH_LONG).show()
                    val extDir = getExternalFilesDir(null)
                    if (extDir == null) {
                        Toast.makeText(this, "应用数据被删除，请重新安装尝试解决", Toast.LENGTH_SHORT).show()
                    } else {
                        val esrganRadio: RadioButton  = findViewById(R.id.radioButton1)
                        var srModelName = ""
                        val argvList = mutableListOf("model", "-i", imagePath!!, "-o", extDir.absolutePath + "/" + imageName + ".png")
                        if (esrganRadio.isChecked) {
                            srModelName = "esrgan"
                            argvList.addAll(arrayOf(
                                "-m",
                                modelPath + "/models",
                                "-n",
                                "realesrgan-x4plus-anime"
                            ))
                        } else {
                            srModelName = "waifu2x"
                            argvList.addAll(arrayOf(
                                "-m",
                                modelPath + "/models-upconv_7_anime_style_art_rgb",
                                "-n",
                                "3"
                            ))
                        }
                        val progressBar: ProgressBar = findViewById(R.id.progressBar)
                        progressBar.isVisible = true
                        argvList[0] = srModelName
                        Log.i("MODEL", modelPath!!)
                        val argv = argvList.toTypedArray()
                        Thread() {
                            Log.i("MODEL", argvList.toString())
                            val res = stringFromJNI(argv.size, argv)
                            Log.i("MODEL", res)
                            runOnUiThread(Runnable() {
                                progressBar.isVisible = false
                            })
                        }.start()
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            1 -> if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openAlbum()
            } else {
                Toast.makeText(this, "必须授权才能读取图片", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("Range")
    private fun getImagePath(uri: Uri?, selection: String?): String? {
        var path: String? = null
        // 通过Uri和selection来获取真实的图片路径
        val cursor = contentResolver.query(uri!!, null, selection, null, null)
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
            }
            cursor.close()
        }
        return path
    }

    private fun openAlbum() {
        getContent.launch("image/*")
    }

    external fun stringFromJNI(argc: Int, argv: Array<String>): String

    companion object {
        // Used to load the 'nativedemo' library on application startup.
        init {
            System.loadLibrary("nativedemo")
        }
    }
}
