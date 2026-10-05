package com.smartshopbd.app

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.smartshopbd.app.data.*

private val Ink = Color(0xFF172B4D)
private val Green = Color(0xFF087F5B)
private val Pale = Color(0xFFF4F7FA)
private val Muted = Color(0xFF65758B)

class MainActivity : ComponentActivity() {

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)

        setContent {
            SmartShopApp()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartShopApp() {

    val vm: ShopViewModel =
        androidx.lifecycle.viewmodel.compose.viewModel()

    val context = LocalContext.current

    val products by vm.products.collectAsState(initial = emptyList())
    val customers by vm.customers.collectAsState(initial = emptyList())
    val sales by vm.sales.collectAsState(initial = emptyList())

    val totalSales by vm.totalSales.collectAsState(initial = 0.0)
    val due by vm.totalSalesDue.collectAsState(initial = 0.0)
    val expense by vm.totalExpenses.collectAsState(initial = 0.0)
    val profit by vm.totalProfit.collectAsState(initial = 0.0)

    var tab by remember { mutableStateOf("হোম") }
    var dialog by remember { mutableStateOf<String?>(null) }
    var toast by remember { mutableStateOf("") }
    var lastSale by remember { mutableStateOf<SaleResult?>(null) }

    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = Green,
            secondary = Ink,
            background = Pale,
            surface = Color.White,
            onPrimary = Color.White
        )
    ) {

        Scaffold(

            topBar = {

                TopAppBar(
                    title = {

                        Column {

                            Text(
                                "Smart Shop BD",
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )

                            Text(
                                "আপনার দোকান, আপনার হিসাব",
                                fontSize = 11.sp,
                                color = Muted
                            )
                        }
                    },

                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },

            bottomBar = {

                NavigationBar(
                    containerColor = Color.White
                ) {

                    listOf(
                        "হোম" to Icons.Default.Dashboard,
                        "বিক্রয়" to Icons.Default.PointOfSale,
                        "পণ্য" to Icons.Default.Inventory2,
                        "কাস্টমার" to Icons.Default.People,
                        "রিপোর্ট" to Icons.Default.Assessment
                    ).forEach { (label, icon) ->

                        NavigationBarItem(

                            selected = tab == label,

                            onClick = {
                                tab = label
                            },

                            icon = {
                                Icon(
                                    icon,
                                    null
                                )
                            },

                            label = {
                                Text(
                                    label,
                                    fontSize = 10.sp
                                )
                            }
                        )
                    }
                }
            },

            containerColor = Pale

        ) { pad ->

            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
            ) {

                when (tab) {

                    "হোম" -> {

                        Dashboard(
                            s = totalSales,
                            e = expense,
                            d = due,
                            p = profit,
                            ps = products,
                            cs = customers,

                            onSale = {
                                tab = "বিক্রয়"
                            },

                            onProduct = {
                                dialog = "product"
                            },

                            onDue = {
                                tab = "কাস্টমার"
                            },

                            onExpense = {
                                dialog = "expense"
                            }
                        )
                    }

                    "বিক্রয়" -> {

                        PosScreen(
                            ps = products,
                            cs = customers,
                            onOpen = {
                                dialog = "sale"
                            }
                        )
                    }

                    "পণ্য" -> {

                        ProductsScreen(
                            ps = products,
                            add = {
                                dialog = "product"
                            }
                        )
                    }

                    "কাস্টমার" -> {

                        CustomersScreen(
                            cs = customers,

                            add = {
                                dialog = "customer"
                            },

                            onDue = { customer ->
                                dialog =
                                    "due:${customer.id}"
                            }
                        )
                    }

                    "রিপোর্ট" -> {

                        ReportsScreen(
                            s = totalSales,
                            d = due,
                            e = expense,
                            p = profit,
                            sales = sales
                        )
                    }
                }

                if (toast.isNotBlank()) {

                    AlertDialog(

                        onDismissRequest = {
                            toast = ""
                        },

                        confirmButton = {

                            TextButton(
                                onClick = {
                                    toast = ""
                                }
                            ) {
                                Text("ঠিক আছে")
                            }
                        },

                        title = {
                            Text("তথ্য")
                        },

                        text = {
                            Text(toast)
                        }
                    )
                }
            }

            when (val d = dialog) {

                "product" -> {

                    ProductDialog(
                        close = {
                            dialog = null
                        }
                    ) { n, pu, pr, s, b ->

                        vm.addProduct(
                            n,
                            pu,
                            pr,
                            s,
                            b
                        )

                        dialog = null
                        toast = "পণ্য যোগ হয়েছে"
                    }
                }

                "customer" -> {

                    CustomerDialog(
                        close = {
                            dialog = null
                        }
                    ) { n, p ->

                        vm.addCustomer(
                            n,
                            p
                        )

                        dialog = null
                        toast = "কাস্টমার যোগ হয়েছে"
                    }
                }

                "expense" -> {

                    ExpenseDialog(
                        close = {
                            dialog = null
                        }
                    ) { t, a ->

                        vm.addExpense(
                            t,
                            a
                        )

                        dialog = null
                        toast = "খরচ যোগ হয়েছে"
                    }
                }

                "sale" -> {

                    SaleDialog(
                        ps = products,
                        cs = customers,
                        close = {
                            dialog = null
                        }
                    ) { cart, customer, discount, paid, payment ->

                        vm.checkout(

                            cart,
                            customer,
                            discount,
                            paid,
                            payment,

                            { r ->

                                lastSale = r
                                dialog = null
                                toast =
                                    "বিক্রি সম্পন্ন: ${r.invoice}"
                            },

                            { e ->

                                toast = e
                            }
                        )
                    }
                }

                else -> {

                    if (d?.startsWith("due:") == true) {

                        val id =
                            d.substringAfter(":")
                                .toLongOrNull()

                        val customer =
                            customers.firstOrNull {
                                it.id == id
                            }

                        if (customer != null) {

                            DueDialog(

                                c = customer,

                                close = {
                                    dialog = null
                                }

                            ) { amount, note ->

                                vm.collectDue(
                                    customer,
                                    amount,
                                    note
                                ) {

                                    dialog = null
                                    toast =
                                        "বাকি পরিশোধ যোগ হয়েছে"
                                }
                            }
                        }
                    }
                }
            }

            lastSale?.let { r ->

                AlertDialog(

                    onDismissRequest = {
                        lastSale = null
                    },

                    confirmButton = {

                        TextButton(

                            onClick = {

                                generateInvoice(
                                    context,
                                    r
                                )

                                lastSale = null
                            }

                        ) {
                            Text("PDF Invoice")
                        }
                    },

                    dismissButton = {

                        TextButton(

                            onClick = {
                                lastSale = null
                            }

                        ) {
                            Text("বন্ধ")
                        }
                    },

                    title = {
                        Text("বিক্রি সফল")
                    },

                    text = {

                        Text(
                            "${r.invoice}\n" +
                                    "মোট: ৳${money(r.total)}\n" +
                                    "পরিশোধ: ৳${money(r.paid)}\n" +
                                    "বাকি: ৳${money(r.due)}"
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun Dashboard(
    s: Double,
    e: Double,
    d: Double,
    p: Double,
    ps: List<ProductEntity>,
    cs: List<CustomerEntity>,
    onSale: () -> Unit,
    onProduct: () -> Unit,
    onDue: () -> Unit,
    onExpense: () -> Unit
) {

    val lowStock = ps.filter { it.stock < 10 }

    LazyColumn(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp),

        verticalArrangement = Arrangement.spacedBy(14.dp),

        contentPadding = PaddingValues(
            top = 14.dp,
            bottom = 28.dp
        )
    ) {

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Ink
                )
            ) {

                Column(
                    Modifier.padding(20.dp)
                ) {

                    Text(
                        "আসসালামু আলাইকুম 👋",
                        color = Color.White,
                        fontSize = 14.sp
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        "SmartShopBD",
                        color = Color.White,
                        fontSize = 27.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        "আপনার দোকান, আপনার হিসাব",
                        color = Color(0xFFD7E3F4),
                        fontSize = 13.sp
                    )

                    Spacer(
                        Modifier.height(16.dp)
                    )

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                "আজকের ব্যবসা",
                                color = Color(0xFFB8C7DA),
                                fontSize = 11.sp
                            )

                            Text(
                                "এক নজরে সবকিছু",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF24446D)
                        ) {

                            Text(
                                "LIVE",
                                modifier = Modifier.padding(
                                    horizontal = 12.dp,
                                    vertical = 7.dp
                                ),
                                color = Color(0xFF7EF0B8),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        item {

            Text(
                "ব্যবসার সারাংশ",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Text(
                "আপনার দোকানের বর্তমান হিসাব",
                fontSize = 12.sp,
                color = Muted
            )
        }

        item {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Stat(
                    "বিক্রি",
                    "৳${money(s)}",
                    Icons.Default.TrendingUp,
                    Green,
                    Modifier.weight(1f)
                )

                Stat(
                    "লাভ",
                    "৳${money(p)}",
                    Icons.Default.ShowChart,
                    Color(0xFF2563EB),
                    Modifier.weight(1f)
                )
            }
        }

        item {

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Stat(
                    "বাকি",
                    "৳${money(d)}",
                    Icons.Default.MenuBook,
                    Color(0xFF7C3AED),
                    Modifier.weight(1f)
                )

                Stat(
                    "খরচ",
                    "৳${money(e)}",
                    Icons.Default.Payments,
                    Color(0xFFEA580C),
                    Modifier.weight(1f)
                )
            }
        }

        item {

            Text(
                "দ্রুত কাজ",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Ink
            )

            Spacer(
                Modifier.height(8.dp)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Quick(
                    "নতুন বিক্রি",
                    Icons.Default.PointOfSale,
                    onSale,
                    Green,
                    Modifier.weight(1f)
                )

                Quick(
                    "পণ্য যোগ",
                    Icons.Default.AddBox,
                    onProduct,
                    Color(0xFF2563EB),
                    Modifier.weight(1f)
                )
            }

            Spacer(
                Modifier.height(10.dp)
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {

                Quick(
                    "বাকি",
                    Icons.Default.MenuBook,
                    onDue,
                    Color(0xFF7C3AED),
                    Modifier.weight(1f)
                )

                Quick(
                    "খরচ",
                    Icons.Default.ReceiptLong,
                    onExpense,
                    Color(0xFFEA580C),
                    Modifier.weight(1f)
                )
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                )
            ) {

                Column(
                    Modifier.padding(16.dp)
                ) {

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (lowStock.isNotEmpty()) {
                                Color(0xFFFFF1E8)
                            } else {
                                Color(0xFFE8F8F1)
                            }
                        ) {

                            Icon(
                                if (lowStock.isNotEmpty()) {
                                    Icons.Default.WarningAmber
                                } else {
                                    Icons.Default.CheckCircle
                                },
                                contentDescription = null,
                                modifier = Modifier.padding(9.dp),
                                tint = if (lowStock.isNotEmpty()) {
                                    Color(0xFFEA580C)
                                } else {
                                    Green
                                }
                            )
                        }

                        Spacer(
                            Modifier.width(12.dp)
                        )

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                "স্টক স্ট্যাটাস",
                                fontWeight = FontWeight.Bold,
                                color = Ink
                            )

                            Text(
                                if (lowStock.isNotEmpty()) {
                                    "${lowStock.size}টি পণ্যের স্টক কম"
                                } else {
                                    "সব পণ্যের স্টক স্বাভাবিক আছে"
                                },
                                fontSize = 12.sp,
                                color = Muted
                            )
                        }
                    }

                    if (lowStock.isNotEmpty()) {

                        Spacer(
                            Modifier.height(12.dp)
                        )

                        lowStock.take(3).forEach { product ->

                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),

                                verticalAlignment =
                                    Alignment.CenterVertically
                            ) {

                                Text(
                                    product.name,
                                    Modifier.weight(1f),
                                    fontSize = 13.sp,
                                    color = Ink
                                )

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFFFF1E8)
                                ) {

                                    Text(
                                        "${product.stock}টি",
                                        modifier = Modifier.padding(
                                            horizontal = 9.dp,
                                            vertical = 5.dp
                                        ),
                                        color = Color(0xFFEA580C),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFEFF4FA)
                )
            ) {

                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(18.dp),

                    horizontalAlignment =
                        Alignment.CenterHorizontally
                ) {

                    Text(
                        "SmartShopBD",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Ink
                    )

                    Text(
                        "Smart business • Simple হিসাব • Better control",
                        fontSize = 11.sp,
                        color = Muted,
                        textAlign = TextAlign.Center
                    )

                    Spacer(
                        Modifier.height(5.dp)
                    )

                    Text(
                        "Powered By Rahanuzzaman Monir",
                        fontSize = 10.sp,
                        color = Color(0xFF49647F)
                    )
                }
            }
        }
    }
}

