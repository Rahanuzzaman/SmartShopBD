package com.smartshopbd.app.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "products")
data class ProductEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val purchasePrice: Double = 0.0, val price: Double, val stock: Int, val barcode: String = "")
@Entity(tableName = "customers")
data class CustomerEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val phone: String, val due: Double = 0.0)
@Entity(tableName = "sales")
data class SaleEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val invoiceNo: String, val customerId: Long? = null, val customerName: String = "Walk-in Customer", val subtotal: Double, val discount: Double, val total: Double, val paid: Double, val due: Double, val paymentMethod: String, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "sale_items")
data class SaleItemEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val saleId: Long, val productId: Long, val productName: String, val quantity: Int, val unitPrice: Double, val unitCost: Double, val lineTotal: Double)
@Entity(tableName = "due_payments")
data class DuePaymentEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val customerId: Long, val amount: Double, val note: String = "", val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "stock_history")
data class StockHistoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val productId: Long, val productName: String, val changeQty: Int, val reason: String, val createdAt: Long = System.currentTimeMillis())
@Entity(tableName = "expenses")
data class ExpenseEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val title: String, val amount: Double, val createdAt: Long = System.currentTimeMillis())

data class SalesSummary(val sales: Double, val paid: Double, val due: Double, val profit: Double)

@Dao
interface ShopDao {
 @Query("SELECT * FROM products ORDER BY name") fun products(): Flow<List<ProductEntity>>
 @Insert suspend fun insertProduct(p: ProductEntity)
 @Update suspend fun updateProduct(p: ProductEntity)
 @Query("SELECT * FROM customers ORDER BY name") fun customers(): Flow<List<CustomerEntity>>
 @Insert suspend fun insertCustomer(c: CustomerEntity)
 @Update suspend fun updateCustomer(c: CustomerEntity)
 @Insert suspend fun insertSale(s: SaleEntity): Long
 @Query("SELECT * FROM sales ORDER BY createdAt DESC") fun sales(): Flow<List<SaleEntity>>
 @Insert suspend fun insertSaleItems(items: List<SaleItemEntity>)
 @Insert suspend fun insertDuePayment(p: DuePaymentEntity)
 @Insert suspend fun insertStockHistory(h: StockHistoryEntity)
 @Insert suspend fun insertExpense(e: ExpenseEntity)
 @Query("SELECT * FROM expenses ORDER BY createdAt DESC") fun expenses(): Flow<List<ExpenseEntity>>
 @Query("SELECT COALESCE(SUM(total),0) FROM sales") fun totalSales(): Flow<Double>
 @Query("SELECT COALESCE(SUM(due),0) FROM sales") fun totalSalesDue(): Flow<Double>
 @Query("SELECT COALESCE(SUM(amount),0) FROM expenses") fun totalExpenses(): Flow<Double>
 @Query("SELECT COALESCE(SUM(lineTotal - unitCost * quantity),0) FROM sale_items") fun totalProfit(): Flow<Double>
 @Transaction suspend fun saveSale(sale: SaleEntity, items: List<SaleItemEntity>, updatedProducts: List<ProductEntity>, stockLogs: List<StockHistoryEntity>, updatedCustomer: CustomerEntity?) {
   val saleId = insertSale(sale)
   insertSaleItems(items.map { it.copy(saleId = saleId) })
   updatedProducts.forEach { updateProduct(it) }
   stockLogs.forEach { insertStockHistory(it) }
   if (updatedCustomer != null) updateCustomer(updatedCustomer)
 }
 @Transaction suspend fun collectDue(customer: CustomerEntity, amount: Double, note: String) {
   val paid = amount.coerceIn(0.0, customer.due)
   updateCustomer(customer.copy(due = customer.due - paid))
   if (paid > 0) insertDuePayment(DuePaymentEntity(customerId = customer.id, amount = paid, note = note))
 }
}

@Database(entities=[ProductEntity::class,CustomerEntity::class,SaleEntity::class,SaleItemEntity::class,DuePaymentEntity::class,StockHistoryEntity::class,ExpenseEntity::class], version=3, exportSchema=false)
abstract class ShopDatabase : RoomDatabase() {
 abstract fun dao(): ShopDao
 companion object { @Volatile private var INSTANCE: ShopDatabase? = null
  fun get(context: android.content.Context): ShopDatabase = INSTANCE ?: synchronized(this) { INSTANCE ?: Room.databaseBuilder(context.applicationContext, ShopDatabase::class.java, "smart_shop_bd.db").fallbackToDestructiveMigration().build().also { INSTANCE = it } }
 }
}
