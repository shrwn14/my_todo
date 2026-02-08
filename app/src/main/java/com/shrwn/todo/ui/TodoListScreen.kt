package com.shrwn.todo.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.shrwn.todo.data.TodoTask
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoListScreen(
    viewModel: TodoViewModel,
    onBack: () -> Unit
) {
    val tasks by viewModel.tasks.collectAsState()
    val listState = rememberLazyListState()
    var showAddDialog by remember { mutableStateOf(false) }
    var taskToEdit by remember { mutableStateOf<TodoTask?>(null) }
    var taskToDelete by remember { mutableStateOf<TodoTask?>(null) }
    var taskToComplete by remember { mutableStateOf<TodoTask?>(null) }

    // Infinite Scroll Logic
    LaunchedEffect(listState) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .collectLatest { index ->
                if (index >= tasks.size - 5 && tasks.isNotEmpty()) {
                    viewModel.loadMoreTasks()
                }
            }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Task")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TodoItemRow(
                    task = task,
                    onEdit = { taskToEdit = it },
                    onDelete = { taskToDelete = it },
                    onToggle = { taskToComplete = it }
                )
            }
        }
    }

    // Dialogs
    if (showAddDialog) {
        TaskDialog(
            title = "Add Task",
            onDismiss = { showAddDialog = false },
            onConfirm = { text ->
                viewModel.addTask(text)
                showAddDialog = false
            }
        )
    }

    taskToEdit?.let { task ->
        TaskDialog(
            title = "Edit Task",
            initialText = task.text,
            onDismiss = { taskToEdit = null },
            onConfirm = { text ->
                viewModel.updateTask(task.copy(text = text))
                taskToEdit = null
            }
        )
    }

    taskToDelete?.let { task ->
        ConfirmationDialog(
            title = "Delete Task",
            message = "Are you sure you want to delete this task?",
            onDismiss = { taskToDelete = null },
            onConfirm = {
                viewModel.deleteTask(task)
                taskToDelete = null
            }
        )
    }

    taskToComplete?.let { task ->
        val action = if (task.isCompleted) "unmark" else "mark"
        ConfirmationDialog(
            title = "Task Status",
            message = "Do you want to $action this task as done?",
            onDismiss = { taskToComplete = null },
            onConfirm = {
                viewModel.updateTask(task.copy(isCompleted = !task.isCompleted))
                taskToComplete = null
            }
        )
    }
}

@Composable
fun TodoItemRow(
    task: TodoTask,
    onEdit: (TodoTask) -> Unit,
    onDelete: (TodoTask) -> Unit,
    onToggle: (TodoTask) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = task.text,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.weight(1f),
                color = if (task.isCompleted) Color.Gray else Color.Unspecified
            )
            
            Row {
                IconButton(onClick = { onToggle(task) }) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle Done",
                        tint = if (task.isCompleted) Color.Green else Color.Gray
                    )
                }
                IconButton(onClick = { onEdit(task) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit")
                }
                IconButton(onClick = { onDelete(task) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TaskDialog(
    title: String,
    initialText: String = "",
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var text by remember { mutableStateOf(initialText) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Task description") },
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onConfirm(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Confirm")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ConfirmationDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("No")
            }
        }
    )
}
