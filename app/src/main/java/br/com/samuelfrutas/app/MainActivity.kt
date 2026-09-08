package br.com.samuelfrutas.app

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
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
import java.net.HttpURLConnection
import java.net.URL
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
    private var productsListener: ListenerRegistration? = null

    private val green = Color.rgb(16, 142, 76)
    private val greenDark = Color.rgb(10, 104, 55)
    private val bg = Color.rgb(246, 248, 247)
    private val dark = Color.rgb(28, 35, 32)
    private val muted = Color.rgb(103, 113, 108)
    private val border = Color.rgb(224, 230, 226)
    private val white = Color.WHITE
    private val units = listOf("Un", "Lote", "Duplo", "Dz", "1/8", "1/4", "Bdj", "Cx", "1/2", "GF", "Inteiro")

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
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
        fitsSystemWindows = true
    }

    private fun tv(text: String, size: Float = 16f, bold: Boolean = false, color: Int = dark) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setPadding(0, dp(4), 0, dp(4))
    }

    private fun rounded(color: Int, radiusDp: Int = 18, strokeColor: Int? = null): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
        if (strokeColor != null) setStroke(dp(1), strokeColor)
    }

    private fun button(text: String, primary: Boolean = false) = Button(this).apply {
        this.text = text
        isAllCaps = false
        textSize = 14f
        minHeight = dp(52)
        minimumHeight = dp(52)
        minWidth = 0
        minimumWidth = 0
        stateListAnimator = null
        background = rounded(if (primary) green else white, 14, if (primary) null else border)
        setTextColor(if (primary) white else dark)
        setPadding(dp(14), 0, dp(14), 0)
        gravity = Gravity.CENTER
    }

    private fun field(hintText: String, value: String = "", input: Int = InputType.TYPE_CLASS_TEXT): EditText = EditText(this).apply {
        hint = hintText
        setText(value)
        inputType = input
        singleLine = true
        minHeight = dp(54)
        background = rounded(white, 14, border)
        setPadding(dp(15), 0, dp(15), 0)
        textSize = 15f
        setTextColor(dark)
        setHintTextColor(muted)
    }

    private fun showLogin() {
        siteListener?.remove()
        productsListener?.remove()
        root = base()
        val scroll = ScrollView(this).apply { isFillViewport = true; clipToPadding = false }
        val login = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setPadding(dp(24), dp(54), dp(24), dp(32))
        }
        val brand = tv("Samuel Frutas", 30f, true, greenDark).apply { gravity = Gravity.CENTER }
        login.addView(brand, LinearLayout.LayoutParams(-1, -2))
        val subtitle = tv("Painel administrativo do Sistema de Pedidos", 15f, false, muted).apply { gravity = Gravity.CENTER }
        login.addView(subtitle, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(28) })

        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(white, 24, border)
            setPadding(dp(20), dp(22), dp(20), dp(22))
            elevation = dp(3).toFloat()
        }
        card.addView(tv("Acesso administrativo", 19f, true), lpField())
        val email = field("E-mail", input = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS)
        val password = field("Senha", input = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
        password.keyListener = android.text.method.DigitsKeyListener.getInstance("0123456789")
        card.addView(email, lpField(dp(12)))
        card.addView(password, lpField(dp(10)))
        val message = tv("", 14f, false, Color.rgb(190, 45, 45))
        val enter = button("Entrar", true)
        card.addView(enter, lpField(dp(16)))
        card.addView(message, lpField(dp(6)))
        login.addView(card, LinearLayout.LayoutParams(-1, -2))
        scroll.addView(login, ViewGroup.LayoutParams(-1, -2))
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
        productsListener?.remove()
        root = base()

        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(16), dp(14), dp(16), dp(12))
            background = rounded(white, 0)
            elevation = dp(2).toFloat()
        }
        val top = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
        val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        titleBox.addView(tv("Samuel Frutas", 13f, true, green))
        titleBox.addView(tv("Painel de Pedidos", 23f, true))
        top.addView(titleBox, LinearLayout.LayoutParams(0, -2, 1f))
        val logout = button("Sair")
        top.addView(logout, LinearLayout.LayoutParams(dp(76), dp(52)))
        header.addView(top)
        header.addView(tv(userEmail, 12f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(2) })

        val status = button("Site: ONLINE", true)
        status.textSize = 13f
        header.addView(status, LinearLayout.LayoutParams(-1, dp(52)).apply { topMargin = dp(12) })
        root.addView(header)

        val tabs = LinearLayout(this).apply { setPadding(dp(14), dp(12), dp(14), dp(8)) }
        val active = button("Ativos", true)
        val archivedButton = button("Arquivados")
        tabs.addView(active, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(5) })
        tabs.addView(archivedButton, LinearLayout.LayoutParams(0, dp(52), 1f).apply { leftMargin = dp(5) })
        root.addView(tabs)

        val searchRow = LinearLayout(this).apply {
            background = rounded(white, 18, border)
            gravity = Gravity.CENTER_VERTICAL
            setPadding(dp(12), 0, dp(7), 0)
            elevation = dp(1).toFloat()
        }
        val icon = tv("⌕", 26f, true, green).apply { gravity = Gravity.CENTER }
        searchRow.addView(icon, LinearLayout.LayoutParams(dp(34), dp(52)))
        search = field("Pesquisar produto...", input = InputType.TYPE_CLASS_TEXT)
        search.background = null
        search.minHeight = dp(52)
        search.setPadding(dp(4), 0, dp(4), 0)
        searchRow.addView(search, LinearLayout.LayoutParams(0, dp(52), 1f))
        val clear = button("Limpar")
        clear.textSize = 13f
        clear.setPadding(dp(10), 0, dp(10), 0)
        searchRow.addView(clear, LinearLayout.LayoutParams(dp(76), dp(44)))
        root.addView(searchRow, LinearLayout.LayoutParams(-1, dp(58)).apply { setMargins(dp(14), dp(4), dp(14), dp(8)) })

        val actions = LinearLayout(this).apply { setPadding(dp(14), 0, dp(14), dp(10)) }
        val add = button("+ Adicionar produto", true)
        val reload = button("Atualizar")
        actions.addView(add, LinearLayout.LayoutParams(0, dp(52), 1f).apply { rightMargin = dp(5) })
        actions.addView(reload, LinearLayout.LayoutParams(0, dp(52), .55f).apply { leftMargin = dp(5) })
        root.addView(actions)

        val scroll = ScrollView(this).apply { clipToPadding = false; isFillViewport = false }
        content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(14), dp(2), dp(14), dp(32)) }
        scroll.addView(content, ViewGroup.LayoutParams(-1, -2))
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
        active.setOnClickListener { archived = false; active.background = rounded(green, 14); active.setTextColor(white); archivedButton.background = rounded(white, 14, border); archivedButton.setTextColor(dark); loadProducts() }
        archivedButton.setOnClickListener { archived = true; archivedButton.background = rounded(green, 14); archivedButton.setTextColor(white); active.background = rounded(white, 14, border); active.setTextColor(dark); loadProducts() }
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
            status.background = rounded(if (siteOffline) Color.rgb(238, 239, 239) else green, 14)
            status.setTextColor(if (siteOffline) dark else white)
        }
    }

    private fun loadProducts() {
        productsListener?.remove()
        productsListener = db.collection("products").orderBy("name", Query.Direction.ASCENDING)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Toast.makeText(this, "Não foi possível sincronizar produtos. Tente Atualizar.", Toast.LENGTH_LONG).show()
                    return@addSnapshotListener
                }
                products = (snap?.documents ?: emptyList()).map { d ->
                    val measures = (d.get("measures") as? List<*>)?.mapNotNull { raw ->
                        val map = raw as? Map<*, *> ?: return@mapNotNull null
                        Measure((map["quantity"] as? Number)?.toInt() ?: 1, map["unit"]?.toString() ?: "Un", (map["price"] as? Number)?.toDouble() ?: 0.0)
                    } ?: emptyList()
                    Product(d.id, d.getString("name").orEmpty(), d.getString("image").orEmpty(), d.getBoolean("archived") == true, measures)
                }.filter { it.archived == archived }.toMutableList()
                renderProducts()
            }
    }

    private fun renderProducts() {
        if (!::content.isInitialized) return
        content.removeAllViews()
        val query = if (::search.isInitialized) search.text.toString().trim().lowercase(Locale.getDefault()) else ""
        val visible = products.filter { query.isBlank() || it.name.lowercase(Locale.getDefault()).contains(query) }
        if (visible.isEmpty()) {
            val empty = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER; setPadding(dp(20), dp(60), dp(20), dp(60)) }
            empty.addView(tv(if (query.isBlank()) "Nenhum produto nesta lista." else "Nenhum produto encontrado.", 16f, true).apply { gravity = Gravity.CENTER })
            if (query.isNotBlank()) empty.addView(tv("Tente outro nome ou palavra-chave.", 13f, false, muted).apply { gravity = Gravity.CENTER })
            content.addView(empty)
            return
        }
        visible.forEach { p ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = rounded(white, 20, border)
                setPadding(dp(16), dp(14), dp(16), dp(14))
                elevation = dp(1).toFloat()
            }
            val nameRow = LinearLayout(this).apply { gravity = Gravity.CENTER_VERTICAL }
            nameRow.addView(tv(p.name, 18f, true), LinearLayout.LayoutParams(0, -2, 1f))
            nameRow.addView(tv(if (p.measures.isEmpty()) "" else "${p.measures.size}ª opção", 12f, false, muted))
            card.addView(nameRow)
            card.addView(tv(if (p.measures.isEmpty()) "Sem medidas cadastradas" else p.measures.joinToString("  •  ") { "${it.quantity} ${it.unit} — ${money(it.price)}" }, 14f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { topMargin = dp(6) })
            val actions = LinearLayout(this).apply { setPadding(0, dp(10), 0, 0) }
            val edit = button("Editar")
            actions.addView(edit, LinearLayout.LayoutParams(0, dp(50), 1f).apply { rightMargin = dp(4) })
            edit.setOnClickListener { showProductDialog(p) }
            if (archived) {
                val restore = button("Desarquivar")
                val delete = button("Excluir")
                actions.addView(restore, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4); rightMargin = dp(4) })
                actions.addView(delete, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4) })
                restore.setOnClickListener { setArchived(p, false) }
                delete.setOnClickListener { confirmDelete(p) }
            } else {
                val archive = button("Arquivar")
                actions.addView(archive, LinearLayout.LayoutParams(0, dp(50), 1f).apply { leftMargin = dp(4) })
                archive.setOnClickListener { setArchived(p, true) }
            }
            card.addView(actions)
            content.addView(card, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(10) })
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
        val scroll = ScrollView(this).apply {
            isFillViewport = false
            clipToPadding = false
            overScrollMode = View.OVER_SCROLL_IF_CONTENT_SCROLLS
            setPadding(0, 0, 0, dp(8))
        }
        val box = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(4), dp(22), dp(8))
        }

        val name = field("Nome do produto", product?.name.orEmpty())
        name.maxLines = 2
        name.ellipsize = null
        box.addView(tv("Nome do produto", 13f, true, muted), lpLabel())
        box.addView(name, lpField())

        val image = field("Imagem (URL)", product?.image.orEmpty(), InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI)
        box.addView(tv("Imagem", 13f, true, muted), lpLabel(dp(14)))
        box.addView(image, lpField())
        val googleImages = button("🔎 Pesquisar imagem no Google")
        box.addView(googleImages, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(8) })
        googleImages.setOnClickListener {
            val productName = name.text.toString().trim()
            if (productName.isBlank()) {
                name.error = "Informe o nome do produto primeiro"
                name.requestFocus()
            } else {
                val query = Uri.encode(productName)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?tbm=isch&q=$query"))
                try { startActivity(intent) } catch (_: Exception) {
                    Toast.makeText(this, "Não foi possível abrir o Google Imagens.", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val preview = ImageView(this).apply {
            setBackgroundColor(bg)
            scaleType = ImageView.ScaleType.FIT_CENTER
            adjustViewBounds = true
            visibility = if (image.text.toString().trim().isBlank()) View.GONE else View.VISIBLE
            contentDescription = "Prévia da imagem do produto"
        }
        box.addView(preview, LinearLayout.LayoutParams(-1, dp(150)).apply { topMargin = dp(8) })
        loadImagePreview(image.text.toString(), preview)
        image.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val url = s?.toString()?.trim().orEmpty()
                preview.visibility = if (url.isBlank()) View.GONE else View.VISIBLE
                if (url.isNotBlank()) loadImagePreview(url, preview)
            }
            override fun afterTextChanged(s: Editable?) = Unit
        })

        box.addView(tv("Medidas e preços", 16f, true), lpLabel(dp(18)))
        box.addView(tv("Cadastre cada opção de venda com quantidade, unidade e preço.", 12f, false, muted), LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
        val rows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        box.addView(rows, LinearLayout.LayoutParams(-1, -2))
        val initialMeasures = product?.measures?.ifEmpty { listOf(Measure(1, "Un", 0.0)) } ?: listOf(Measure(1, "Un", 0.0))
        initialMeasures.forEach { addMeasureRow(rows, it) }

        val addMeasure = button("+ Adicionar medida")
        box.addView(addMeasure, LinearLayout.LayoutParams(-1, dp(50)).apply { topMargin = dp(12) })
        addMeasure.setOnClickListener { addMeasureRow(rows, Measure(1, "Un", 0.0)); scroll.post { scroll.fullScroll(View.FOCUS_DOWN) } }
        scroll.addView(box, ViewGroup.LayoutParams(-1, -2))

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (product == null) "Novo produto" else "Editar produto")
            .setView(scroll)
            .setNegativeButton("Cancelar", null)
            .setPositiveButton("Salvar", null)
            .create()

        dialog.setOnShowListener {
            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).minHeight = dp(50)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).minHeight = dp(50)
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setPadding(dp(14), 0, dp(14), 0)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setPadding(dp(14), 0, dp(14), 0)
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val n = name.text.toString().trim()
                val measures = mutableListOf<Measure>()
                for (i in 0 until rows.childCount) {
                    val row = rows.getChildAt(i) as? LinearLayout ?: continue
                    if (row.childCount < 4) continue
                    val qty = (row.getChildAt(0) as EditText).text.toString().toIntOrNull() ?: 1
                    val unit = (row.getChildAt(1) as Spinner).selectedItem.toString()
                    val price = (row.getChildAt(2) as EditText).text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0
                    measures.add(Measure(qty.coerceAtLeast(1), unit, price))
                }
                if (n.isBlank()) { name.error = "Informe o nome"; name.requestFocus(); return@setOnClickListener }
                if (measures.isEmpty()) { Toast.makeText(this, "Adicione pelo menos uma medida.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (measures.any { it.quantity < 1 }) { Toast.makeText(this, "A quantidade deve ser maior que zero.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (measures.any { !it.price.isFinite() || it.price <= 0.0 }) { Toast.makeText(this, "Informe um preço válido e maior que zero em todas as opções.", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                val imageUrl = image.text.toString().trim()
                if (imageUrl.isNotBlank()) {
                    val parsed = try { URL(imageUrl) } catch (_: Exception) { null }
                    if (parsed == null || (parsed.protocol != "http" && parsed.protocol != "https")) {
                        image.error = "Informe uma URL http:// ou https:// válida"
                        image.requestFocus()
                        return@setOnClickListener
                    }
                }
                val data = hashMapOf<String, Any>(
                    "name" to n,
                    "category" to "produtos",
                    "measures" to measures.map { mapOf("quantity" to it.quantity, "unit" to it.unit, "price" to it.price) },
                    "unit" to measures.first().unit,
                    "price" to measures.first().price,
                    "priceTiers" to measures.filter { it.unit == measures.first().unit && it.quantity > 1 }.map { mapOf("minQty" to it.quantity, "unitPrice" to it.price) },
                    "image" to imageUrl,
                    "archived" to (product?.archived ?: false),
                    "updatedAt" to FieldValue.serverTimestamp()
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
        dialog.window?.let { window ->
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            window.setLayout((resources.displayMetrics.widthPixels * 0.94f).toInt(), (resources.displayMetrics.heightPixels * 0.88f).toInt())
        }
    }

    private fun addMeasureRow(parent: LinearLayout, measure: Measure) {
        val row = LinearLayout(this).apply {
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, dp(4), 0, dp(4))
        }
        val qty = field("Qtd", measure.quantity.toString(), InputType.TYPE_CLASS_NUMBER)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(this@MainActivity, android.R.layout.simple_spinner_dropdown_item, units)
            setSelection(units.indexOf(measure.unit).coerceAtLeast(0))
            minimumHeight = dp(54)
        }
        val price = field("Preço", if (measure.price == 0.0) "" else String.format(Locale.US, "%.2f", measure.price), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
        val remove = button("×")
        remove.textSize = 22f
        remove.contentDescription = "Remover medida"
        row.addView(qty, LinearLayout.LayoutParams(0, dp(54), .72f).apply { rightMargin = dp(5) })
        row.addView(spinner, LinearLayout.LayoutParams(0, dp(54), 1.12f).apply { leftMargin = dp(2); rightMargin = dp(5) })
        row.addView(price, LinearLayout.LayoutParams(0, dp(54), .92f).apply { leftMargin = dp(2); rightMargin = dp(5) })
        row.addView(remove, LinearLayout.LayoutParams(dp(50), dp(54)))
        remove.setOnClickListener { parent.removeView(row) }
        parent.addView(row)
    }

    private fun loadImagePreview(url: String, target: ImageView) {
        val cleanUrl = url.trim()
        if (cleanUrl.isBlank()) return
        val parsed = try { URL(cleanUrl) } catch (_: Exception) { return }
        if (parsed.protocol != "http" && parsed.protocol != "https") return

        Thread {
            var connection: HttpURLConnection? = null
            try {
                connection = parsed.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 7000
                connection.instanceFollowRedirects = true
                connection.doInput = true
                connection.connect()
                if (connection.responseCode !in 200..299) return@Thread

                connection.inputStream.use { stream ->
                    val bytes = stream.readBytes()
                    if (bytes.isEmpty() || bytes.size > 12 * 1024 * 1024) return@Thread

                    val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return@Thread

                    var sample = 1
                    while (bounds.outWidth / sample > 1200 || bounds.outHeight / sample > 1200) sample *= 2
                    val options = android.graphics.BitmapFactory.Options().apply {
                        inSampleSize = sample
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565
                    }
                    val bitmap = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)

                    runOnUiThread {
                        if (!isFinishing && !isDestroyed && bitmap != null && target.isAttachedToWindow) {
                            target.setImageBitmap(bitmap)
                            target.scaleType = ImageView.ScaleType.FIT_CENTER
                            target.visibility = View.VISIBLE
                        }
                    }
                }
            } catch (_: Exception) {
                // Invalid, unsupported or oversized remote images must never crash the app.
            } finally {
                connection?.disconnect()
            }
        }.start()
    }

    private fun lpField(top: Int = 0) = LinearLayout.LayoutParams(-1, dp(54)).apply {
        if (top > 0) topMargin = top
    }

    private fun lpLabel(top: Int = 0) = LinearLayout.LayoutParams(-1, -2).apply {
        if (top > 0) topMargin = top
        bottomMargin = dp(5)
    }

    private fun money(value: Double): String = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

    override fun onDestroy() { siteListener?.remove(); productsListener?.remove(); super.onDestroy() }

    data class Measure(val quantity: Int, val unit: String, val price: Double)
    data class Product(val id: String, val name: String, val image: String, val archived: Boolean, val measures: List<Measure>)
}
