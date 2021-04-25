package com.viatom.lpble.ui

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.viatom.lpble.R
import com.viatom.lpble.data.entity.UserEntity
import com.viatom.lpble.ui.MainActivity

class WelcomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        findViewById<TextView>(R.id.enter).setOnClickListener {

            Intent(this, MainActivity::class.java ).let {
                Bundle().apply {
                    putParcelable("userEntity", UserEntity(1001, "小明","188", "78", "1992-9-23","男"))
                    it.putExtras(this)
                }
                startActivity(it)
            }
        }

    }
}