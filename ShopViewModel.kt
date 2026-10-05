package com.smartshopbd.app

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.smartshopbd.app.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CartItem(
    val product: ProductEntity,
    val quantity: Int
)

data class SaleResult(
    val invoice: String,
    val total: Double,
    val paid: Double,
    val due: Double
)

class ShopViewModel(app: Application) : AndroidViewModel(app) {

    private val dao = ShopDatabase.get(app).dao()

    val products = dao.products()
    val customers = dao.customers()
    val sales = dao.sales()
    val expenses = dao.expenses()

    val totalSales = dao.totalSales()
    val totalSalesDue = dao.totalSalesDue()
    val totalExpenses = dao.totalExpenses()
    val totalProfit = dao.totalProfit()

    fun addProduct(
        name: String,
        purchase: Double,
        price: Double,
        stock: Int,
        barcode: String = ""
    ) = viewModelScope.launch {

        dao.insertProduct(
            ProductEntity(
                name = name,
                purchasePrice = purchase,
                price = price,
                stock = stock,
                barcode = barcode
            )
        )
    }

    fun addCustomer(
        name: String,
        phone: String
    ) = viewModelScope.launch {

        dao.insertCustomer(
            CustomerEntity(
                name = name,
                phone = phone
            )
        )
    }

    fun addExpense(
        title: String,
        amount: Double
    ) = viewModelScope.launch {

        dao.insertExpense(
            ExpenseEntity(
                title = title,
                amount = amount
            )
        )
    }

    fun checkout(
        cart: List<CartItem>,
        customer: CustomerEntity?,
        discount: Double,
        paid: Double,
        payment: String,
        onDone: (SaleResult) -> Unit,
        onError: (String) -> Unit
    ) = viewModelScope.launch {

        try {

            if (cart.isEmpty()) {
                onError("কার্ট খালি।")
                return@launch
            }

            if (
                cart.any {
                    it.quantity <= 0 ||
                    it.quantity > it.product.stock
                }
            ) {
                onError("কোনো পণ্যের স্টক পর্যাপ্ত নেই।")
                return@launch
            }

            val subtotal =
                cart.sumOf {
                    it.product.price * it.quantity
                }

            val safeDiscount =
                discount.coerceAtLeast(0.0)

            val total =
                (subtotal - safeDiscount)
                    .coerceAtLeast(0.0)

            val actualPaid =
                paid.coerceIn(0.0, total)

            val due =
                total - actualPaid

            val invoice =
                "INV-" +
                        SimpleDateFormat(
                            "yyyyMMdd-HHmmss",
                            Locale.US
                        ).format(Date())

            val sale =
                SaleEntity(
                    invoiceNo = invoice,
                    customerId = customer?.id,
                    customerName =
                        customer?.name
                            ?: "Walk-in Customer",
                    subtotal = subtotal,
                    discount = safeDiscount,
                    total = total,
                    paid = actualPaid,
                    due = due,
                    paymentMethod = payment
                )

            val items =
                cart.map {

                    SaleItemEntity(
                        saleId = 0,
                        productId = it.product.id,
                        productName = it.product.name,
                        quantity = it.quantity,
                        unitPrice = it.product.price,
                        unitCost = it.product.purchasePrice,
                        lineTotal =
                            it.product.price *
                                    it.quantity
                    )
                }

            val updatedProducts =
                cart.map {

                    it.product.copy(
                        stock =
                            it.product.stock -
                                    it.quantity
                    )
                }

            val stockLogs =
                cart.map {

                    StockHistoryEntity(
                        productId = it.product.id,
                        productName = it.product.name,
                        changeQty = -it.quantity,
                        reason = "Sale $invoice"
                    )
                }

            val updatedCustomer =
                if (
                    customer != null &&
                    due > 0
                ) {

                    customer.copy(
                        due = customer.due + due
                    )

                } else {
                    customer
                }

            // Database-এ বিক্রির তথ্য সংরক্ষণ
            dao.saveSale(
                sale = sale,
                items = items,
                updatedProducts = updatedProducts,
                stockLogs = stockLogs,
                updatedCustomer = updatedCustomer
            )

            // সফল হলে
            onDone(
                SaleResult(
                    invoice = invoice,
                    total = total,
                    paid = actualPaid,
                    due = due
                )
            )

        } catch (e: Exception) {

            // Database বা অন্য কোনো error হলে
            onError(
                "বিক্রি সংরক্ষণ করা যায়নি:\n" +
                        (e.message ?: "অজানা সমস্যা")
            )
        }
    }

    fun collectDue(
        c: CustomerEntity,
        amount: Double,
        note: String = "",
        onDone: () -> Unit = {}
    ) = viewModelScope.launch {

        try {

            dao.collectDue(
                c,
                amount,
                note
            )

            onDone()

        } catch (e: Exception) {
            // প্রয়োজনে এখানে error handling করা যাবে
        }
    }
}
