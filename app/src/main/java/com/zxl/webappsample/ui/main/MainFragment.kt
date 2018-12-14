package com.zxl.webappsample.ui.main

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.zxl.webappsample.R
import com.zxl.webappsample.WebActivity
import kotlinx.android.synthetic.main.main_fragment.*

const val WEB_URL = "web_url"

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        viewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        // TODO: Use the ViewModel
        btnGoLocal.setOnClickListener {
            var intent = Intent(context, WebActivity::class.java)
            startActivity(intent)
        }
        etUrl.setText("http://")
        btnGo.setOnClickListener {
            var intent = Intent(context, WebActivity::class.java)
            intent.putExtra(WEB_URL, etUrl.text.toString())
            startActivity(intent)
        }

    }

}
