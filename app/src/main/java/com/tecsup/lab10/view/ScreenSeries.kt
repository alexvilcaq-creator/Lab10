package com.tecsup.lab10.view

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.tecsup.lab10.data.SerieApiService
import com.tecsup.lab10.data.SerieModel
import kotlinx.coroutines.launch

// ---------- LISTADO ----------
@Composable
fun ContenidoSeriesListado(navController: NavHostController, servicio: SerieApiService) {
    val listaSeries: SnapshotStateList<SerieModel> = remember { mutableStateListOf() }

    LaunchedEffect(Unit) {
        try {
            val listado = servicio.selectSeries()
            listaSeries.clear()
            listado.forEach { listaSeries.add(it) }
        } catch (e: Exception) {
            Log.e("SERIES-API", "Error al cargar series: ${e.message}", e)
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items(listaSeries) { item ->
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
                        text = "${item.id}",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.weight(0.12f)
                    )
                    Column(modifier = Modifier.weight(0.68f)) {
                        Text(
                            text = item.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${item.category} · ★ ${item.rating}",
                            fontSize = 13.sp
                        )
                    }
                    IconButton(
                        onClick = {
                            navController.navigate("serieVer/${item.id}")
                            Log.e("SERIE-VER", "ID = ${item.id}")
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.Edit, contentDescription = "Editar")
                    }
                    IconButton(
                        onClick = {
                            navController.navigate("serieDel/${item.id}")
                            Log.e("SERIE-DEL", "ID = ${item.id}")
                        }
                    ) {
                        Icon(imageVector = Icons.Outlined.Delete, contentDescription = "Eliminar")
                    }
                }
            }
        }
    }
}

// ---------- CREAR / EDITAR ----------
@Composable
fun ContenidoSerieEditar(navController: NavHostController, servicio: SerieApiService, pid: Int = 0) {
    val id = pid
    var name by remember { mutableStateOf("") }
    var releaseDate by remember { mutableStateOf("") }
    var rating by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var mensaje by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(id) {
        if (id != 0) {
            try {
                val serie = servicio.selectSerie(id.toString()).body()
                if (serie != null) {
                    name = serie.name
                    releaseDate = serie.release_date
                    rating = serie.rating.toString()
                    category = serie.category
                }
            } catch (e: Exception) {
                Log.e("SERIES-API", "Error al cargar la serie: ${e.message}", e)
                mensaje = "No se pudo cargar la serie"
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
            text = if (id == 0) "Nueva serie" else "Editar serie #$id",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = releaseDate,
            onValueChange = { releaseDate = it },
            label = { Text("Fecha de estreno (AAAA-MM-DD)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = rating,
            onValueChange = { rating = it },
            label = { Text("Rating (número)") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = category,
            onValueChange = { category = it },
            label = { Text("Categoría") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (mensaje.isNotEmpty()) {
            Text(text = mensaje, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(
                onClick = {
                    val ratingInt = rating.toIntOrNull()
                    if (name.isBlank() || releaseDate.isBlank() || category.isBlank() || ratingInt == null) {
                        mensaje = "Completa todos los campos (el rating debe ser un número)"
                    } else {
                        mensaje = ""
                        scope.launch {
                            try {
                                val serie = SerieModel(id, name, releaseDate, ratingInt, category)
                                val respuesta = if (id == 0)
                                    servicio.insertSerie(serie)
                                else
                                    servicio.updateSerie(id.toString(), serie)

                                if (respuesta.isSuccessful) {
                                    navController.navigate("series")
                                } else {
                                    mensaje = "Error del servidor: ${respuesta.code()}"
                                }
                            } catch (e: Exception) {
                                Log.e("SERIES-API", "Error al grabar: ${e.message}", e)
                                mensaje = "No se pudo conectar con el servidor"
                            }
                        }
                    }
                }
            ) {
                Text("Grabar", fontSize = 16.sp)
            }
            OutlinedButton(onClick = { navController.navigate("series") }) {
                Text("Cancelar", fontSize = 16.sp)
            }
        }
    }
}

// ---------- ELIMINAR ----------
@Composable
fun ContenidoSerieEliminar(navController: NavHostController, servicio: SerieApiService, id: Int) {
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { navController.navigate("series") },
        title = { Text(text = "Confirmar eliminación") },
        text = { Text("¿Está seguro de eliminar la serie #$id?") },
        confirmButton = {
            Button(
                onClick = {
                    scope.launch {
                        try {
                            servicio.deleteSerie(id.toString())
                        } catch (e: Exception) {
                            Log.e("SERIES-API", "Error al eliminar: ${e.message}", e)
                        }
                        navController.navigate("series")
                    }
                }
            ) {
                Text("Aceptar")
            }
        },
        dismissButton = {
            TextButton(onClick = { navController.navigate("series") }) {
                Text("Cancelar")
            }
        }
    )
}