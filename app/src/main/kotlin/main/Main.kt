package main

import android.app.*
import android.os.*
import android.widget.*
import com.example.update_build_date_plugin.*


class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Toast.makeText(this, getString(R.string.build_time_epoc_seconds), Toast.LENGTH_LONG).show()
    }
}
