package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.domain.model.NetWorthPoint

@Entity(
    tableName = "net_worth_snapshots",
    indices = [
        Index("date")
    ]
)
data class NetWorthSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: Long,
    val totalAssets: Double,
    val totalLiabilities: Double,
    val netWorth: Double
) {
    fun toDomain(label: String): NetWorthPoint = NetWorthPoint(
        timestamp = date,
        label = label,
        assets = totalAssets,
        liabilities = totalLiabilities,
        netWorth = netWorth
    )
}
