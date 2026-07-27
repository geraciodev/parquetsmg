package com.geraciodev.parquetsmg

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

@Composable
fun App(initialFile: String? = null) {
    val darkColorScheme = darkColorScheme(
        primary = Color(0xFF90CAF9),
        secondary = Color(0xFFB0BEC5),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onBackground = Color.White,
        onSurface = Color.White,
        primaryContainer = Color(0xFF1976D2),
        onPrimaryContainer = Color.White
    )

    val typography = Typography(
        bodyMedium = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Normal,
            fontSize = 14.sp
        ),
        titleLarge = TextStyle(
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        ),
        labelMedium = TextStyle(
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp
        )
    )

    val shapes = Shapes(
        small = RoundedCornerShape(0.dp),
        medium = RoundedCornerShape(0.dp),
        large = RoundedCornerShape(0.dp),
        extraLarge = RoundedCornerShape(0.dp)
    )

    MaterialTheme(
        colorScheme = darkColorScheme,
        typography = typography,
        shapes = shapes
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            ParquetViewer(initialFile)
        }
    }
}

@Composable
fun ParquetViewer(initialFile: String? = null) {
    val scope = rememberCoroutineScope()
    val parquetService = remember { ParquetService() }
    
    var filePath by remember { mutableStateOf<String?>(initialFile) }
    var allColumns by remember { mutableStateOf(emptyList<String>()) }
    var visibleColumns by remember { mutableStateOf(emptyList<String>()) }
    var rows by remember { mutableStateOf(emptyList<Map<String, Any?>>()) }
    var totalRows by remember { mutableStateOf(0L) }
    var currentPage by remember { mutableStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }
    val pageSize = 50
    var isLoading by remember { mutableStateOf(false) }
    var showColumnSelector by remember { mutableStateOf(false) }
    
    var columnFilters by remember { mutableStateOf(mapOf<String, Set<String>>()) }
    var activeFilterMenuColumn by remember { mutableStateOf<String?>(null) }
    var activeStatsColumn by remember { mutableStateOf<String?>(null) }

    fun loadPage(path: String, page: Int, query: String = searchQuery, filters: Map<String, Set<String>> = columnFilters) {
        scope.launch {
            isLoading = true
            try {
                val (data, total) = withContext(Dispatchers.IO) {
                    val r = parquetService.getRows(path, pageSize, page * pageSize, query, filters)
                    val t = parquetService.getTotalRows(path, query, filters)
                    Pair(r, t)
                }
                rows = data
                totalRows = total
                currentPage = page
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(initialFile) {
        initialFile?.let { path ->
            isLoading = true
            try {
                withContext(Dispatchers.IO) {
                    val cols = parquetService.getColumns(path)
                    allColumns = cols
                    visibleColumns = cols
                }
                loadPage(path, 0, "", emptyMap())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    fun openFilePicker() {
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("Parquet files", "parquet")
            dialogTitle = "Select a Parquet File"
        }
        val result = chooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            val selectedFile = chooser.selectedFile.absolutePath
            filePath = selectedFile
            searchQuery = "" 
            columnFilters = emptyMap()
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        val cols = parquetService.getColumns(selectedFile)
                        allColumns = cols
                        visibleColumns = cols 
                    }
                    loadPage(selectedFile, 0, "", emptyMap())
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    fun exportData() {
        val currentPath = filePath ?: return
        val chooser = JFileChooser().apply {
            fileFilter = FileNameExtensionFilter("CSV files", "csv")
            dialogTitle = "Export to CSV"
            selectedFile = File("export.csv")
        }
        val result = chooser.showSaveDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            var destination = chooser.selectedFile.absolutePath
            if (!destination.endsWith(".csv", ignoreCase = true)) {
                destination += ".csv"
            }
            scope.launch {
                isLoading = true
                try {
                    withContext(Dispatchers.IO) {
                        parquetService.exportToCsv(
                            currentPath,
                            destination,
                            searchQuery,
                            columnFilters,
                            visibleColumns
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isLoading = false
                }
            }
        }
    }

    if (showColumnSelector) {
        ColumnSelectorDialog(
            allColumns = allColumns,
            visibleColumns = visibleColumns,
            onDismiss = { showColumnSelector = false },
            onUpdateVisibleColumns = { visibleColumns = it }
        )
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Button(onClick = { openFilePicker() }, shape = MaterialTheme.shapes.small) {
                Text("Cargar Parquet")
            }
            
            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { showColumnSelector = true },
                enabled = allColumns.isNotEmpty(),
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            ) {
                Text("Columnas")
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = { exportData() },
                enabled = filePath != null && !isLoading,
                shape = MaterialTheme.shapes.small,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
            ) {
                Text("Exportar CSV")
            }

            if (columnFilters.isNotEmpty()) {
                Spacer(modifier = Modifier.width(8.dp))
                TextButton(onClick = {
                    columnFilters = emptyMap()
                    filePath?.let { loadPage(it, 0, searchQuery, emptyMap()) }
                }) {
                    Text("Limpiar Filtros (${columnFilters.size})", color = Color.Red)
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Buscar en todas las columnas...") },
                singleLine = true,
                shape = MaterialTheme.shapes.small,
                enabled = filePath != null,
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { 
                            searchQuery = ""
                            filePath?.let { loadPage(it, 0, "") }
                        }) {
                            Text("✕")
                        }
                    }
                }
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(
                onClick = { filePath?.let { loadPage(it, 0) } },
                enabled = filePath != null && !isLoading,
                shape = MaterialTheme.shapes.small
            ) {
                Text("Buscar")
            }
        }

        filePath?.let {
            Text(
                "Archivo: ${File(it).name} | Columnas visibles: ${visibleColumns.size}/${allColumns.size}",
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.secondary
            )
        }

        if (isLoading) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (rows.isNotEmpty() || (searchQuery.isNotEmpty() && filePath != null) || columnFilters.isNotEmpty()) {
            if (rows.isEmpty() && !isLoading) {
                Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No se encontraron resultados")
                }
            } else {
                val horizontalScrollState = rememberScrollState()
                val verticalLazyListState = rememberLazyListState()

                // Estilo personalizado para que los scrollbars sean más visibles
                val scrollbarStyle = defaultScrollbarStyle().copy(
                    unhoverColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
                    hoverColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    thickness = 10.dp,
                    shape = MaterialTheme.shapes.small
                )

                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    // Contenedor principal con scroll horizontal
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 12.dp) // Espacio para la barra horizontal
                            .horizontalScroll(horizontalScrollState)
                    ) {
                        Column {
                            // Encabezado
                            Row(
                                modifier = Modifier
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                visibleColumns.forEach { column ->
                                    Row(
                                        modifier = Modifier.width(180.dp).padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = column,
                                            modifier = Modifier.weight(1f),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            maxLines = 1
                                        )

                                        IconButton(
                                            onClick = { activeStatsColumn = column },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text("Σ", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        }

                                        IconButton(
                                            onClick = { activeFilterMenuColumn = column },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Text(
                                                if (columnFilters.containsKey(column)) "▼!" else "▼",
                                                fontSize = 10.sp,
                                                color = if (columnFilters.containsKey(column)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }

                                        if (activeFilterMenuColumn == column) {
                                            ColumnFilterDialog(
                                                columnName = column,
                                                filePath = filePath!!,
                                                parquetService = parquetService,
                                                currentFilters = columnFilters[column] ?: emptySet(),
                                                onDismiss = { activeFilterMenuColumn = null },
                                                onApply = { newFilters ->
                                                    val updatedFilters = columnFilters.toMutableMap()
                                                    if (newFilters.isEmpty()) {
                                                        updatedFilters.remove(column)
                                                    } else {
                                                        updatedFilters[column] = newFilters
                                                    }
                                                    columnFilters = updatedFilters
                                                    activeFilterMenuColumn = null
                                                    loadPage(filePath!!, 0, searchQuery, updatedFilters)
                                                }
                                            )
                                        }

                                        if (activeStatsColumn == column) {
                                            ColumnStatsDialog(
                                                columnName = column,
                                                filePath = filePath!!,
                                                parquetService = parquetService,
                                                searchQuery = searchQuery,
                                                columnFilters = columnFilters,
                                                onDismiss = { activeStatsColumn = null }
                                            )
                                        }
                                    }
                                }
                            }

                            // Cuerpo de la tabla (Filas)
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .border(0.5.dp, Color.Gray),
                                state = verticalLazyListState
                            ) {
                                items(rows) { row ->
                                    Row(modifier = Modifier.border(0.2.dp, Color.Gray)) {
                                        visibleColumns.forEach { column ->
                                            Text(
                                                text = row[column]?.toString() ?: "null",
                                                modifier = Modifier.width(180.dp).padding(8.dp),
                                                fontSize = 11.sp,
                                                maxLines = 1
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Barras de scroll con el nuevo estilo
                    VerticalScrollbar(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .fillMaxHeight(),
                        adapter = rememberScrollbarAdapter(verticalLazyListState),
                        style = scrollbarStyle
                    )

                    HorizontalScrollbar(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(),
                        adapter = rememberScrollbarAdapter(horizontalScrollState),
                        style = scrollbarStyle
                    )
                }
            }

            if (totalRows > 0) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val totalPages = (totalRows + pageSize - 1) / pageSize
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Button(
                            onClick = { loadPage(filePath!!, currentPage - 1) },
                            enabled = currentPage > 0 && !isLoading,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Anterior")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = { loadPage(filePath!!, currentPage + 1) },
                            enabled = (currentPage + 1) * pageSize < totalRows && !isLoading,
                            shape = MaterialTheme.shapes.small
                        ) {
                            Text("Siguiente")
                        }
                    }
                    
                    Text("Página ${currentPage + 1} de $totalPages (Total: $totalRows)")
                }
            }
        } else {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text("No hay datos cargados. Selecciona un archivo Parquet.")
            }
        }
    }
}

@Composable
fun ColumnFilterDialog(
    columnName: String,
    filePath: String,
    parquetService: ParquetService,
    currentFilters: Set<String>,
    onDismiss: () -> Unit,
    onApply: (Set<String>) -> Unit
) {
    var uniqueValues by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedValues by remember { mutableStateOf(currentFilters) }
    var isLoadingValues by remember { mutableStateOf(true) }
    var filterText by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    LaunchedEffect(columnName) {
        scope.launch {
            isLoadingValues = true
            uniqueValues = withContext(Dispatchers.IO) {
                parquetService.getUniqueValues(filePath, columnName)
            }
            isLoadingValues = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            tonalElevation = 6.dp,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Filtrar: $columnName", 
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = filterText,
                    onValueChange = { filterText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar valor...") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                    textStyle = TextStyle(fontSize = 14.sp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                if (isLoadingValues) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    val filteredList = uniqueValues.filter { it.contains(filterText, ignoreCase = true) }
                    
                    Column(modifier = Modifier.weight(1f).border(1.dp, Color.Gray.copy(alpha = 0.5f))) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .clickable {
                                    selectedValues = if (selectedValues.size == uniqueValues.size) {
                                        emptySet()
                                    } else {
                                        uniqueValues.toSet()
                                    }
                                }.padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            TriStateCheckbox(
                                state = when {
                                    selectedValues.isEmpty() -> androidx.compose.ui.state.ToggleableState.Off
                                    selectedValues.size == uniqueValues.size -> androidx.compose.ui.state.ToggleableState.On
                                    else -> androidx.compose.ui.state.ToggleableState.Indeterminate
                                },
                                onClick = null
                            )
                            Text("(Seleccionar todo)", modifier = Modifier.padding(start = 12.dp), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider()

                        LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                            items(filteredList) { value ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth().clickable {
                                        selectedValues = if (selectedValues.contains(value)) {
                                            selectedValues - value
                                        } else {
                                            selectedValues + value
                                        }
                                    }.padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Checkbox(checked = selectedValues.contains(value), onCheckedChange = null)
                                    Text(
                                        value, 
                                        modifier = Modifier.padding(start = 12.dp), 
                                        fontSize = 13.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = { selectedValues = emptySet() },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Limpiar")
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = { onApply(selectedValues) },
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text("Aplicar")
                    }
                }
            }
        }
    }
}

@Composable
fun ColumnStatsDialog(
    columnName: String,
    filePath: String,
    parquetService: ParquetService,
    searchQuery: String,
    columnFilters: Map<String, Set<String>>,
    onDismiss: () -> Unit
) {
    var stats by remember { mutableStateOf<Map<String, Any?>>(emptyMap()) }
    var isLoading by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(columnName) {
        scope.launch {
            isLoading = true
            stats = withContext(Dispatchers.IO) {
                parquetService.getColumnStats(filePath, columnName, searchQuery, columnFilters)
            }
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.padding(16.dp).width(350.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Estadísticas: $columnName",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        StatRow("Recuento (no nulos)", stats["row_count"]?.toString() ?: "0")
                        StatRow("Valores únicos", stats["unique_count"]?.toString() ?: "0")
                        
                        val avg = stats["average"]
                        if (avg != null) {
                            StatRow("Promedio", String.format("%.4f", (avg as Number).toDouble()))
                        } else {
                            StatRow("Promedio", "N/A")
                        }
                        
                        val sum = stats["total_sum"]
                        if (sum != null) {
                            StatRow("Suma total", String.format("%.4f", (sum as Number).toDouble()))
                        } else {
                            StatRow("Suma total", "N/A")
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                    shape = MaterialTheme.shapes.small
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun ColumnSelectorDialog(
    allColumns: List<String>,
    visibleColumns: List<String>,
    onDismiss: () -> Unit,
    onUpdateVisibleColumns: (List<String>) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxWidth(0.6f).fillMaxHeight(0.7f)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Seleccionar Columnas", style = MaterialTheme.typography.titleLarge)
                Spacer(modifier = Modifier.height(16.dp))
                
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(allColumns) { column ->
                        val isChecked = visibleColumns.contains(column)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (isChecked) {
                                    onUpdateVisibleColumns(visibleColumns - column)
                                } else {
                                    onUpdateVisibleColumns(visibleColumns + column)
                                }
                            }.padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null
                            )
                            Text(column, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = { onUpdateVisibleColumns(allColumns) }) {
                        Text("Todas")
                    }
                    TextButton(onClick = { onUpdateVisibleColumns(emptyList()) }) {
                        Text("Ninguna")
                    }
                    Button(onClick = onDismiss, shape = MaterialTheme.shapes.small) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }
}