@Composable
fun Stat(
    t: String,
    v: String,
    i: androidx.compose.ui.graphics.vector.ImageVector,
    c: Color,
    m: Modifier
) {

    Card(
        m,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            Modifier.padding(16.dp)
        ) {

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {

                Surface(
                    shape = RoundedCornerShape(11.dp),
                    color = c.copy(alpha = 0.10f)
                ) {

                    Icon(
                        i,
                        contentDescription = null,
                        modifier = Modifier.padding(8.dp),
                        tint = c
                    )
                }
            }

            Spacer(
                Modifier.height(12.dp)
            )

            Text(
                v,
                fontSize = 21.sp,
                fontWeight = FontWeight.ExtraBold,
                color = c
            )

            Text(
                t,
                fontSize = 12.sp,
                color = Muted
            )
        }
    }
}

@Composable
fun Quick(
    t: String,
    i: androidx.compose.ui.graphics.vector.ImageVector,
    on: () -> Unit,
    c: Color,
    m: Modifier
) {

    Card(
        m.clickable {
            on()
        },

        shape = RoundedCornerShape(20.dp),

        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {

        Column(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),

            horizontalAlignment =
                Alignment.CenterHorizontally
        ) {

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = c.copy(alpha = 0.10f)
            ) {

                Icon(
                    i,
                    contentDescription = null,
                    modifier = Modifier.padding(11.dp),
                    tint = c
                )
            }

            Spacer(
                Modifier.height(8.dp)
            )

            Text(
                t,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = c,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun PosScreen(
    ps: List<ProductEntity>,
    cs: List<CustomerEntity>,
    onOpen: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Text(
            "বিক্রয় / POS",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Ink
        )

        Text(
            "একাধিক পণ্য দিয়ে কার্ট তৈরি করুন",
            color = Muted
        )

        Spacer(
            Modifier.height(14.dp)
        )

        Button(
            onClick = onOpen,
            modifier =
                Modifier.fillMaxWidth()
        ) {

            Icon(
                Icons.Default.ShoppingCart,
                null
            )

            Spacer(
                Modifier.width(8.dp)
            )

            Text(
                "নতুন বিক্রি শুরু করুন"
            )
        }

        Spacer(
            Modifier.height(12.dp)
        )

        Text(
            "পণ্য: ${ps.size}টি • কাস্টমার: ${cs.size} জন",
            color = Muted
        )
    }
}

@Composable
fun ProductsScreen(
    ps: List<ProductEntity>,
    add: () -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "পণ্য ও স্টক",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    "${ps.size}টি পণ্য",
                    color = Muted
                )
            }

            Button(
                onClick = add
            ) {
                Text("+ পণ্য")
            }
        }

        Spacer(
            Modifier.height(10.dp)
        )

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(ps) { p ->

                Card(

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        ),

                    shape =
                        RoundedCornerShape(14.dp)

                ) {

                    Row(

                        Modifier
                            .fillMaxWidth()
                            .padding(13.dp),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                p.name,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color = Ink
                            )

                            Text(
                                "বিক্রি ৳${money(p.price)} • ক্রয় ৳${money(p.purchasePrice)}",
                                fontSize = 11.sp,
                                color = Muted
                            )

                            if (p.barcode.isNotBlank()) {

                                Text(
                                    "Barcode: ${p.barcode}",
                                    fontSize = 10.sp,
                                    color = Muted
                                )
                            }
                        }

                        Text(
                            "${p.stock}টি",

                            fontWeight =
                                FontWeight.Bold,

                            color =
                                if (p.stock < 10) {
                                    Color(0xFFB45309)
                                } else {
                                    Green
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CustomersScreen(
    cs: List<CustomerEntity>,
    add: () -> Unit,
    onDue: (CustomerEntity) -> Unit
) {

    Column(
        Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {

        Row(
            verticalAlignment =
                Alignment.CenterVertically
        ) {

            Column(
                Modifier.weight(1f)
            ) {

                Text(
                    "কাস্টমার ও বাকি",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Ink
                )

                Text(
                    "মোট বাকি ৳${money(cs.sumOf { it.due })}",
                    color = Muted
                )
            }

            Button(
                onClick = add
            ) {
                Text("+ যোগ")
            }
        }

        Spacer(
            Modifier.height(10.dp)
        )

        LazyColumn(
            verticalArrangement =
                Arrangement.spacedBy(8.dp)
        ) {

            items(cs) { c ->

                Card(

                    Modifier
                        .fillMaxWidth()
                        .clickable {

                            if (c.due > 0) {
                                onDue(c)
                            }
                        },

                    colors =
                        CardDefaults.cardColors(
                            containerColor =
                                Color.White
                        )
                ) {

                    Row(

                        Modifier.padding(14.dp),

                        verticalAlignment =
                            Alignment.CenterVertically
                    ) {

                        Column(
                            Modifier.weight(1f)
                        ) {

                            Text(
                                c.name,
                                fontWeight =
                                    FontWeight.SemiBold,
                                color = Ink
                            )

                            Text(
                                c.phone,
                                color = Muted,
                                fontSize = 12.sp
                            )
                        }

                        Column(
                            horizontalAlignment =
                                Alignment.End
                        ) {

                            Text(
                                "৳${money(c.due)}",
                                fontWeight =
                                    FontWeight.Bold,

                                color =
                                    if (c.due > 0) {
                                        Color(0xFFB45309)
                                    } else {
                                        Green
                                    }
                            )

                            if (c.due > 0) {

                                Text(
                                    "ট্যাপ করে পরিশোধ",
                                    fontSize = 9.sp,
                                    color = Muted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReportsScreen(
    s: Double,
    d: Double,
    e: Double,
    p: Double,
    sales: List<SaleEntity>
) {

    LazyColumn(

        Modifier
            .fillMaxSize()
            .padding(16.dp),

        verticalArrangement =
            Arrangement.spacedBy(10.dp)

    ) {

        item {

            Text(
                "রিপোর্ট",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Ink
            )

            Text(
                "ব্যবসার সারাংশ",
                color = Muted
            )
        }

        item {

            Stat(
                "মোট বিক্রি",
                "৳${money(s)}",
                Icons.Default.TrendingUp,
                Green,
                Modifier.fillMaxWidth()
            )
        }

        item {

            Stat(
                "মোট লাভ",
                "৳${money(p)}",
                Icons.Default.ShowChart,
                Color(0xFF2563EB),
                Modifier.fillMaxWidth()
            )
        }

        item {

            Stat(
                "মোট বাকি",
                "৳${money(d)}",
                Icons.Default.MenuBook,
                Color(0xFF7C3AED),
                Modifier.fillMaxWidth()
            )
        }

        item {

            Stat(
                "মোট খরচ",
                "৳${money(e)}",
                Icons.Default.Payments,
                Color(0xFFB45309),
                Modifier.fillMaxWidth()
            )
        }

        item {

            Text(
                "সাম্প্রতিক বিক্রি",
                fontWeight = FontWeight.Bold,
                color = Ink
            )
        }

        items(
            sales.take(20)
        ) { x ->

            Card(
                Modifier.fillMaxWidth(),

                colors =
                    CardDefaults.cardColors(
                        containerColor =
                            Color.White
                    )
            ) {

                Row(
                    Modifier.padding(12.dp)
                ) {

                    Column(
                        Modifier.weight(1f)
                    ) {

                        Text(
                            x.invoiceNo,
                            fontWeight =
                                FontWeight.SemiBold
                        )

                        Text(
                            x.customerName,
                            fontSize = 11.sp,
                            color = Muted
                        )
                    }

                    Text(
                        "৳${money(x.total)}",
                        fontWeight =
                            FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun ProductDialog(
    close: () -> Unit,
    add: (
        String,
        Double,
        Double,
        Int,
        String
    ) -> Unit
) {

    var n by remember {
        mutableStateOf("")
    }

    var pu by remember {
        mutableStateOf("")
    }

    var pr by remember {
        mutableStateOf("")
    }

    var s by remember {
        mutableStateOf("")
    }

    var b by remember {
        mutableStateOf("")
    }

    FormDialog(

        title = "পণ্য যোগ",

        close = close,

        save = {

            if (n.isNotBlank()) {

                add(
                    n,
                    pu.toDoubleOrNull() ?: 0.0,
                    pr.toDoubleOrNull() ?: 0.0,
                    s.toIntOrNull() ?: 0,
                    b
                )
            }
        }

    ) {

        Field(
            "পণ্যের নাম",
            n
        ) {
            n = it
        }

        Field(
            "ক্রয় মূল্য",
            pu,
            KeyboardType.Decimal
        ) {
            pu = it
        }

        Field(
            "বিক্রয় মূল্য",
            pr,
            KeyboardType.Decimal
        ) {
            pr = it
        }

        Field(
            "স্টক",
            s,
            KeyboardType.Number
        ) {
            s = it
        }

        Field(
            "Barcode",
            b
        ) {
            b = it
        }
    }
}

@Composable
fun CustomerDialog(
    close: () -> Unit,
    add: (
        String,
        String
    ) -> Unit
) {

    var n by remember {
        mutableStateOf("")
    }

    var p by remember {
        mutableStateOf("")
    }

    FormDialog(

        title = "কাস্টমার যোগ",

        close = close,

        save = {

            if (n.isNotBlank()) {
                add(n, p)
            }
        }

    ) {

        Field(
            "নাম",
            n
        ) {
            n = it
        }

        Field(
            "ফোন",
            p,
            KeyboardType.Phone
        ) {
            p = it
        }
    }
}

@Composable
fun ExpenseDialog(
    close: () -> Unit,
    add: (
        String,
        Double
    ) -> Unit
) {

    var t by remember {
        mutableStateOf("")
    }

    var a by remember {
        mutableStateOf("")
    }

    FormDialog(

        title = "খরচ যোগ",

        close = close,

        save = {

            val amount =
                a.toDoubleOrNull()

            if (amount != null) {

                add(
                    if (t.isBlank()) {
                        "Expense"
                    } else {
                        t
                    },
                    amount
                )
            }
        }

    ) {

        Field(
            "খরচের নাম",
            t
        ) {
            t = it
        }

        Field(
            "পরিমাণ",
            a,
            KeyboardType.Decimal
        ) {
            a = it
        }
    }
}

@Composable
fun DueDialog(
    c: CustomerEntity,
    close: () -> Unit,
    pay: (
        Double,
        String
    ) -> Unit
) {

    var a by remember {
        mutableStateOf("")
    }

    var n by remember {
        mutableStateOf("")
    }

    FormDialog(

        title = "বাকি পরিশোধ: ${c.name}",

        close = close,

        save = {

            val x =
                a.toDoubleOrNull()

            if (x != null && x > 0) {
                pay(x, n)
            }
        }

    ) {

        Text(
            "বর্তমান বাকি: ৳${money(c.due)}",
            color = Muted
        )

        Field(
            "পরিশোধের পরিমাণ",
            a,
            KeyboardType.Decimal
        ) {
            a = it
        }

        Field(
            "নোট",
            n
        ) {
            n = it
        }
    }
}

@Composable
fun SaleDialog(
    ps: List<ProductEntity>,
    cs: List<CustomerEntity>,
    close: () -> Unit,
    checkout: (
        List<CartItem>,
        CustomerEntity?,
        Double,
        Double,
        String
    ) -> Unit
) {

    var cart by remember {
        mutableStateOf(
            emptyList<CartItem>()
        )
    }

    var search by remember {
        mutableStateOf("")
    }

    var discount by remember {
        mutableStateOf("")
    }

    var paid by remember {
        mutableStateOf("")
    }

    var customer by remember {
        mutableStateOf<CustomerEntity?>(null)
    }

    var payment by remember {
        mutableStateOf("Cash")
    }

    AlertDialog(

        onDismissRequest = close,

        confirmButton = {

            Button(

                onClick = {

                    checkout(
                        cart,
                        customer,
                        discount.toDoubleOrNull()
                            ?: 0.0,
                        paid.toDoubleOrNull()
                            ?: 0.0,
                        payment
                    )
                },

                enabled =
                    cart.isNotEmpty()

            ) {

                Text(
                    "বিক্রি সম্পন্ন"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = close
            ) {

                Text(
                    "বাতিল"
                )
            }
        },

        title = {

            Text(
                "নতুন বিক্রি"
            )
        },

        text = {

            Column(
                Modifier.fillMaxWidth()
            ) {

                OutlinedTextField(

                    value = search,

                    onValueChange = {
                        search = it
                    },

                    label = {
                        Text(
                            "পণ্য/Barcode খুঁজুন"
                        )
                    },

                    singleLine = true,

                    modifier =
                        Modifier.fillMaxWidth()
                )

                val filtered =
                    ps
                        .filter {

                            it.name.contains(
                                search,
                                true
                            ) ||

                            it.barcode.contains(
                                search,
                                true
                            )
                        }
                        .take(5)

                LazyColumn(
                    Modifier.heightIn(
                        max = 160.dp
                    )
                ) {

                    items(filtered) { p ->

                        Text(

                            "${p.name} — ৳${money(p.price)} (${p.stock})",

                            Modifier
                                .fillMaxWidth()
                                .clickable {

                                    val old =
                                        cart.firstOrNull {
                                            it.product.id ==
                                                p.id
                                        }

                                    cart =

                                        if (old == null) {

                                            cart +
                                                CartItem(
                                                    p,
                                                    1
                                                )

                                        } else {

                                            cart.map {

                                                if (
                                                    it.product.id ==
                                                    p.id
                                                ) {

                                                    it.copy(

                                                        quantity =
                                                            (
                                                                it.quantity + 1
                                                            ).coerceAtMost(
                                                                p.stock
                                                            )
                                                    )

                                                } else {
                                                    it
                                                }
                                            }
                                        }
                                }
                                .padding(8.dp)
                        )
                    }
                }

                cart.forEach { ci ->

                    Row(

                        Modifier.fillMaxWidth(),

                        verticalAlignment =
                            Alignment.CenterVertically

                    ) {

                        Text(

                            "${ci.product.name} × ${ci.quantity}",

                            Modifier.weight(1f),

                            fontSize = 12.sp
                        )

                        Text(
                            "৳${money(ci.product.price * ci.quantity)}",
                            fontSize = 12.sp
                        )
                    }
                }

                val sub =
                    cart.sumOf {
                        it.product.price *
                                it.quantity
                    }

                Spacer(
                    Modifier.height(5.dp)
                )

                Text(
                    "Subtotal ৳${money(sub)}",
                    fontWeight =
                        FontWeight.Bold
                )

                Field(
                    "Discount",
                    discount,
                    KeyboardType.Decimal
                ) {
                    discount = it
                }

                Field(
                    "Paid",
                    paid,
                    KeyboardType.Decimal
                ) {
                    paid = it
                }

                val total =
                    (
                        sub -
                            (
                                discount.toDoubleOrNull()
                                    ?: 0.0
                            )
                    ).coerceAtLeast(0.0)

                Text(
                    "Total ৳${money(total)}",
                    fontWeight =
                        FontWeight.Bold,
                    color = Green
                )

                if (cs.isNotEmpty()) {

                    Text(
                        "Customer: ${
                            customer?.name
                                ?: "Walk-in Customer"
                        }",
                        fontSize = 12.sp,
                        color = Muted
                    )

                    TextButton(

                        onClick = {

                            customer =
                                if (customer == null) {
                                    cs.first()
                                } else {
                                    null
                                }
                        }

                    ) {

                        Text(

                            if (customer == null) {
                                "প্রথম কাস্টমার নির্বাচন"
                            } else {
                                "Walk-in"
                            }
                        )
                    }
                }

                Text(
                    "Payment: $payment",
                    fontSize = 12.sp
                )

                Row {

                    listOf(
                        "Cash",
                        "bKash",
                        "Nagad",
                        "Card",
                        "Due"
                    ).forEach { method ->

                        TextButton(

                            onClick = {
                                payment = method
                            }

                        ) {

                            Text(
                                method
                            )
                        }
                    }
                }
            }
        }
    )
}

@Composable
fun FormDialog(
    title: String,
    close: () -> Unit,
    save: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {

    AlertDialog(

        onDismissRequest = close,

        confirmButton = {

            Button(
                onClick = save
            ) {

                Text(
                    "সংরক্ষণ"
                )
            }
        },

        dismissButton = {

            TextButton(
                onClick = close
            ) {

                Text(
                    "বাতিল"
                )
            }
        },

        title = {
            Text(title)
        },

        text = {

            Column(
                content = content
            )
        }
    )
}

@Composable
fun Field(
    label: String,
    value: String,
    type: KeyboardType = KeyboardType.Text,
    onChange: (String) -> Unit
) {

    OutlinedTextField(

        value = value,

        onValueChange = onChange,

        label = {
            Text(label)
        },

        singleLine = true,

        keyboardOptions =
            KeyboardOptions(
                keyboardType = type
            ),

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(
                    vertical = 3.dp
                )
    )
}

fun money(
    v: Double
): String {

    return String.format(
        Locale.US,
        "%.2f",
        v
    )
}

fun generateInvoice(
    context: Context,
    r: SaleResult
) {

    val dir =
        File(
            context.cacheDir,
            "invoices"
        )

    dir.mkdirs()

    val file =
        File(
            dir,
            "${r.invoice}.pdf"
        )

    val doc =
        PdfDocument()

    val page =
        doc.startPage(

            PdfDocument.PageInfo
                .Builder(
                    595,
                    842,
                    1
                )
                .create()
        )

    val canvas =
        page.canvas

    val paint =
        Paint().apply {

            color =
                Color.Black.toArgb()

            textSize =
                22f

            typeface =
                Typeface.DEFAULT_BOLD
        }

    canvas.drawText(
        "Smart Shop BD",
        40f,
        60f,
        paint
    )

    paint.textSize =
        14f

    paint.typeface =
        Typeface.DEFAULT

    canvas.drawText(
        "Invoice: ${r.invoice}",
        40f,
        90f,
        paint
    )

    canvas.drawText(
        "Total: ৳${money(r.total)}",
        40f,
        125f,
        paint
    )

    canvas.drawText(
        "Paid: ৳${money(r.paid)}",
        40f,
        150f,
        paint
    )

    canvas.drawText(
        "Due: ৳${money(r.due)}",
        40f,
        175f,
        paint
    )

    canvas.drawText(

        SimpleDateFormat(
            "dd-MM-yyyy HH:mm",
            Locale.US
        ).format(Date()),

        40f,
        210f,
        paint
    )

    doc.finishPage(
        page
    )

    FileOutputStream(
        file
    ).use {

        doc.writeTo(it)
    }

    doc.close()

    val uri =
        androidx.core.content.FileProvider
            .getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

    context.startActivity(

        Intent.createChooser(

            Intent(
                Intent.ACTION_SEND
            ).apply {

                type =
                    "application/pdf"

                putExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )

                addFlags(
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            },

            "Invoice share"
        )
    )
}
