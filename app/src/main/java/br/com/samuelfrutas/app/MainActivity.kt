package br.com.samuelfrutas.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.ArrayAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.Keep
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.text.NumberFormat
import java.util.Locale

@Keep
class MainActivity : Activity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var loginView: LinearLayout
    private var archived = false
    private var products = mutableListOf<Product>()
    private var siteOffline = false
    private var siteListener: ListenerRegistration? = null

    private val green = Color.rgb(15, 138, 75)
    private val bg = Color.rgb(244, 246, 245)
    private val dark = Color.rgb(30, 38, 34)
    private val units = listOf("Un", "Lote", "Duplo", "Dz", "1/8", "1/4", "Bdj", "Cx", "1/2", "GF", "Inteiro")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseConfig.initialize(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        showLogin()
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) showLogin() else showApp(firebaseAuth.currentUser?.email.orEmpty())
        }
    }

    private fun base(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bg)
    }

    private fun tv(text: String, size: Float = 16f, bold: Boolean = false): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(dark)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(16, 10, 16, 10)
    }

    private fun button(text: String, primary: Boolean = false): Button = Button(this).apply {
        this.text = text
        isAllCaps = false
        if (primary) setBackgroundColor(green) else setBackgroundColor(Color.WHITE)
        setTextColor(if (primary) Color.WHITE else dark)
    }

    private fun showLogin() {
        siteListener?.remove()
        root = base()
        loginView = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 40, 28, 28)
        }
        loginView.addView(tv("Samuel Frutas", 28f, true))
        loginView.addView(tv("Painel administrativo do Sistema de Pedidos", 16f))
        val email = EditText(this).apply { hint = "E-mail"; inputType = 33 }
        val password = EditText(this).apply { hint = "Senha"; inputType = 129 }
        loginView.addView(email, lp())
        loginView.addView(password, lp())
        val message = tv("")
        val enter = button("Entrar", true)
        loginView.addView(enter, lp())
        loginView.addView(message)
        enter.setOnClickListener {
            message.text = "Entrando..."
            enter.isEnabled = false
            auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                .addOnSuccessListener { message.text = "" }
                .addOnFailureListener {
                    message.text = if (it.message?.contains("credential", true) == true) "E-mail ou senha inválidos." else "Não foi possível entrar."
                    enter.isEnabled = true
                }
        }
        root.addView(loginView, LinearLayout.LayoutParams(-1, -1))
        setContentView(root)
    }

    private fun showApp(userEmail: String) {
        root = base()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(8, 8, 8, 4)
            setBackgroundColor(Color.WHITE)
        }
        val titleRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        titleRow.addView(tv("Painel de Pedidos", 22f, true), LinearLayout.LayoutParams(0, -2, 1f))
        val logout = button("Sair")
        titleRow.addView(logout, LinearLayout.LayoutParams(-2, -2))
        header.addView(titleRow)
        header.addView(tv(userEmail, 12f))
        val status = button("Site: ONLINE")
        header.addView(status, LinearLayout.LayoutParams(-1, -2))
        status.setOnClickListener {
            val next = !siteOffline
            status.isEnabled = false
            db.collection("config").document("bot").set(mapOf("siteOffline" to next, "updatedAt" to FieldValue.serverTimestamp()), com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener { status.isEnabled = true }
                .addOnFailureListener { status.isEnabled = true; Toast.makeText(this, "Não foi possível alterar o status.", Toast.LENGTH_SHORT).show() }
        }
        logout.setOnClickListener { auth.signOut() }
        root.addView(header)

        val tabs = LinearLayout(this).apply { setPadding(8, 4, 8, 4) }
        val active = button("Ativos", true)
        val archivedButton = button("Arquivados")
        tabs.addView(active, LinearLayout.LayoutParams(0, -2, 1f))
        tabs.addView(archivedButton, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(tabs)

        val actions = LinearLayout(this).apply { setPadding(8, 2, 8, 4) }
        val add = button("+ Adicionar produto", true)
        val reload = button("Atualizar")
        actions.addView(add, LinearLayout.LayoutParams(0, -2, 1f))
        actions.addView(reload, LinearLayout.LayoutParams(0, -2, .55f))
        root.addView(actions)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(10, 4, 10, 20) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        add.setOnClickListener { showProductDialog(null) }
        reload.setOnClickListener { loadProducts() }
        active.setOnClickListener { archived = false; active.isEnabled = false; archivedButton.isEnabled = true; loadProducts() }
        archivedButton.setOnClickListener { archived = true; active.isEnabled = true; archivedButton.isEnabled = false; loadProducts() }
        archivedButton.isEnabled = true
        listenSiteStatus(status)
        loadProducts()
    }

    private fun listenSiteStatus(status: Button) {
        siteListener?.remove()
        siteListener = db.collection("config").document("bot").addSnapshotListener { snap, _ ->
            siteOffline = snap?.getBoolean("siteOffline") == true
            status.text = if (siteOffline) "Site: FORA DO AR" else "Site: ONLINE"
            status.setTextColor(if (siteOffline) Color.DKGRAY else green)
        }
    }

    private fun loadProducts() {
        db.collection("products").orderBy("name", Query.Direction.ASCENDING).get()
            .addOnSuccessListener { snap ->
                products = snap.documents.map { d ->
                    Product(d.id, d.getString("name").orEmpty(), d.getString("image").orEmpty(), d.getBoolean("archived") == true,
                        (d.get("measures") as? List<*>)?.mapNotNull { m ->
                            val map = m as? Map<*, *> ?: return@mapNotNull null
                            Measure((map["quantity"] as? Number)?.toInt() ?: 1, map["unit"]?.toString() ?: "Un", (map["price"] as? Number)?.toDouble() ?: 0.0)
                        } ?: emptyList())
                }.filter { it.archived == archived }.toMutableList()
                renderProducts()
            }
            .addOnFailureListener { Toast.makeText(this, "Erro ao carregar produtos: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun renderProducts() {
        content.removeAllViews()
        if (products.isEmpty()) { content.addView(tv("Nenhum produto nesta lista.", 15f)); return }
        products.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setBackgroundColor(Color.WHITE)
                setPadding(8, 8, 8, 8)
            }
            card.addView(tv(p.name, 18f, true))
            val prices = p.measures.joinToString(" | ") { "${it.quantity} ${it.unit} — ${money(it.price)}" }
            card.addView(tv(if (prices.isBlank()) "Sem medidas" else prices, 14f))
            val actions = LinearLayout(this)
            val edit = button("Editar")
            actions.addView(edit, LinearLayout.LayoutParams(0, -2, 1f))
            if (archived) {
                val restore = button("Desarquivar")
                val delete = button("Excluir")
                actions.addView(restore, LinearLayout.LayoutParams(0, -2, 1f))
                actions.addView(delete, LinearLayout.LayoutParams(0, -2, 1f))
                restore.setOnClickListener { setArchived(p, false) }
                delete.setOnClickListener { confirmDelete(p) }
            } else {
                val archive = button("Arquivar")
                actions.addView(archive, LinearLayout.LayoutParams(0, -2, 1f))
                archive.setOnClickListener { setArchived(p, true) }
            }
            edit.setOnClickListener { showProductDialog(p) }
            card.addView(actions)
            val params = LinearLayout.LayoutParams(-1, -2); params.setMargins(0, 0, 0, 10)
            content.addView(card, params)
        }
    }

    private fun setArchived(p: Product, value: Boolean) {
        db.collection("products").document(p.id).update(mapOf("archived" to value, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { loadProducts() }
            .addOnFailureListener { Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun confirmDelete(p: Product) {
        android.app.AlertDialog.Builder(this).setTitle("Excluir produto?").setMessage("Excluir permanentemente ${p.name}?")
            .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
                db.collection("products").document(p.id).delete().addOnSuccessListener { loadProducts() }
                    .addOnFailureListener { Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun showProductDialog(product: Product?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val name = EditText(this).apply { hint = "Nome"; setText(product?.name.orEmpty()) }
        val image = EditText(this).apply { hint = "Imagem (URL)"; setText(product?.image.orEmpty()) }
        box.addView(name, lp())
        box.addView(image, lp())
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(tv("Medidas e preços", 16f, true))
        box.addView(rows)
        val existing = product?.measures?.ifEmpty { listOf(Measure(1, "Un", 0.0)) } ?: listOf(Measure(1, "Un", 0.0))
        existing.forEach { addMeasureRow(rows, it) }
        val addMeasure = button("+ Adicionar medida")
        box.addView(addMeasure)
        addMeasure.setOnClickListener { addMeasureRow(rows, Measure(1, "Un", 0.0)) }

        val dialog = android.app.AlertDialog.Builder(this).setTitle(if (product == null) "Novo produto" else "Editar produto")
            .setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                val measures = mutableListOf<Measure>()
                for (i in 0 until rows.childCount) {
                    val row = rows.getChildAt(i) as? LinearLayout ?: continue
                    val qty = (row.getChildAt(0) as EditText).text.toString().toIntOrNull() ?: 1
                    val unit = (row.getChildAt(1) as Spinner).selectedItem.toString()
                    val price = (row.getChildAt(2) as EditText).text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    measures.add(Measure(qty.coerceAtLeast(1), unit, price))
                }
                if (n.isBlank()) { name.error = "Informe o nome"; return@setOnClickListener }
                if (measures.none { it.price > 0 }) { Toast.makeText(this, "Adicione pelo menos uma medida com preço.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val data = hashMapOf<String, Any?>(
                    "name" to n, "category" to "produtos", "measures" to measures.map { mapOf("quantity" to it.quantity, "unit" to it.unit, "price" to it.price) },
                    "unit" to measures.first().unit, "price" to measures.first().price,
                    "priceTiers" to measures.filter { it.unit == measures.first().unit && it.quantity > 1 }.map { mapOf("minQty" to it.quantity, "unitPrice" to it.price) },
                    "image" to image.text.toString().trim(), "archived" to false, "updatedAt" to FieldValue.serverTimestamp()
                )
                val task = if (product == null) db.collection("products").add(data.apply { put("createdAt", FieldValue.serverTimestamp()) }) else db.collection("products").document(product.id).update(data.filterValues { it != null } as Map<String, Any>)
                task.addOnSuccessListener { dialog.dismiss(); loadProducts() }.addOnFailureListener { Toast.makeText(this, "Não foi possível salvar: ${it.message}", Toast.LENGTH_LONG).show() }
            }
        }
        dialog.show()
    }

    private fun addMeasureRow(parent: LinearLayout, measure: Measure) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val qty = EditText(this).apply { setText(measure.quantity.toString()); hint = "Qtd"; inputType = 2 }
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, units); setSelection(units.indexOf(measure.unit).coerceAtLeast(0)) }
        val price = EditText(this).apply { setText(if (measure.price == 0.0) "" else String.format(Locale.US, "%.2f", measure.price)); hint = "Preço"; inputType = 8194 }
        val remove = button("×")
        row.addView(qty, LinearLayout.LayoutParams(0, -2, .8f)); row.addView(spinner, LinearLayout.LayoutParams(0, -2, 1.2f)); row.addView(price, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(remove, LinearLayout.LayoutParams(50, -2))
        remove.setOnClickListener { parent.removeView(row) }
        parent.addView(row)
    }

    private fun lp() = LinearLayout.LayoutParams(-1, -2).apply { setMargins(0, 5, 0, 5) }
    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

    override fun onDestroy() {
        siteListener?.remove()
        super.onDestroy()
    }

    data class Measure(val quantity: Int, val unit: String, val price: Double)
    data class Product(val id: String, val name: String, val image: String, val archived: Boolean, val measures: List<Measure>)
}
