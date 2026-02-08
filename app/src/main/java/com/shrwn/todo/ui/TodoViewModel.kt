package com.shrwn.todo.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.shrwn.todo.data.AppDatabase
import com.shrwn.todo.data.TodoTask
import com.shrwn.todo.data.User
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class TodoViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application, viewModelScope)
    private val todoDao = database.todoDao()
    private val userDao = database.userDao()

    // Auth State
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    // Dashboard Stats
    val todayTasksCount = todoDao.getTasksForDayCount(getStartOfDay()).stateIn(viewModelScope, SharingStarted.Lazily, 0)
    val completedTasksCount = todoDao.getCompletedTasksCount().stateIn(viewModelScope, SharingStarted.Lazily, 0)

    // Todo List State
    private val _tasks = MutableStateFlow<List<TodoTask>>(emptyList())
    val tasks: StateFlow<List<TodoTask>> = _tasks.asStateFlow()

    private var currentOffset = 0
    private val pageSize = 20
    private var isLastPage = false

    init {
        loadMoreTasks()
    }

    fun login(username: String, password: String, onSelection: () -> Unit) {
        viewModelScope.launch {
            val user = userDao.getUserByUsername(username)
            if (user != null && user.password == password) {
                _currentUser.value = user
                _loginError.value = null
                onSelection()
            } else {
                _loginError.value = "Invalid username or password"
            }
        }
    }

    fun logout() {
        _currentUser.value = null
    }

    fun loadMoreTasks() {
        if (isLastPage) return
        viewModelScope.launch {
            val newTasks = todoDao.getTasksPaged(pageSize, currentOffset)
            if (newTasks.size < pageSize) {
                isLastPage = true
            }
            _tasks.value = _tasks.value + newTasks
            currentOffset += pageSize
        }
    }

    fun addTask(text: String) {
        viewModelScope.launch {
            val newTask = TodoTask(text = text)
            todoDao.insertTask(newTask)
            // Refresh list (simplified for this app)
            refreshTasks()
        }
    }

    fun updateTask(task: TodoTask) {
        viewModelScope.launch {
            todoDao.updateTask(task)
            updateTaskInList(task)
        }
    }

    fun deleteTask(task: TodoTask) {
        viewModelScope.launch {
            todoDao.deleteTask(task)
            _tasks.value = _tasks.value.filter { it.id != task.id }
        }
    }

    private fun refreshTasks() {
        currentOffset = 0
        isLastPage = false
        _tasks.value = emptyList()
        loadMoreTasks()
    }

    private fun updateTaskInList(updatedTask: TodoTask) {
        _tasks.value = _tasks.value.map { if (it.id == updatedTask.id) updatedTask else it }
    }

    private fun getStartOfDay(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
