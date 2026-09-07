package br.com.samuelfrutas.app

import android.app.Activity
import android.app.AlertDialog
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import java.text.NumberFormat
import java.util.Locale

class MainActivity : Activity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var root: LinearLayout
    private lateinit var content: LinearLayout
    private lateinit var search: EditText
    private var archived = false
    private var products = mutableListOf<Product>()
    private var siteOffline = false
    private var siteListener: ListenerRegistration? = null

    private val green = Color.rgb(16, 142, 76)
    private val greenDark = Color.rgb(10, 104, 55)
    private val bg = Color.rgb(246, 248, 247)
    private val dark = Color.rgb(28, 35, 32)
    private val muted = Color.rgb(103, 113, 108)
    private val border = Color.rgb(224, 230, 226)
    private val white = Color.WHITE
    private val units = listOf("Un", "Lote", "Duplo", "Dz", "1/8", "1/4", "Bdj", "Cx", "1/2", "GF", "Inteiro")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseConfig.initialize(this)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        if (auth.currentUser == null) showLogin() else showApp(auth.currentUser?.email.orEmpty())
        auth.addAuthStateListener { firebaseAuth ->
            if (firebaseAuth.currentUser == null) showLogin()
            else if (::root.isInitialized) showApp(firebaseAuth.currentUser?.email.orEmpty())
        }
    }

    private fun base() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(bg)
    }

    private fun tv(text: String, size: Float = 16f, bold: Boolean = false, color: Int = dark) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 4, 0, 4)
    }

    private fun rounded(color: Int, radius: Float = 18f, strokeColor: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = radius
        if (strokeColor != null) setStroke(1, strokeColor)
    }

    private fun button(text: String, primary: Boolean = false) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 14f
        minHeight = 48
        stateListAnimator = null
        background = rounded(if (primary) green else white, 16f, if (primary) null else border)
        setTextColor(if (primary) white else dark)
        setPadding(18, 0, 18, 0)
    }

    private fun showLogin() {
        siteListener?.remove()
        root = base()
        val scroll = ScrollView(this)
        val login = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(28, 70, 28, 28)
        }
        val brand = tv("Samuel Frutas", 30f, true, greenDark)
        brand.gravity = Gravity.CENTER
        login.addView(brand, LinearLayout.LayoutParams(-1, -2))
        val subtitle = tv("Painel administrativo do Sistema de Pedidos", 15f, false, muted)
        subtitle.gravity = Gravity.CENTER
        login.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 28 })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 24f, border)
            setPadding(22, 22, 22, 22)
            elevation = 3f
        }
        card.addView(tv("Acesso administrativo", 19f, true), lp())
        val email = EditText(this).apply {
            hint = "E-mail"
            inputType = 33
            singleLine = true
            background = rounded(bg, 14f, border)
            setPadding(16, 0, 16, 0)
        }
        val password = EditText(this).apply {
            hint = "Senha"
            inputType = 129
            singleLine = true
            background = rounded(bg, 14f, border)
            setPadding(16, 0, 16, 0)
        }
        card.addView(email, lp().apply { topMargin = 14 })
        card.addView(password, lp().apply { topMargin = 10 })
        val message = tv("", 14f, false, Color.rgb(190, 45, 45))
        val enter = button("Entrar", true)
        card.addView(enter, lp().apply { topMargin = 16 })
        card.addView(message, lp().apply { topMargin = 6 })
        login.addView(card, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(login)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        enter.setOnClickListener {
            message.text = "Entrando..."
            enter.isEnabled = false
            auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString())
                .addOnSuccessListener { message.text = "" }
                .addOnFailureListener { message.text = "E-mail ou senha inválidos."; enter.isEnabled = true }
        }
    }

    private fun showApp(userEmail: String) {
        siteListener?.remove()
        root = base()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(18, 18, 18, 10)
            background = rounded(white, 0f)
            elevation = 2f
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(tv("Samuel Frutas", 13f, true, green))
        titleBox.addView(tv("Painel de Pedidos", 23f, true))
        top.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val logout = button("Sair")
        top.addView(logout, LinearLayout.LayoutParams(-2, 48))
        header.addView(top)
        header.addView(tv(userEmail, 12f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = 2 })

        val status = button("Site: ONLINE", true)
        status.textSize = 13f
        header.addView(status, LinearLayout.LayoutParams(-1, 46).apply { topMargin = 12 })
        root.addView(header)

        val tabs = LinearLayout(this).apply { setPadding(14, 12, 14, 6) }
        val active = button("Ativos", true)
        val archivedButton = button("Arquivados")
        tabs.addView(active, LinearLayout.LayoutParams(0, 48, 1f).apply { rightMargin = 5 })
        tabs.addView(archivedButton, LinearLayout.LayoutParams(0, 48, 1f).apply { leftMargin = 5 })
        root.addView(tabs)

        val searchRow = LinearLayout(this).apply {
            background = rounded(white, 18f, border)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(14, 0, 8, 0)
            elevation = 1f
        }
        val icon = tv("⌕", 26f, true, green)
        searchRow.addView(icon, LinearLayout.LayoutParams(32, 50))
        search = EditText(this).apply {
            hint = "Pesquisar produto..."
            textSize = 15f
            singleLine = true
            background = null
            setPadding(4, 0, 4, 0)
        }
        searchRow.addView(search, LinearLayout.LayoutParams(0, 50, 1f))
        val clear = button("Limpar")
        clear.minHeight = 40
        clear.setPadding(10, 0, 10, 0)
        searchRow.addView(clear, LinearLayout.LayoutParams(-2, 40))
        root.addView(searchRow, LinearLayout.LayoutParams(-1, 56).apply { setMargins(14, 4, 14, 8) })

        val actions = LinearLayout(this).apply { setPadding(14, 0, 14, 8) }
        val add = button("+ Adicionar produto", true)
        val reload = button("Atualizar")
        actions.addView(add, LinearLayout.LayoutParams(0, 48, 1f).apply { rightMargin = 5 })
        actions.addView(reload, LinearLayout.LayoutParams(0, 48, .55f).apply { leftMargin = 5 })
        root.addView(actions)

        val scroll = ScrollView(this)
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(14, 2, 14, 24) }
        scroll.addView(content)
        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        setContentView(root)

        logout.setOnClickListener { auth.signOut() }
        status.setOnClickListener {
            val next = !siteOffline
            status.isEnabled = false
            db.collection("config").document("bot").set(
                mapOf("siteOffline" to next, "updatedAt" to FieldValue.serverTimestamp()), SetOptions.merge()
            ).addOnSuccessListener { status.isEnabled = true }
                .addOnFailureListener { status.isEnabled = true; Toast.makeText(this, "Não foi possível alterar o status.", Toast.LENGTH_SHORT).show() }
        }
        add.setOnClickListener { showProductDialog(null) }
        reload.setOnClickListener { loadProducts() }
        active.setOnClickListener { archived = false; active.background = rounded(green, 16f); active.setTextColor(white); archivedButton.background = rounded(white, 16f, border); archivedButton.setTextColor(dark); loadProducts() }
        archivedButton.setOnClickListener { archived = true; archivedButton.background = rounded(green, 16f); archivedButton.setTextColor(white); active.background = rounded(white, 16f, border); active.setTextColor(dark); loadProducts() }
        clear.setOnClickListener { search.setText("") }
        search.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) { renderProducts() }
            override fun afterTextChanged(s: Editable?) = Unit
        })
        listenSiteStatus(status)
        loadProducts()
    }

    private fun listenSiteStatus(status: Button) {
        siteListener?.remove()
        siteListener = db.collection("config").document("bot").addSnapshotListener { snap, _ ->
            siteOffline = snap?.getBoolean("siteOffline") == true
            status.text = if (siteOffline) "Site: FORA DO AR" else "Site: ONLINE"
            status.background = rounded(if (siteOffline) Color.rgb(238, 239, 239) else green, 16f)
            status.setTextColor(if (siteOffline) dark else white)
        }
    }

    private fun loadProducts() {
        db.collection("products").orderBy("name", Query.Direction.ASCENDING).get().addOnSuccessListener { snap ->
            products = snap.documents.map { d ->
                val measures = (d.get("measures") as? List<*>)?.mapNotNull { raw ->
                    val map = raw as? Map<*, *> ?: return@mapNotNull null
                    Measure((map["quantity"] as? Number)?.toInt() ?: 1, map["unit"]?.toString() ?: "Un", (map["price"] as? Number)?.toDouble() ?: 0.0)
                } ?: emptyList()
                Product(d.id, d.getString("name").orEmpty(), d.getString("image").orEmpty(), d.getBoolean("archived") == true, measures)
            }.filter { it.archived == archived }.toMutableList()
            renderProducts()
        }.addOnFailureListener { Toast.makeText(this, "Erro ao carregar produtos: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun renderProducts() {
        if (!::content.isInitialized) return
        content.removeAllViews()
        val query = if (::search.isInitialized) search.text.toString().trim().lowercase(Locale.getDefault()) else ""
        val visible = products.filter { query.isBlank() || it.name.lowercase(Locale.getDefault()).contains(query) }
        if (visible.isEmpty()) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(20, 60, 20, 60) }
            empty.addView(tv(if (query.isBlank()) "Nenhum produto nesta lista." else "Nenhum produto encontrado.", 16f, true).apply { gravity = Gravity.CENTER })
            if (query.isNotBlank()) empty.addView(tv("Tente outro nome ou palavra-chave.", 13f, false, muted).apply { gravity = Gravity.CENTER })
            content.addView(empty)
            return
        }
        visible.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(white, 20f, border)
                setPadding(16, 14, 16, 14)
                elevation = 1.5f
            }
            val nameRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            nameRow.addView(tv(p.name, 18f, true), LinearLayout.LayoutParams(0, -2, 1f))
            nameRow.addView(tv(if (p.measures.isEmpty()) "" else "${p.measures.size} opção${if (p.measures.size == 1) "" else "ões"}", 12f, false, muted))
            card.addView(nameRow)
            card.addView(tv(if (p.measures.isEmpty()) "Sem medidas cadastradas" else p.measures.joinToString("  •  ") { "${it.quantity} ${it.unit} — ${money(it.price)}" }, 14f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = 6 })
            val actions = LinearLayout(this).apply { setPadding(0, 10, 0, 0) }
            val edit = button("Editar")
            actions.addView(edit, LinearLayout.LayoutParams(0, 44, 1f).apply { rightMargin = 4 })
            edit.setOnClickListener { showProductDialog(p) }
            if (archived) {
                val restore = button("Desarquivar")
                val delete = button("Excluir")
                actions.addView(restore, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4; rightMargin = 4 })
                actions.addView(delete, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4 })
                restore.setOnClickListener { setArchived(p, false) }
                delete.setOnClickListener { confirmDelete(p) }
            } else {
                val archive = button("Arquivar")
                actions.addView(archive, LinearLayout.LayoutParams(0, 44, 1f).apply { leftMargin = 4 })
                archive.setOnClickListener { setArchived(p, true) }
            }
            card.addView(actions)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = 10 })
        }
    }

    private fun setArchived(p: Product, value: Boolean) {
        db.collection("products").document(p.id).update(mapOf("archived" to value, "updatedAt" to FieldValue.serverTimestamp()))
            .addOnSuccessListener { loadProducts() }
            .addOnFailureListener { Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show() }
    }

    private fun confirmDelete(p: Product) {
        AlertDialog.Builder(this).setTitle("Excluir produto?").setMessage("Excluir permanentemente ${p.name}?")
            .setNegativeButton("Cancelar", null).setPositiveButton("Excluir") { _, _ ->
                db.collection("products").document(p.id).delete().addOnSuccessListener { loadProducts() }
                    .addOnFailureListener { Toast.makeText(this, "Erro: ${it.message}", Toast.LENGTH_LONG).show() }
            }.show()
    }

    private fun showProductDialog(product: Product?) {
        val box = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(24, 8, 24, 0) }
        val name = EditText(this).apply { hint = "Nome"; setText(product?.name.orEmpty()) }
        val image = EditText(this).apply { hint = "Imagem (URL)"; setText(product?.image.orEmpty()) }
        box.addView(name, lp()); box.addView(image, lp()); box.addView(tv("Medidas e preços", 16f, true).apply { setPadding(0, 14, 0, 6) })
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; box.addView(rows)
        (product?.measures?.ifEmpty { listOf(Measure(1, "Un", 0.0)) } ?: listOf(Measure(1, "Un", 0.0))).forEach { addMeasureRow(rows, it) }
        val addMeasure = button("+ Adicionar medida"); box.addView(addMeasure); addMeasure.setOnClickListener { addMeasureRow(rows, Measure(1, "Un", 0.0)) }
        val dialog = AlertDialog.Builder(this).setTitle(if (product == null) "Novo produto" else "Editar produto").setView(box).setNegativeButton("Cancelar", null).setPositiveButton("Salvar", null).create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim(); val measures = mutableListOf<Measure>()
                for (i in 0 until rows.childCount) {
                    val row = rows.getChildAt(i) as? LinearLayout ?: continue
                    val qty = (row.getChildAt(0) as EditText).text.toString().toIntOrNull() ?: 1
                    val unit = (row.getChildAt(1) as Spinner).selectedItem.toString()
                    val price = (row.getChildAt(2) as EditText).text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    measures.add(Measure(qty.coerceAtLeast(1), unit, price))
                }
                if (n.isBlank()) { name.error = "Informe o nome"; return@setOnClickListener }
                if (measures.none { it.price > 0 }) { Toast.makeText(this, "Adicione pelo menos uma medida com preço.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val data = hashMapOf<String, Any>(
                    "name" to n, "category" to "produtos", "measures" to measures.map { mapOf("quantity" to it.quantity, "unit" to it.unit, "price" to it.price) },
                    "unit" to measures.first().unit, "price" to measures.first().price,
                    "priceTiers" to measures.filter { it.unit == measures.first().unit && it.quantity > 1 }.map { mapOf("minQty" to it.quantity, "unitPrice" to it.price) },
                    "image" to image.text.toString().trim(), "archived" to false, "updatedAt" to FieldValue.serverTimestamp()
                )
                if (product == null) {
                    data["createdAt"] = FieldValue.serverTimestamp()
                    db.collection("products").add(data).addOnSuccessListener { dialog.dismiss(); loadProducts() }
                        .addOnFailureListener { Toast.makeText(this, "Não foi possível salvar: ${it.message}", Toast.LENGTH_LONG).show() }
                } else {
                    db.collection("products").document(product.id).update(data).addOnSuccessListener { dialog.dismiss(); loadProducts() }
                        .addOnFailureListener { Toast.makeText(this, "Não foi possível salvar: ${it.message}", Toast.LENGTH_LONG).show() }
                }
            }
        }
        dialog.show()
    }

    private fun addMeasureRow(parent: LinearLayout, measure: Measure) {
        val row = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val qty = EditText(this).apply { setText(measure.quantity.toString()); hint = "Qtd"; inputType = 2 }
        val spinner = Spinner(this).apply { adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, units); setSelection(units.indexOf(measure.unit).coerceAtLeast(0)) }
        val price = EditText(this).apply { setText(if (measure.price == 0.0) "" else String.format(Locale.US, "%.2f", measure.price)); hint = "Preço"; inputType = 8194 }
        val remove = button("×"); remove.minWidth = 46
        row.addView(qty, LinearLayout.LayoutParams(0, -2, .8f)); row.addView(spinner, LinearLayout.LayoutParams(0, -2, 1.2f)); row.addView(price, LinearLayout.LayoutParams(0, -2, 1f)); row.addView(remove, LinearLayout.LayoutParams(50, -2)); remove.setOnClickListener { parent.removeView(row) }; parent.addView(row)
    }

    private fun lp() = LinearLayout.LayoutParams(-1, 54).apply { setMargins(0, 5, 0, 5) }
    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)
    override fun onDestroy() { siteListener?.remove(); super.onDestroy() }

    data class Measure(val quantity: Int, val unit: String, val price: Double)
    data class Product(val id: String, val name: String, val image: String, val archived: Boolean, val measures: List<Measure>)
}
