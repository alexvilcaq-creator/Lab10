package com.tecsup.lab10.view

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tecsup.lab10.data.PostApiService
import com.tecsup.lab10.data.PostModel
import kotlinx.coroutines.launch

// La API de prueba no guarda cambios, por eso se mantiene una lista local
// que se actualiza cuando la API responde correctamente a cada verbo.
object PostsCache {
    val lista = mutableStateListOf<PostModel>()
    var cargado = false
}

// ---------- LISTADO ----------
@Composable
fun ContenidoPostsListado(navController: NavHostController, servicio: PostApiService) {
    LaunchedEffect(Unit) {
        if (!PostsCache.cargado) {
            try {
                val listado = servicio.selectPosts()
                PostsCache.lista.clear()
                PostsCache.lista.addAll(listado)
                PostsCache.cargado = true
            } catch (e: Exception) {
                Log.e("POSTS-API", "Error al cargar posts: ${e.message}", e)
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(PostsCache.lista) { post ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${post.id}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 12.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.title,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = post.body,
                            fontSize = 13.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(onClick = { navController.navigate("postVer/${post.id}") }) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(onClick = { navController.navigate("postDel/${post.id}") }) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

// ---------- CREAR / EDITAR ----------
@Composable
fun ContenidoPostEditar(navController: NavHostController, servicio: PostApiService, pid: Int = 0) {
    var userId by remember { mutableStateOf("1") }
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(pid) {
        if (pid != 0) {
            val local = PostsCache.lista.find { it.id == pid }
            if (local != null) {
                userId = local.userId.toString()
                title = local.title
                body = local.body
            } else {
                try {
                    val post = servicio.selectPost(pid.toString()).body()
                    if (post != null) {
                        userId = post.userId.toString()
                        title = post.title
                        body = post.body
                    }
                } catch (e: Exception) {
                    Log.e("POSTS-API", "Error al cargar el post: ${e.message}", e)
                    mensaje = "No se pudo cargar el post"
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = if (pid == 0) "Nuevo post" else "Editar post #$pid",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = userId,
            onValueChange = { userId = it },
            label = { Text("ID de usuario (número)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Título") },
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = body,
            onValueChange = { body = it },
            label = { Text("Contenido") },
            minLines = 3,
            modifier = Modifier.fillMaxWidth()
        )

        if (mensaje.isNotEmpty()) {
            Text(text = mensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val userIdInt = userId.toIntOrNull()
                    if (title.isBlank() || body.isBlank() || userIdInt == null) {
                        mensaje = "Completa todos los campos (el ID de usuario debe ser un número)"
                    } else {
                        mensaje = ""
                        scope.launch {
                            try {
                                val post = PostModel(userIdInt, pid, title, body)
                                val respuesta = if (pid == 0)
                                    servicio.insertPost(post)
                                else
                                    servicio.updatePost(pid.toString(), post)

                                if (respuesta.isSuccessful) {
                                    Log.e("POSTS-API", "Respuesta ${respuesta.code()}: ${respuesta.body()}")
                                    if (pid == 0) {
                                        // La API simula el alta, así que se agrega a la lista local
                                        val nuevoId = (PostsCache.lista.maxOfOrNull { it.id } ?: 0) + 1
                                        PostsCache.lista.add(0, PostModel(userIdInt, nuevoId, title, body))
                                    } else {
                                        val pos = PostsCache.lista.indexOfFirst { it.id == pid }
                                        if (pos >= 0) {
                                            PostsCache.lista[pos] = PostModel(userIdInt, pid, title, body)
                                        }
                                    }
                                    navController.navigate("posts")
                                } else {
                                    mensaje = "Error del servidor: ${respuesta.code()}"
                                }
                            } catch (e: Exception) {
                                Log.e("POSTS-API", "Error al grabar: ${e.message}", e)
                                mensaje = "No se pudo conectar con el servidor"
                            }
                        }
                    }
                }
            ) {
                Text("Grabar", fontSize = 16.sp)
            }
            OutlinedButton(onClick = { navController.navigate("posts") }) {
                Text("Cancelar", fontSize = 16.sp)
            }
        }
    }
}

// ---------- ELIMINAR ----------
@Composable
fun ContenidoPostEliminar(navController: NavHostController, servicio: PostApiService, id: Int) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { navController.navigate("posts") },
        title = { Text(text = "Confirmar eliminación") },
        text = { Text("¿Está seguro de eliminar el post #$id?") },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            val respuesta = servicio.deletePost(id.toString())
                            if (respuesta.isSuccessful) {
                                PostsCache.lista.removeAll { it.id == id }
                            }
                        } catch (e: Exception) {
                            Log.e("POSTS-API", "Error al eliminar: ${e.message}", e)
                        }
                        navController.navigate("posts")
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = { navController.navigate("posts") }) {
                Text("Cancelar")
            }
        }
    )
}