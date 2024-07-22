package com.example.jakar

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private val ipMachineMap = mapOf(
        "192.168.1.208" to "JAKAR 10"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val listView: ListView = findViewById(R.id.listView)
        val flowLayout: LinearLayout = findViewById(R.id.flowLayout)
        val adapter = ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mutableListOf())
        listView.adapter = adapter

        fetchMachineData(adapter, flowLayout)
    }

    private fun fetchMachineData(adapter: ArrayAdapter<String>, flowLayout: LinearLayout) {
        lifecycleScope.launch {
            for ((ip, machineName) in ipMachineMap) {
                val url = "http://$ip/report.html"
                val (result, result2) = fetchDataFromUrl(url)
                withContext(Dispatchers.Main) {
                    if (result.isNotEmpty() && result2.isNotEmpty()) {
                        adapter.add("MAKİNA ADI: $machineName - DESEN: $result - DURUM: $result2")
                        val indicatorColor = if (result2.contains("Running forward")) Color.GREEN else Color.RED
                        addStatusIndicator(flowLayout, machineName, result2, indicatorColor)
                    } else {
                        adapter.add("MAKİNA ADI: $machineName - Zaman aşımı veya veri alınamadı.")
                        addStatusIndicator(flowLayout, machineName, "Zaman aşımı veya veri alınamadı.", Color.YELLOW)
                    }
                }
            }
        }
    }

    private suspend fun fetchDataFromUrl(url: String): Pair<String, String> {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()

                val request = Request.Builder().url(url).build()
                val response = client.newCall(request).execute()

                if (!response.isSuccessful) {
                    Log.e("HttpError", "HTTP error: ${response.code}")
                    return@withContext Pair("", "HTTP error: ${response.code}")
                }

                val htmlContent = response.body?.string() ?: ""
                Log.d("HTMLContent", htmlContent) // HTML içeriğini log'a yazdır

                val document = Jsoup.parse(htmlContent)
                Log.d("JsoupDebug", "Document parsed successfully.")

                val tdNode = document.select("td[rowspan='4']").first()
                val h3Node = document.select("h3").first()

                val result = tdNode?.text()?.trim() ?: ""
                val result2 = h3Node?.text()?.trim() ?: ""

                Log.d("JsoupDebug", "tdNode text: $result")
                Log.d("JsoupDebug", "h3Node text: $result2")

                Pair(result, result2)
            } catch (e: IOException) {
                Log.e("FetchError", "IOException: ${e.message}")
                Pair("", "IOException: ${e.message}")
            } catch (e: Exception) {
                Log.e("FetchError", "Exception: ${e.message}")
                Pair("", "Exception: ${e.message}")
            }
        }
    }

    private fun addStatusIndicator(flowLayout: LinearLayout, machineName: String, status: String, backgroundColor: Int) {
        val label = TextView(this).apply {
            text = "$machineName - $status"
            setBackgroundColor(backgroundColor)
            textSize = 16f
            setPadding(10, 10, 10, 10)
        }
        flowLayout.addView(label)
    }
}
