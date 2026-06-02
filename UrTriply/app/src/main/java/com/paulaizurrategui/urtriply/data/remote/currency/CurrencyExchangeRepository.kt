package com.paulaizurrategui.urtriply.data.remote.currency

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Path
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class ExchangeRatesResponse(
    @Json(name = "result") val result: String? = null,
    @Json(name = "base_code") val baseCode: String? = null,
    @Json(name = "rates") val rates: Map<String, Double>? = null
)

private interface ExchangeRatesApi {
    @GET("v6/latest/{base}")
    suspend fun latest(@Path("base") base: String): ExchangeRatesResponse
}

// Pequeño repo para convertir precios desde otra divisa a EUR.
// - Usa https://open.er-api.com/ como fuente de rates (endpoints gratuitos)
// - Si falla la consulta o no existe la tasa, retorna el `amount` sin convertir
class CurrencyExchangeRepository {

    companion object {
        private const val TAG = "CurrencyExchangeRepo"
    }

    private val logger = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    private val api: ExchangeRatesApi by lazy {
        val client = OkHttpClient.Builder()
            .addInterceptor(logger)
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()

        Retrofit.Builder()
            .baseUrl("https://open.er-api.com/")
            .client(client)
            .addConverterFactory(retrofit2.converter.moshi.MoshiConverterFactory.create(moshi))
            .build()
            .create(ExchangeRatesApi::class.java)
    }

    suspend fun convertToEur(amount: Double, sourceCurrency: String): Double {
        val normalizedSource = sourceCurrency.trim().uppercase(Locale.getDefault())
        if (normalizedSource.isBlank() || normalizedSource == "EUR") return amount

        return withContext(Dispatchers.IO) {
            try {
                val response = api.latest(normalizedSource)
                val rate = response.rates?.get("EUR")
                if (rate == null || rate <= 0.0) {
                    // Si no hay tasa EUR válida, devuelvo el amount original
                    Log.w(TAG, "Missing EUR rate for $normalizedSource")
                    amount
                } else {
                    amount * rate
                }
            } catch (e: Throwable) {
                // En caso de error en la llamada, no quiero romper el flujo; devuelvo amount
                Log.w(TAG, "Currency conversion failed for $normalizedSource: ${e.message}")
                amount
            }
        }
    }
}