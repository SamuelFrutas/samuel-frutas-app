package br.com.samuelfrutas.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.net.URL
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val green = Color.rgb(15, 138, 75)
    private val orange = Color.rgb(249, 115, 22)
    private val bg = Color.rgb(244, 246, 245)
    private val textColor = Color.rgb(24, 34, 48)
    private val muted = Color.rgb(102, 112, 133)
    private val red = Color.rgb(180, 35, 24)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var siteListener: ListenerRegistration? = null
    private var archived = false
    private var editingId: String? = null
    private lateinit var nameInput: EditText
    private lateinit var imageInput: EditText
    private lateinit var imagePreview: ImageView
    private lateinit var imageStatus: TextView
    private lateinit var imageWrap: LinearLayout
    private lateinit var measureRows: LinearLayout
    private lateinit var productsList: LinearLayout
    private lateinit var form: LinearLayout
    private lateinit var formTitle: TextView
    private lateinit var activeTab: MaterialButton
    private lateinit var archivedTab: MaterialButton
    private lateinit var siteToggle: MaterialButton
    private lateinit var saveButton: MaterialButton
    private lateinit var userText: TextView
    private var siteOffline = false

    data class Measure(var quantity: Int = 1, var unit: String = "Un", var price: Double = 0.0, var lotSize: Int? = null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initializeFirebase()
        if (auth.currentUser == null) showLogin() else showAdmin()
    }

    private fun initializeFirebase() {
        val options = FirebaseOptions.Builder()
            .setApiKey("AIzaSyB1E9oQsIwYO2-r4W5-uqK4ax92OmxISOI")
            .setApplicationId("1:1052699327049:web:cf22d68c76d064d56b5d98")
            .setProjectId("samuelfrutasbot")
            .setStorageBucket("samuelfrutasbot.firebasestorage.app")
            .build()
        if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun field(hint: String) = EditText(this).apply {
        this.hint = hint; textSize = 15f; setPadding(dp(12), 0, dp(12), 0)
        setBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, dp(46)).apply { bottomMargin = dp(10) }
    }

    private fun button(label: String, primary: Boolean = true) = MaterialButton(this).apply {
        text = label; isAllCaps = false; minHeight = dp(44); textSize = 14f
        if (primary) { setBackgroundColor(green); setTextColor(Color.WHITE) }
        else { setBackgroundColor(Color.WHITE); setTextColor(Color.rgb(52, 64, 84)) }
    }

    private fun card() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(18), dp(18), dp(18), dp(18)); setBackgroundColor(Color.WHITE); elevation = dp(2).toFloat()
        layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(14) }
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg); setPadding(dp(16), dp(50), dp(16), dp(20)) }
        root.addView(TextView(this).apply { text = "Samuel Frutas"; textSize = 28f; setTextColor(orange); gravity = Gravity.CENTER; setTypeface(null, 1) })
        root.addView(TextView(this).apply { text = "Painel administrativo do Sistema de Pedidos"; textSize = 14f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, dp(6), 0, dp(22)) })
        val box = card(); val email = field("E-mail"); val password = field("Senha").apply { inputType = 0x81 }; val msg = TextView(this).apply { setTextColor(red) }; val enter = button("Entrar")
        enter.setOnClickListener { enter.isEnabled = false; auth.signInWithEmailAndPassword(email.text.toString().trim(), password.text.toString()).addOnCompleteListener { task -> enter.isEnabled = true; if (task.isSuccessful) showAdmin() else msg.text = "E-mail ou senha inválidos." } }
        box.addView(email); box.addView(password); box.addView(enter); box.addView(msg); root.addView(box); setContentView(root)
    }

    private fun showAdmin() {
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(bg) }
        val header = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(dp(16), dp(14), dp(8), dp(14)); setBackgroundColor(Color.WHITE) }
        val brand = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }
        brand.addView(TextView(this).apply { text = "Painel de Pedidos"; textSize = 23f; setTextColor(textColor); setTypeface(null, 1) })
        userText = TextView(this).apply { setTextColor(muted); textSize = 12f }; brand.addView(userText); header.addView(brand)
        val store = button("Abrir loja", false); store.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://samuelfrutas.github.io/samuel-frutas/pedidos/"))) }; header.addView(store)
        siteToggle = button("Site: ONLINE"); siteToggle.setOnClickListener { toggleSite() }; header.addView(siteToggle)
        val logout = button("Sair", false); logout.setOnClickListener { auth.signOut() }; header.addView(logout); root.addView(header)
        val scroll = ScrollView(this); val content = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(10), dp(12), dp(10), dp(30)) }; scroll.addView(content); root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
        val main = card(); val tabs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, 0, 0, dp(10)) }
        activeTab = button("Ativos"); archivedTab = button("Arquivados", false); tabs.addView(activeTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(5) }); tabs.addView(archivedTab, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(5) }); main.addView(tabs)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }; val newProduct = button("+ Adicionar produto"); val reload = button("Atualizar", false); actions.addView(newProduct, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(4) }); actions.addView(reload, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(4) }); main.addView(actions)
        main.addView(buildForm()); productsList = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; main.addView(productsList); content.addView(main); setContentView(root)
        userText.text = auth.currentUser?.email ?: ""; newProduct.setOnClickListener { showForm(null) }; reload.setOnClickListener { loadProducts() }; activeTab.setOnClickListener { archived = false; updateTabs(); loadProducts() }; archivedTab.setOnClickListener { archived = true; updateTabs(); loadProducts() }; listenSiteStatus(); updateTabs(); loadProducts()
    }

    private fun updateTabs() { activeTab.setBackgroundColor(if (!archived) green else Color.WHITE); activeTab.setTextColor(if (!archived) Color.WHITE else Color.rgb(52,64,84)); archivedTab.setBackgroundColor(if (archived) green else Color.WHITE); archivedTab.setTextColor(if (archived) Color.WHITE else Color.rgb(52,64,84)) }

    private fun buildForm(): View {
        form = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE; setPadding(0, dp(16), 0, dp(4)) }
        formTitle = TextView(this).apply { text = "Novo produto"; textSize = 20f; setTextColor(textColor); setTypeface(null, 1); setPadding(0, 0, 0, dp(10)) }; form.addView(formTitle)
        nameInput = field("Nome"); imageInput = field("Imagem (URL)"); form.addView(nameInput); form.addView(imageInput)
        imageWrap = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(12), dp(10), dp(12), dp(10)); setBackgroundColor(Color.rgb(248,250,249)); visibility = View.GONE }
        imagePreview = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(180), dp(180)).apply { gravity = Gravity.CENTER }; scaleType = ImageView.ScaleType.CENTER_CROP }; imageStatus = TextView(this).apply { textSize = 12f; setTextColor(muted); gravity = Gravity.CENTER; setPadding(0, dp(6), 0, 0) }; imageWrap.addView(imagePreview); imageWrap.addView(imageStatus); form.addView(imageWrap)
        imageInput.setOnFocusChangeListener { _, has -> if (!has) updateImagePreview() }
        val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL; setPadding(0, dp(14), 0, dp(8)) }; val titleBox = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; titleBox.addView(TextView(this).apply { text = "Medidas e preços"; textSize = 15f; setTypeface(null, 1); setTextColor(textColor) }); titleBox.addView(TextView(this).apply { text = "Cadastre as opções que o cliente poderá escolher na loja."; textSize = 11f; setTextColor(muted) }); head.addView(titleBox); val add = button("+ Adicionar medida", false); add.setOnClickListener { addMeasureRow(Measure()) }; head.addView(add); form.addView(head)
        measureRows = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }; form.addView(measureRows)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(10), 0, 0) }; saveButton = button("Salvar"); val cancel = button("Cancelar", false); cancel.setOnClickListener { hideForm() }; actions.addView(saveButton, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(4) }); actions.addView(cancel, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(4) }); form.addView(actions); saveButton.setOnClickListener { saveProduct() }; return form
    }

    private fun showForm(product: Map<String, Any>?) {
        editingId = product?.get("id")?.toString(); form.visibility = View.VISIBLE; formTitle.text = if (product == null) "Novo produto" else "Editar produto"; nameInput.setText(product?.get("name")?.toString() ?: ""); imageInput.setText(product?.get("image")?.toString() ?: ""); measureRows.removeAllViews()
        val list = product?.get("measures") as? List<*>; if (list.isNullOrEmpty()) addMeasureRow(Measure(1, product?.get("unit")?.toString() ?: "Un", (product?.get("price") as? Number)?.toDouble() ?: 0.0)) else list.forEach { m -> val map = m as? Map<*, *> ?: return@forEach; addMeasureRow(Measure((map["quantity"] as? Number)?.toInt() ?: 1, map["unit"]?.toString() ?: "Un", (map["price"] as? Number)?.toDouble() ?: 0.0, (map["lotSize"] as? Number)?.toInt())) }; updateImagePreview(); form.postDelayed({ nameInput.requestFocus() }, 100)
    }

    private fun hideForm() { editingId = null; form.visibility = View.GONE; nameInput.setText(""); imageInput.setText(""); measureRows.removeAllViews(); imageWrap.visibility = View.GONE }

    private fun addMeasureRow(measure: Measure) {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.BOTTOM; setPadding(dp(8), dp(8), dp(8), dp(8)); setBackgroundColor(Color.WHITE); elevation = dp(1).toFloat() }
        val qty = field("Qtd").apply { setText(measure.quantity.toString()); inputType = 2 }; val unit = Spinner(this); val units = listOf("Un", "Lote", "Duplo", "Dz", "1/8", "1/4", "Bdj", "Cx", "1/2", "GF", "Inteiro"); unit.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, units); unit.setSelection(units.indexOf(measure.unit).coerceAtLeast(0)); val price = field("Preço").apply { setText(if (measure.price == 0.0) "" else String.format(Locale.US, "%.2f", measure.price)); inputType = 8194 }; val remove = button("×", false); remove.setTextColor(red); remove.setOnClickListener { measureRows.removeView(row) }
        row.addView(qty, LinearLayout.LayoutParams(0, dp(46), 1f).apply { rightMargin = dp(4) }); row.addView(unit, LinearLayout.LayoutParams(0, dp(46), 1.1f).apply { leftMargin = dp(2); rightMargin = dp(4) }); row.addView(price, LinearLayout.LayoutParams(0, dp(46), 1f).apply { leftMargin = dp(2); rightMargin = dp(4) }); row.addView(remove, LinearLayout.LayoutParams(dp(44), dp(46))); measureRows.addView(row, LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(8) })
    }

    private fun readMeasures(): List<Measure> { val out = mutableListOf<Measure>(); for (i in 0 until measureRows.childCount) { val row = measureRows.getChildAt(i) as LinearLayout; val qty = (row.getChildAt(0) as EditText).text.toString().toIntOrNull()?.coerceAtLeast(1) ?: 1; val unit = (row.getChildAt(1) as Spinner).selectedItem.toString(); val price = (row.getChildAt(2) as EditText).text.toString().replace(',', '.').toDoubleOrNull() ?: 0.0; out.add(Measure(qty, unit, price, if (unit == "Lote") qty else null)) }; return out.filter { it.price >= 0 } }

    private fun saveProduct() {
        val name = nameInput.text.toString().trim(); if (name.isEmpty()) { nameInput.error = "Informe o nome do produto."; return }; val measures = readMeasures(); if (measures.none { it.price > 0 }) { Toast.makeText(this, "Adicione pelo menos uma medida com preço.", Toast.LENGTH_LONG).show(); return }
        val measureMaps = measures.map { m -> hashMapOf<String, Any>("quantity" to m.quantity, "unit" to m.unit, "price" to m.price).apply { m.lotSize?.let { put("lotSize", it) } } }; val first = measures.first(); val tiers = measures.filter { it.unit == first.unit && it.quantity > 1 }.map { hashMapOf<String, Any>("minQty" to it.quantity, "unitPrice" to it.price) }
        val data = hashMapOf<String, Any>("name" to name, "category" to "produtos", "measures" to measureMaps, "unit" to first.unit, "price" to first.price, "priceTiers" to tiers, "image" to imageInput.text.toString().trim(), "archived" to archived, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp())
        val id = editingId; saveButton.isEnabled = false; val task = if (id == null) db.collection("products").add(data) else db.collection("products").document(id).set(data, SetOptions.merge()); task.addOnCompleteListener { saveButton.isEnabled = true; if (it.isSuccessful) { hideForm(); loadProducts() } else Toast.makeText(this, "Não foi possível salvar o produto.", Toast.LENGTH_LONG).show() }
    }

    private fun loadProducts() { db.collection("products").get().addOnSuccessListener { snap -> productsList.removeAllViews(); val docs = snap.documents.mapNotNull { d -> if ((d.getBoolean("archived") ?: false) == archived) d.data?.toMutableMap()?.apply { put("id", d.id) } else null }.sortedBy { it["name"]?.toString()?.lowercase(Locale("pt", "BR")) ?: "" }; if (docs.isEmpty()) productsList.addView(TextView(this).apply { text = "Nenhum produto nesta lista."; setTextColor(muted); setPadding(0, dp(20), 0, dp(20)) }) else docs.forEach { addProductRow(it) } }.addOnFailureListener { Toast.makeText(this, "Não foi possível carregar os produtos.", Toast.LENGTH_LONG).show() } }

    private fun addProductRow(p: Map<String, Any>) {
        val id = p["id"]?.toString() ?: return; val box = MaterialCardView(this).apply { radius = dp(13).toFloat(); cardElevation = dp(2).toFloat(); setCardBackgroundColor(Color.WHITE); layoutParams = LinearLayout.LayoutParams(-1, -2).apply { bottomMargin = dp(9) } }; val row = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(dp(13), dp(13), dp(13), dp(13)) }; val head = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }; val image = ImageView(this).apply { layoutParams = LinearLayout.LayoutParams(dp(60), dp(60)).apply { rightMargin = dp(12) }; scaleType = ImageView.ScaleType.CENTER_CROP; setBackgroundColor(Color.LTGRAY) }; val url = p["image"]?.toString().orEmpty(); if (url.isNotEmpty()) loadUrlImage(url, image); head.addView(image)
        val info = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; layoutParams = LinearLayout.LayoutParams(0, -2, 1f) }; info.addView(TextView(this).apply { text = p["name"]?.toString() ?: ""; textSize = 17f; setTextColor(textColor); setTypeface(null, 1) }); val measures = (p["measures"] as? List<*>)?.mapNotNull { m -> val x = m as? Map<*, *> ?: return@mapNotNull null; "${x["quantity"] ?: 1} ${x["unit"] ?: "Un"} — R$ ${String.format(Locale("pt", "BR"), "%.2f", (x["price"] as? Number)?.toDouble() ?: 0.0)}" }?.joinToString(" | ") ?: "Sem medidas"; info.addView(TextView(this).apply { text = measures; textSize = 12f; setTextColor(muted); setPadding(0, dp(4), 0, 0) }); head.addView(info); row.addView(head)
        val actions = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; setPadding(0, dp(9), 0, 0) }; val edit = button("Editar", false); edit.setOnClickListener { showForm(p) }; actions.addView(edit, LinearLayout.LayoutParams(0, dp(44), 1f).apply { rightMargin = dp(4) }); val action = if (archived) button("Desarquivar") else button("Arquivar", false); action.setOnClickListener { db.collection("products").document(id).update("archived", !archived).addOnSuccessListener { loadProducts() } }; actions.addView(action, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(4) }); if (archived) { val del = button("Excluir", false); del.setTextColor(red); del.setOnClickListener { db.collection("products").document(id).delete().addOnSuccessListener { loadProducts() } }; actions.addView(del, LinearLayout.LayoutParams(0, dp(44), 1f).apply { leftMargin = dp(8) }) }; row.addView(actions); box.addView(row); productsList.addView(box)
    }

    private fun loadUrlImage(url: String, target: ImageView) { Thread { try { val bitmap = BitmapFactory.decodeStream(URL(url).openStream()); runOnUiThread { target.setImageBitmap(bitmap) } } catch (_: Exception) {} }.start() }
    private fun updateImagePreview() { val url = imageInput.text.toString().trim(); if (url.isEmpty()) { imageWrap.visibility = View.GONE; return }; imageWrap.visibility = View.VISIBLE; imageStatus.text = "Carregando pré-visualização..."; loadUrlImage(url, imagePreview); imageStatus.text = "Pré-visualização da imagem" }
    private fun listenSiteStatus() { siteListener?.remove(); siteListener = db.collection("config").document("bot").addSnapshotListener { snap, _ -> siteOffline = snap?.getBoolean("siteOffline") ?: false; renderSiteToggle() } }
    private fun renderSiteToggle() { siteToggle.text = if (siteOffline) "Site: FORA DO AR" else "Site: ONLINE"; siteToggle.setBackgroundColor(if (siteOffline) red else green); siteToggle.setTextColor(Color.WHITE) }
    private fun toggleSite() { val next = !siteOffline; db.collection("config").document("bot").set(mapOf("siteOffline" to next, "updatedAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()), SetOptions.merge()).addOnFailureListener { Toast.makeText(this, "Não foi possível alterar o status do site.", Toast.LENGTH_LONG).show() } }
    override fun onDestroy() { siteListener?.remove(); super.onDestroy() }
}
