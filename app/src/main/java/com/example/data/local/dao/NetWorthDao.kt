package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NetWorthSnapshotEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NetWorthDao {

    @Query("SELECT * FROM net_worth_snapshots ORDER BY date ASC")
    fun getAllSnapshots(): Flow<List<NetWorthSnapshotEntity>>

    @Query("SELECT * FROM net_worth_snapshots WHERE date >= :sinceMillis ORDER BY date ASC")
    fun getSnapshotsSince(sinceMillis: Long): Flow<List<NetWorthSnapshotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshot(snapshot: NetWorthSnapshotEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSnapshots(snapshots: List<NetWorthSnapshotEntity>)

    @Query("DELETE FROM net_worth_snapshots WHERE date < :beforeMillis")
    suspend fun deleteOldSnapshots(beforeMillis: Long)
}
