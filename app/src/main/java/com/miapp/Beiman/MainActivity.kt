package com.miapp.Beiman

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.*
import com.miapp.Beiman.enlaces_internos.AuthManager
import com.miapp.Beiman.enlaces_internos.WebViewURL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Comprobamos si existen credenciales almacenadas en lugar de verificar un token
        val (storedUser, storedPassword) = AuthManager.getUserCredentials(this)

        if (storedUser.isNotEmpty() && storedPassword.isNotEmpty()) {
            // Si existen credenciales, redirigimos a Fichar directamente
            navigateToFichar(storedUser, storedPassword)
        } else {
            // Si no existen credenciales, mostramos la pantalla de inicio de sesión
            setContent {
                MaterialTheme {
                    val navController = rememberNavController()

                    NavHost(navController = navController, startDestination = "login") {
                        composable("login") {
                            DisplayLogo(
                                onSubmit = { usuario: String, password: String ->
                                    if (usuario.isNotEmpty() && password.isNotEmpty()) {
                                        lifecycleScope.launch(Dispatchers.IO) {
                                            try {
                                                val (success, xEmpleado) = AuthManager.authenticateUser(this@MainActivity, usuario, password)
                                                runOnUiThread {
                                                    if (success) {
                                                        // Guardamos únicamente las credenciales de usuario y contraseña
                                                        AuthManager.saveUserCredentials(this@MainActivity, usuario, password)
                                                        navController.navigate("fichar/$usuario/$password")
                                                    } else {
                                                        Toast.makeText(
                                                            this@MainActivity,
                                                            "Usuario o contraseña incorrectos",
                                                            Toast.LENGTH_SHORT
                                                        ).show()
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                runOnUiThread {
                                                    Toast.makeText(
                                                        this@MainActivity,
                                                        "Error de autenticación: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    } else {
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Por favor, completa ambos campos",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onForgotPassword = {
                                    val url = WebViewURL.forgotPassword  // Usa la constante definida en URL.kt
                                    val intent = Intent(
                                        Intent.ACTION_VIEW,
                                        android.net.Uri.parse(url)
                                    )
                                    startActivity(intent)
                                }
                            )
                        }
                        composable("fichar/{usuario}/{password}") { backStackEntry ->
                            val usuario = backStackEntry.arguments?.getString("usuario") ?: ""
                            val password = backStackEntry.arguments?.getString("password") ?: ""
                            FicharScreen(usuario = usuario, password = password)
                        }
                    }
                }
            }
        }
    }

    private fun navigateToFichar(usuario: String, password: String) {
        val intent = Intent(this, Fichar::class.java)
        intent.putExtra("usuario", usuario)
        intent.putExtra("password", password)
        startActivity(intent)
        finish()  // Cierra MainActivity para evitar volver al login
    }
}

// Función composable para la pantalla de login
@Composable
fun DisplayLogo(
    onSubmit: (String, String) -> Unit,
    onForgotPassword: () -> Unit
) {
    val usuario = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }

    // Estado del CheckBox para guardar datos localmente
    var isChecked by remember { mutableStateOf(false) }

    // Estado del CheckBox para mostrar/ocultar contraseña
    var passwordVisible by remember { mutableStateOf(false) }

    // Mensaje de error
    var errorMessage by remember { mutableStateOf("") }

    // Obtén el contexto dentro del composable
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(modifier = Modifier.padding(24.dp)) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .width(200.dp)
                        .height(60.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de usuario
                OutlinedTextField(
                    value = usuario.value,
                    onValueChange = { newValue ->
                        // Se eliminan los espacios, pero se permiten todos los demás caracteres, incluyendo signos.
                        usuario.value = newValue.filter { it != ' ' }
                    },
                    label = { Text("Usuario") },
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Next)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Campo de contraseña con opción de mostrar/ocultar
                OutlinedTextField(
                    value = password.value,
                    onValueChange = { newValue ->
                        // Se eliminan los espacios, pero se permiten todos los demás caracteres, incluyendo signos.
                        password.value = newValue.filter { it != ' ' }
                    },
                    label = { Text("Contraseña") },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done)
                )

                // Checkbox para "Mostrar Contraseña"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = passwordVisible,
                        onCheckedChange = { passwordVisible = it }
                    )
                    Text(
                        text = "Mostrar contraseña",
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // CheckBox para dar consentimiento de guardar datos
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it }
                    )
                    Text(
                        text = "Doy mi consentimiento para guardar mis datos localmente en mi dispositivo.",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botón de acceso
                Button(
                    onClick = {
                        val trimmedUsuario = usuario.value.trim()
                        val trimmedPassword = password.value.trim()

                        // Validar si hay espacios al principio o al final
                        if (trimmedUsuario != usuario.value || trimmedPassword != password.value) {
                            errorMessage = "No se permiten espacios al principio o al final"
                            return@Button
                        }

                        if (trimmedUsuario.isNotEmpty() && trimmedPassword.isNotEmpty()) {
                            if (isChecked) {
                                AuthManager.saveUserCredentials(context, trimmedUsuario, trimmedPassword)
                            }
                            onSubmit(trimmedUsuario, trimmedPassword)
                        } else {
                            errorMessage = "Por favor, completa ambos campos"
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7599B6)),
                    modifier = Modifier.fillMaxWidth(),
                    enabled = usuario.value.isNotEmpty() && password.value.isNotEmpty() && isChecked
                ) {
                    Text("Acceso", color = Color.White)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "¿Olvidaste la contraseña?",
                    color = Color(0xFF7599B6),
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable { onForgotPassword() }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = """
                        Para control de calidad y aumentar la seguridad de nuestro sistema, todos los accesos, acciones, consultas o cambios (Trazabilidad) que realice dentro de Kairos24h serán almacenados.
                        Les recordamos que la Empresa podrá auditar los medios técnicos que pone a disposición del Trabajador para el desempeño de sus funciones.
                    """.trimIndent(),
                    color = Color(0xFF447094),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 25.dp)
                        .padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    MaterialTheme {
        DisplayLogo(onSubmit = { _: String, _: String -> }, onForgotPassword = {})
    }
}
