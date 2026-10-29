package com.gemid.photostudio.data.order

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Room Entity representing a completed customer print job or studio export order.
 * Persisted locally in SQLite for repeat printing and historical record-keeping.
 */
@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey
    val orderId: String,
    val customerName: String,
    val photoSpecTitle: String,
    val paperSize: String,
    val timestamp: Long = System.currentTimeMillis(),
    val photoPath: String,
    val photoSpecId: String = "in_passport",
    val paperSizeId: String = "paper_4x6",
    val photoCount: Int = 8,
    val isLandscape: Boolean = false,
    val exportFormat: String = "JPEG"
) {
    val formattedDate: String
        get() {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            return sdf.format(Date(timestamp))
        }

    val relativeTimeSpan: String
        get() {
            val diff = System.currentTimeMillis() - timestamp
            val seconds = diff / 1000
            val minutes = seconds / 60
            val hours = minutes / 60
            val days = hours / 24

            return when {
                minutes < 1 -> "Just now"
                minutes < 60 -> "${minutes}m ago"
                hours < 24 -> "${hours}h ago"
                days < 7 -> "${days}d ago"
                else -> SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(timestamp))
            }
        }
}
