package com.Jorge.asistentevoz.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// ==========================================
// 1. MODELOS DE DATOS (Lo que responde internet)
// ==========================================
data class NewsResponse(val status: String, val articles: List<Article>)
data class Article(
    val title: String?,
    val description: String?,
    val urlToImage: String?,
    val url: String?,
    val source: Source?
)
data class Source(val name: String?)

// ==========================================
// 2. EL TRADUCTOR DE RETROFIT
// ==========================================
interface NewsApiService {
    // Buscamos noticias de tecnología, software o Android en español
    @GET("v2/everything")
    suspend fun getTechNews(
        @Query("q") query: String = "tecnología OR software OR android",
        @Query("language") language: String = "es",
        @Query("sortBy") sortBy: String = "publishedAt",
        @Query("apiKey") apiKey: String = "53a87c7ee3df4acd97978a2964925758" // ¡Tu llave mágica!
    ): NewsResponse
}

// ==========================================
// 3. EL CEREBRO (ViewModel)
// ==========================================
class NewsViewModel : ViewModel() {
    val newsList = mutableStateListOf<Article>()
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf("")

    // Inicializamos la conexión a los servidores de NewsAPI
    private val api = Retrofit.Builder()
        .baseUrl("https://newsapi.org/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(NewsApiService::class.java)

    init {
        descargarNoticias()
    }

    private fun descargarNoticias() {
        viewModelScope.launch {
            try {
                isLoading.value = true
                val response = api.getTechNews()

                // Filtramos las noticias que no traen imagen para que la app siempre se vea hermosa
                val validArticles = response.articles.filter {
                    !it.title.isNullOrEmpty() && !it.urlToImage.isNullOrEmpty() && it.title != "[Removed]"
                }

                newsList.clear()
                newsList.addAll(validArticles)
                isLoading.value = false
            } catch (e: Exception) {
                errorMessage.value = "Error al conectar con el servidor: ${e.message}"
                isLoading.value = false
            }
        }
    }
}