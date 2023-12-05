package com.kunzisoft.keepass.app.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface FileDatabaseHistoryDao {
    @Query("SELECT * FROM file_database_history ORDER BY updated DESC")
    fun getAll(): List<FileDatabaseHistoryEntity>

    @Query("SELECT * FROM file_database_history WHERE database_uri = :databaseUriString")
    fun getByDatabaseUri(databaseUriString: String): FileDatabaseHistoryEntity?

    @Insert
    fun add(vararg fileDatabaseHistory: FileDatabaseHistoryEntity)

    @Update
    fun update(vararg fileDatabaseHistory: FileDatabaseHistoryEntity)

    @Delete
    fun delete(fileDatabaseHistory: FileDatabaseHistoryEntity): Int

    @Query("UPDATE file_database_history SET keyfile_uri=null WHERE database_uri = :databaseUriString")
    fun deleteKeyFileByDatabaseUri(databaseUriString: String)

    @Query("UPDATE file_database_history SET keyfile_uri=null")
    fun deleteAllKeyFiles()

    @Query("DELETE FROM file_database_history")
    fun deleteAll()
}