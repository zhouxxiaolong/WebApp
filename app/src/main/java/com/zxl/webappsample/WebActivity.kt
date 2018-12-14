package com.zxl.webappsample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.widget.Toast
import com.github.lzyzsd.jsbridge.BridgeWebView
import com.github.lzyzsd.jsbridge.DefaultHandler
import com.google.gson.Gson
import com.tbruyelle.rxpermissions2.RxPermissions
import com.zxl.webappsample.ui.main.WEB_URL
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*


/**
 * Created
 * 创 建 人: @author zhouxiaolong
 * 创建日期: 2018/11/14
 * 邮   箱: 1016579848@qq.com
 * 参   考: @link
 * 描   述:
 */
class WebActivity : AppCompatActivity() {

    private lateinit var mWebView: BridgeWebView

    var RESULT_CODE = 0

    private val multiple_files = false

    private var mCM: String? = null

    var mUploadMessage: ValueCallback<Uri>? = null

    var mUploadMessageArray: ValueCallback<Array<Uri>>? = null

    var mLocation: Location? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_activity)

        val rxPermissions = RxPermissions(this)
        mWebView = findViewById(R.id.webView)
        mWebView.setDefaultHandler(DefaultHandler())

        initWebViewSetting()

        mWebView.setDefaultHandler(DefaultHandler())

        rxPermissions
            .request(
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            .subscribe { granted ->
                if (granted) { // Always true pre-M
                    openGPSSettings(this)
                    val locationResult = object : MyLocation.LocationResult() {
                        override fun gotLocation(location: android.location.Location?) {
                            if (null != location) {
                                mLocation = Location(location.longitude, location.latitude)
                            }
                        }

                    };
                    val myLocation = MyLocation();
                    myLocation.getLocation(this, locationResult);
                } else {
                    // Oups permission denied
                }
            }

        mWebView.webChromeClient = object : WebChromeClient() {

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, AcceptType: String, capture: String) {
                this.openFileChooser(uploadMsg)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>, AcceptType: String) {
                this.openFileChooser(uploadMsg)
            }

            fun openFileChooser(uploadMsg: ValueCallback<Uri>) {
                mUploadMessage = uploadMsg
                pickFile()
            }

            override fun onShowFileChooser(
                webView: WebView,
                filePathCallback: ValueCallback<Array<Uri>>,
                fileChooserParams: WebChromeClient.FileChooserParams
            ): Boolean {
                mUploadMessageArray = filePathCallback
                takeAndPickPhoto()
                return true
            }
        }

        mWebView.loadUrl(intent.getStringExtra(WEB_URL) ?: "file:///android_asset/echo.html")

        mWebView.callHandler("setData", Gson().toJson(Bean("原生传入的", "东西", "值"))) { }

        mWebView.registerHandler("getUserId") { data, function -> function!!.onCallBack("12312312123") }

        mWebView.registerHandler("getLocation") { data, function ->
            function!!.onCallBack(
                Gson().toJson(
                    mLocation
                )
            )
        }

    }

    private fun initWebViewSetting() {
        val webSettings = mWebView.getSettings()
        webSettings.setJavaScriptEnabled(true)
        webSettings.setJavaScriptCanOpenWindowsAutomatically(true)
        webSettings.setCacheMode(WebSettings.LOAD_NO_CACHE)
        webSettings.setDomStorageEnabled(true)
        webSettings.setDatabaseEnabled(true)
        webSettings.setAppCacheEnabled(true)
        webSettings.setAllowFileAccess(true)
        webSettings.setSupportZoom(false)
        webSettings.setBuiltInZoomControls(false)

        webSettings.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS)
        webSettings.setUseWideViewPort(true)
    }

    fun takeAndPickPhoto() {
        var takePictureIntent: Intent? = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (takePictureIntent!!.resolveActivity(getPackageManager()) != null) {
            var photoFile: File? = null
            try {
                photoFile = createImageFile()
                takePictureIntent.putExtra("PhotoPath", mCM)
            } catch (ex: IOException) {
                Log.e("CommonWebViewActivity", "Image file creation failed", ex)
            }

            if (photoFile != null) {
                mCM = "file:" + photoFile.absolutePath
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile))
            } else {
                takePictureIntent = null
            }
        }
        val contentSelectionIntent = Intent(Intent.ACTION_GET_CONTENT)
        contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE)
        contentSelectionIntent.type = "image/*"
        if (multiple_files) {
            contentSelectionIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        val intentArray: Array<Intent?>
        if (takePictureIntent != null) {
            intentArray = arrayOf(takePictureIntent)
        } else {
            intentArray = arrayOfNulls(0)
        }

        val chooserIntent = Intent(Intent.ACTION_CHOOSER)
        chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent)
        chooserIntent.putExtra(Intent.EXTRA_TITLE, "Image Chooser")
        chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray)
        if (multiple_files && Build.VERSION.SDK_INT >= 18) {
            chooserIntent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        startActivityForResult(chooserIntent, RESULT_CODE)

    }

    fun pickFile() {
        val chooserIntent = Intent(Intent.ACTION_GET_CONTENT)
        chooserIntent.type = "image/*"
        startActivityForResult(chooserIntent, RESULT_CODE)
    }

    private fun createImageFile(): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val imageFileName = "img_" + timeStamp + "_"
        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(imageFileName, ".jpg", storageDir)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        if (null == intent) {
            mUploadMessageArray?.onReceiveValue(null)
            return
        }

        if (requestCode == RESULT_CODE) {
            if (null == mUploadMessage && null == mUploadMessageArray) {
                return
            }
            if (null != mUploadMessage && null == mUploadMessageArray) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                mUploadMessage?.onReceiveValue(result)
                mUploadMessage = null
            }

            if (null == mUploadMessage && null != mUploadMessageArray) {
                val result = if (intent == null || resultCode != Activity.RESULT_OK) null else intent.data
                if (null != result) {
                    mUploadMessageArray?.onReceiveValue(arrayOf<Uri>(result))
                } else {
                    mUploadMessageArray?.onReceiveValue(arrayOf(Uri.parse(mCM)))
                }
                mUploadMessageArray = null
            }
        } else {
            mUploadMessage = null
            mUploadMessageArray = null
        }
    }

    fun openGPSSettings(context: Activity) {
        val alm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (alm.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
//            Toast.makeText(context, "GPS模块正常", Toast.LENGTH_SHORT)
//                    .show();
            return
        }

        Toast.makeText(context, " 请开启GPS！ ", Toast.LENGTH_SHORT).show()
        val intent = Intent(Settings.ACTION_SECURITY_SETTINGS)
        context.startActivityForResult(intent, 0) // 此为设置完成后返回到获取界面

    }


    class Bean(val parameter_name: String, val alias_name: String, val value: String)

}