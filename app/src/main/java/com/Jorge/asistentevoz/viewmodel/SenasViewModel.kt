package com.Jorge.asistentevoz.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

// Así es como Firebase entenderá cada GIF que descargue
data class SenaGif(
    val nombre: String = "",
    val titulo: String = "", // Otra posible variante
    val name: String = "",   // Por si se escribió en inglés
    val url_gif: String = ""
) {
    // Esta variable extrae el nombre válido sin importar cómo esté en Firebase
    val displayNombre: String
        get() = nombre.ifEmpty { titulo }.ifEmpty { name }.ifEmpty { "Con gusto" }
}

class SenasViewModel : ViewModel() {
    // La lista vacía que se llenará con los datos de internet
    val listaSenas = mutableStateListOf<SenaGif>()
    
    // Estados para retroalimentación en la interfaz
    val isLoading = mutableStateOf(true)
    val errorMessage = mutableStateOf("")

    // Conexión a tu base de datos
    private val db = FirebaseFirestore.getInstance()

    // --- ESTADOS DEL DICCIONARIO (ABECEDARIO) ---
    val textoAbecedario = mutableStateOf("")
    val selectedTabIndex = mutableStateOf(0)

    fun actualizarTextoAbecedario(nuevoTexto: String) {
        textoAbecedario.value = nuevoTexto
    }

    fun cambiarPestana(index: Int) {
        selectedTabIndex.value = index
    }

    init {
        descargarGifsDesdeFirebase()
    }

    fun descargarGifsDesdeFirebase() {
        isLoading.value = true
        errorMessage.value = ""
        // Busca en la colección "senas" que creaste en la consola
        db.collection("senas")
            .get()
            .addOnSuccessListener { result ->
                listaSenas.clear()
                for (document in result) {
                    // Transforma el documento de internet a nuestro objeto SenaGif
                    val sena = document.toObject(SenaGif::class.java)
                    listaSenas.add(sena)
                }
                isLoading.value = false
                Log.d("Firebase", "¡Se descargaron ${listaSenas.size} GIFs correctamente!")
            }
            .addOnFailureListener {
                isLoading.value = false
                errorMessage.value = "Error al conectar con el servidor: ${it.message}"
                Log.e("Firebase", "Error al descargar los GIFs", it)
            }
    }
}