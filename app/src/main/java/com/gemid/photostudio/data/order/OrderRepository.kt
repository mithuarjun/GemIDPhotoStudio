package com.gemid.photostudio.data.order

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.gemid.photostudio.data.model.PhotoSpec
import com.gemid.photostudio.sheet.PaperSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Repository orchestrating local Room order records and companion disk image files.
 */
class OrderRepository(
    private val orderDao: OrderDao,
    private val context: Context
) {
    val recentOrders: Flow<List<OrderEntity>> = orderDao.getRecentOrders(12)
    val allOrders: Flow<List<OrderEntity>> = orderDao.getAllOrders()
    val orderCount: Flow<Int> = orderDao.getOrderCount()

    private val ordersDir: File by lazy {
        val dir = File(context.filesDir, "orders")
        if (!dir.exists()) dir.mkdirs()
        dir
    }

    /**
     * Records a completed print job or exported photo order, archiving the sheet bitmap locally.
     */
    suspend fun recordOrder(
        bitmap: Bitmap,
        customerName: String,
        photoSpec: PhotoSpec,
        paperSize: PaperSize,
        photoCount: Int = 8,
        isLandscape: Boolean = false,
        exportFormat: String = "JPEG"
    ): OrderEntity = withContext(Dispatchers.IO) {
        val time = System.currentTimeMillis()
        val dateStamp = SimpleDateFormat("yyMMdd-HHmmss", Locale.US).format(Date(time))
        val orderId = "ORD-$dateStamp"

        val photoFile = File(ordersDir, "${orderId}.jpg")
        try {
            FileOutputStream(photoFile).use { out ->
                // Compress with high quality 90%
                bitmap.compress(Bitmap.CompressFormat.JPEG, 90, out)
            }
        } catch (_: Exception) {}

        val entity = OrderEntity(
            orderId = orderId,
            customerName = customerName.ifBlank { "Walk-in Customer" },
            photoSpecTitle = photoSpec.title,
            paperSize = paperSize.title,
            timestamp = time,
            photoPath = photoFile.absolutePath,
            photoSpecId = photoSpec.id,
            paperSizeId = paperSize.id,
            photoCount = photoCount,
            isLandscape = isLandscape,
            exportFormat = exportFormat
        )

        orderDao.insertOrder(entity)
        return@withContext entity
    }

    /**
     * Loads the saved photo bitmap for an order from local storage.
     */
    suspend fun loadOrderBitmap(order: OrderEntity): Bitmap? = withContext(Dispatchers.IO) {
        val file = File(order.photoPath)
        if (!file.exists()) return@withContext null
        return@withContext try {
            BitmapFactory.decodeFile(file.absolutePath)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Deletes an order and its associated image archive from local storage.
     */
    suspend fun deleteOrder(order: OrderEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(order.photoPath)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
        orderDao.deleteOrder(order)
    }

    /**
     * Clears all historical orders and removes cached order files.
     */
    suspend fun clearAllOrders() = withContext(Dispatchers.IO) {
        try {
            ordersDir.listFiles()?.forEach { it.delete() }
        } catch (_: Exception) {}
        orderDao.clearAllOrders()
    }
}
