package ru.brauer.httpclientcache

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.logging.HttpLoggingInterceptor
import ru.brauer.httpclientcache.databinding.ActivityMainBinding
import ru.brauer.httpclientcache.mycache.CachePostResponseInterceptor
import ru.brauer.httpclientcache.mycache.TransformPostRequestInterceptor
import java.io.IOException
import java.util.regex.Pattern

class MainActivity : AppCompatActivity() {

    private val binding: ActivityMainBinding by lazy {
        ActivityMainBinding.inflate(layoutInflater)
    }

    private lateinit var okHttpClient: OkHttpClient
    private val preference: SharedPreferences by lazy { getPreferences(MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)

        okHttpClient = OkHttpClient.Builder()
            .cache(Cache(cacheDir, CACHE_MAX_SIZE))
            .addInterceptor(TransformPostRequestInterceptor())
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            })
            .addNetworkInterceptor(CachePostResponseInterceptor())
            .build()

        with(binding) {
            sendGetRequest.setOnClickListener {
                val url = getUrl() ?: return@setOnClickListener
                val request = Request.Builder()
                    .url(url)
                    .get()
                    .build()
                callRequest(request)
            }
            sendPostRequest.setOnClickListener {
                val url = getUrl() ?: return@setOnClickListener
                val request = Request.Builder()
                    .url(url)
                    .post("{\"fiels\":3}".toRequestBody())
                    .build()
                callRequest(request)
            }
        }
        binding.inputIpAddress.setText(getAddress())
    }

    private fun callRequest(request: Request) = with(binding.resultTextView) {
        okHttpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                post {
                    text = e.message
                }
            }

            override fun onResponse(call: Call, response: Response) {
                post {
                    text = response.body?.string()
                }
            }
        })
    }

    private fun getUrl(): String? =
        binding.inputIpAddress
            .text
            .toString()
            .takeIf { PATTERN_IP_ADDRESS.matcher(it).matches() }
            ?.also { saveAddress(it) }
            ?.let { "http://$it:8000/test/cache" }
            ?: run {
                Toast.makeText(this, "IP адрес некорректный", Toast.LENGTH_SHORT).show()
                null
            }

    private fun saveAddress(address: String) {
        val currentIp = getAddress()
        if (currentIp == address) return
        preference
            .edit()
            .putString(IP_ADDRESS_PREFERENCE_KEY, address)
            .apply()
    }

    private fun getAddress(): String = preference.getString(IP_ADDRESS_PREFERENCE_KEY, "") ?: ""
}

private const val CACHE_MAX_SIZE: Long = 10 * 1024 * 1024
private const val IP_ADDRESS_PREFERENCE_KEY = "IP_ADDRESS_PREFERENCE_KEY"
private val PATTERN_IP_ADDRESS = Pattern.compile(
    "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9])\\.(25[0-5]|2[0-4]"
            + "[0-9]|[0-1][0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1]"
            + "[0-9]{2}|[1-9][0-9]|[1-9]|0)\\.(25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}"
            + "|[1-9][0-9]|[0-9]))"
)