package com.example.practice.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PendingAttendanceDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: PendingAttendanceEntity)

    @Query("SELECT * FROM pending_attendance ORDER BY createdAt ASC")
    suspend fun getAll(): List<PendingAttendanceEntity>

    @Query("DELETE FROM pending_attendance WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("SELECT COUNT(*) FROM pending_attendance")
    suspend fun count(): Int
}
