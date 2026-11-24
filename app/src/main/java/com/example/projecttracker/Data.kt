package com.example.projecttracker

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Entity(tableName = "projects")
data class Project(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: String = LocalDate.now().toString()
)

@Entity(
    tableName = "tasks",
    foreignKeys = [ForeignKey(entity = Project::class, parentColumns = ["id"], childColumns = ["projectId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("projectId")]
)
data class DailyTask(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val projectId: Long,
    val topic: String,
    val dueDate: String,
    val completed: Boolean = false
)

@Dao
interface TrackerDao {
    @Query("SELECT * FROM projects ORDER BY createdAt DESC, id DESC")
    fun projects(): Flow<List<Project>>

    @Query("SELECT * FROM tasks ORDER BY dueDate DESC, id DESC")
    fun tasks(): Flow<List<DailyTask>>

    @Insert suspend fun addProject(project: Project)
    @Query("DELETE FROM projects WHERE id = :id") suspend fun deleteProject(id: Long)
    @Insert suspend fun addTask(task: DailyTask)
    @Query("UPDATE tasks SET completed = :done WHERE id = :id")
    suspend fun setCompleted(id: Long, done: Boolean)
    @Query("DELETE FROM tasks WHERE id = :id") suspend fun deleteTask(id: Long)
}

@Database(entities = [Project::class, DailyTask::class], version = 1, exportSchema = false)
abstract class TrackerDatabase : RoomDatabase() {
    abstract fun dao(): TrackerDao
    companion object {
        @Volatile private var INSTANCE: TrackerDatabase? = null
        fun get(context: Context): TrackerDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, TrackerDatabase::class.java, "project_tracker.db"
                ).build().also { INSTANCE = it }
            }
    }
}
