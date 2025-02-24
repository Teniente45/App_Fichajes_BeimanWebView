package com.miapp.beiman.enlaces_internos

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

object AuthManager {

    // Obtener el xEmpleado desde SharedPreferences
    fun getXEmpleado(context: Context): String? {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        return sharedPreferences.getString("xEmpleado", null)
    }

    // Obtener las credenciales del usuario desde SharedPreferences
    fun getUserCredentials(context: Context): Pair<String, String> {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val usuario = sharedPreferences.getString("usuario", "") ?: ""
        val password = sharedPreferences.getString("password", "") ?: ""
        return Pair(usuario, password)
    }

    // Guardar las credenciales del usuario en SharedPreferences
    fun saveUserCredentials(context: Context, usuario: String, password: String) {
        val sharedPreferences = context.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString("usuario", usuario)
        editor.putString("password", password)
        editor.apply()
    }

    // Método para realizar el login y obtener el xEmpleado
    suspend fun authenticateUser(context: Context, usuario: String, password: String): Pair<Boolean, String?> {
        val client = OkHttpClient()
        val url = BuildURL.LOGIN_URL +
                "loginExterno&cUsuario=$usuario&tPassword=$password"
        Log.d("AuthManager", "URL: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        return try {
            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("AuthManager", "Response Body: $responseBody")
                val jsonResponse = JSONObject(responseBody)
                val code = jsonResponse.optInt("code", -1)
                val xEmpleado = jsonResponse.optString("xEmpleado", null) // Obtener xEmpleado del JSON
                if (code == 1) {
                    // Si la respuesta es exitosa (code == 1), se devuelve xEmpleado sin gestionar ningún token.
                    Pair(true, xEmpleado)
                } else {
                    Pair(false, null)
                }
            } else {
                Log.d("AuthManager", "Request failed with status: ${response.code}")
                Pair(false, null)
            }
        } catch (e: Exception) {
            Log.e("AuthManager", "Error de autenticación: ", e)
            Pair(false, null)
        }
    }
}
