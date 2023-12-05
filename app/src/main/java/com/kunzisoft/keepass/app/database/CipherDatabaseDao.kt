package com.kunzisoft.keepass.app.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CipherDatabaseDao {

    @Query("SELECT * FROM cipher_database WHERE database_uri = :databaseUriString")
    fun getByDatabaseUri(databaseUriString: String): CipherDatabaseEntity?

    @Insert
    fun add(vararg fileDatabaseHistory: CipherDatabaseEntity)

    @Update
    fun update(vararg fileDatabaseHistory: CipherDatabaseEntity)

    @Query("DELETE FROM cipher_database WHERE database_uri = :databaseUriString")
    fun deleteByDatabaseUri(databaseUriString: String)

    @Query("DELETE FROM cipher_database")
    fun deleteAll()
}