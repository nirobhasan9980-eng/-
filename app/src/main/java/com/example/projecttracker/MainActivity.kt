package com.example.projecttracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val dao = TrackerDatabase.get(this).dao()
        setContent {
            val vm: TrackerViewModel = viewModel(factory = TrackerViewModel.Factory(dao))
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF2563EB),
                error = Color(0xFFDC2626),
                secondary = Color(0xFF16A34A)
            )) { TrackerScreen(vm) }
        }
    }
}

class TrackerViewModel(private val dao: TrackerDao) : ViewModel() {
    val projects = dao.projects()
    val tasks = dao.tasks()
    fun addProject(name: String) = viewModelScope.launch {
        if (name.isNotBlank()) dao.addProject(Project(name = name.trim()))
    }
    fun deleteProject(id: Long) = viewModelScope.launch { dao.deleteProject(id) }
    fun addTask(projectId: Long, topic: String, date: String) = viewModelScope.launch {
        if (topic.isNotBlank()) dao.addTask(DailyTask(projectId = projectId, topic = topic.trim(), dueDate = date))
    }
    fun complete(task: DailyTask, done: Boolean) = viewModelScope.launch { dao.setCompleted(task.id, done) }
    fun deleteTask(id: Long) = viewModelScope.launch { dao.deleteTask(id) }

    class Factory(private val dao: TrackerDao) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackerViewModel(dao) as T
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrackerScreen(vm: TrackerViewModel) {
    val projects by vm.projects.collectAsStateWithLifecycle(initialValue = emptyList())
    val tasks by vm.tasks.collectAsStateWithLifecycle(initialValue = emptyList())
    var projectName by remember { mutableStateOf("") }
    var topic by remember { mutableStateOf("") }
    var selectedProject by remember { mutableStateOf<Long?>(null) }
    var taskDate by remember { mutableStateOf(LocalDate.now().toString()) }
    var showTaskForm by remember { mutableStateOf(false) }
    val today = LocalDate.now().toString()
    val doneCount = tasks.count { it.completed }
    val missedCount = tasks.count { !it.completed && it.dueDate < today }
    val pendingCount = tasks.count { !it.completed && it.dueDate >= today }

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Project Tracker", fontWeight = FontWeight.Bold) }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showTaskForm = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add daily task")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 90.dp)
        ) {
            item {
                Text("আজকের অগ্রগতি", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    SummaryCard("Completed", doneCount.toString(), Color(0xFF16A34A), Modifier.weight(1f))
                    SummaryCard("Pending", pendingCount.toString(), Color(0xFF64748B), Modifier.weight(1f))
                    SummaryCard("Missed", missedCount.toString(), Color(0xFFDC2626), Modifier.weight(1f))
                }
            }
            item {
                Text("প্রজেক্টসমূহ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = projectName, onValueChange = { projectName = it },
                        label = { Text("নতুন প্রজেক্টের নাম") }, modifier = Modifier.weight(1f),
                        singleLine = true, keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                    )
                    IconButton(onClick = { vm.addProject(projectName); projectName = "" }) {
                        Icon(Icons.Default.Add, contentDescription = "Add project", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            if (projects.isEmpty()) {
                item { EmptyHint("এখনো কোনো প্রজেক্ট নেই। উপরে নাম লিখে + চাপুন।") }
            }
            items(projects, key = { it.id }) { project ->
                val projectTasks = tasks.filter { it.projectId == project.id }
                val completed = projectTasks.count { it.completed }
                val progress = if (projectTasks.isEmpty()) 0f else completed.toFloat() / projectTasks.size
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(project.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text("${projectTasks.size} টি টপিক · ${ (progress * 100).toInt() }% সম্পন্ন", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { vm.deleteProject(project.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete project", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                        projectTasks.sortedByDescending { it.dueDate }.forEach { task ->
                            TaskRow(task, onDone = { vm.complete(task, it) }, onDelete = { vm.deleteTask(task.id) })
                        }
                        TextButton(onClick = { selectedProject = project.id; showTaskForm = true }) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Text("টপিক / দৈনিক কাজ যোগ করুন")
                        }
                    }
                }
            }
            item {
                Text("সব দিনের কাজ", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("তারিখ অনুযায়ী কাজের অবস্থা স্বয়ংক্রিয়ভাবে দেখানো হয়।", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            items(tasks.sortedByDescending { it.dueDate }, key = { "all-${it.id}" }) { task ->
                val projectNameForTask = projects.firstOrNull { it.id == task.projectId }?.name ?: "Deleted project"
                Column {
                    Text(projectNameForTask, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TaskRow(task, onDone = { vm.complete(task, it) }, onDelete = { vm.deleteTask(task.id) })
                }
            }
        }
    }

    if (showTaskForm) {
        AlertDialog(
            onDismissRequest = { showTaskForm = false; selectedProject = null },
            title = { Text("দৈনিক কাজ যোগ করুন") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (selectedProject == null) {
                        var expanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                            OutlinedTextField(
                                value = projects.firstOrNull { it.id == selectedProject }?.name ?: "প্রজেক্ট নির্বাচন করুন",
                                onValueChange = {}, readOnly = true, modifier = Modifier.menuAnchor().fillMaxWidth(),
                                label = { Text("প্রজেক্ট") }
                            )
                            ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                projects.forEach { p ->
                                    DropdownMenuItem(text = { Text(p.name) }, onClick = { selectedProject = p.id; expanded = false })
                                }
                            }
                        }
                    } else {
                        Text("প্রজেক্ট: ${projects.firstOrNull { it.id == selectedProject }?.name ?: ""}")
                    }
                    OutlinedTextField(topic, { topic = it }, label = { Text("কাজের টপিক") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(taskDate, { taskDate = it }, label = { Text("তারিখ (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pid = selectedProject
                    val validDate = runCatching { LocalDate.parse(taskDate); true }.getOrDefault(false)
                    if (pid != null && validDate && topic.isNotBlank()) {
                        vm.addTask(pid, topic, taskDate)
                        topic = ""; taskDate = LocalDate.now().toString()
                        showTaskForm = false; selectedProject = null
                    }
                }) { Text("যোগ করুন") }
            },
            dismissButton = { TextButton(onClick = { showTaskForm = false; selectedProject = null }) { Text("বাতিল") } }
        )
    }
}

@Composable
fun SummaryCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(14.dp)) {
        Column(Modifier.padding(12.dp)) {
            Text(value, style = MaterialTheme.typography.headlineSmall, color = color, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun TaskRow(task: DailyTask, onDone: (Boolean) -> Unit, onDelete: () -> Unit) {
    val today = LocalDate.now().toString()
    val missed = !task.completed && task.dueDate < today
    val bg = when {
        task.completed -> Color(0xFFE8F5E9)
        missed -> Color(0xFFFFEBEE)
        else -> MaterialTheme.colorScheme.surface
    }
    val status = when {
        task.completed -> "Completed"
        missed -> "Missed"
        else -> "Pending"
    }
    val statusColor = when {
        task.completed -> Color(0xFF15803D)
        missed -> Color(0xFFB91C1C)
        else -> Color(0xFF64748B)
    }
    Card(colors = CardDefaults.cardColors(containerColor = bg), shape = RoundedCornerShape(12.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = task.completed, onCheckedChange = onDone)
            Column(Modifier.weight(1f)) {
                Text(task.topic, fontWeight = FontWeight.Medium)
                Text("${task.dueDate} · $status", style = MaterialTheme.typography.labelSmall, color = statusColor)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
fun EmptyHint(message: String) {
    Text(message, modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp)).padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
}
