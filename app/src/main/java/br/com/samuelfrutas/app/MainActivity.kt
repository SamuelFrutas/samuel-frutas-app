package br.com.samuelfrutas.app

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class MainActivity : AppCompatActivity() {
    private val green = Color.rgb(46,125,50)
    private val darkGreen = Color.rgb(27,94,32)
    private val lightGreen = Color.rgb(232,245,233)
    private val border = Color.rgb(223,229,223)
    private val secondary = Color.rgb(96,125,139)
    private val red = Color.rgb(211,47,47)
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var listener: ListenerRegistration? = null
    private var editingId: String? = null
    private var imageUri: Uri? = null
    private var existingImage = ""
    private lateinit var listContainer: LinearLayout
    private lateinit var nameInput: EditText
    private lateinit var category: Spinner
    private lateinit var active: CheckBox
    private lateinit var unitChecks: List<CheckBox>
    private lateinit var loteQty: EditText
    private lateinit var loteBox: LinearLayout
    private lateinit var imagePreview: ImageView
    private lateinit var saveButton: MaterialButton
    private lateinit var cancelButton: MaterialButton
    private val gallery = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { imageUri = uri; imagePreview.visibility = View.VISIBLE; imagePreview.setImageURI(uri) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initFirebase()
        if (auth.currentUser == null) showLogin() else showAdmin()
    }

    private fun initFirebase() {
        val options = FirebaseOptions.Builder()
            .setApiKey("AIzaSyBLU3UNXuPGUFrpmV6syI80ynUHppupeNA")
            .setApplicationId("1:475005081261:android:314ac91f8b0578b995824")
            .setProjectId("samuel-frutas")
            .setStorageBucket("samuel-frutas.firebasestorage.app")
            .build()
        if (FirebaseApp.getApps(this).isEmpty()) FirebaseApp.initializeApp(this, options)
        auth = FirebaseAuth.getInstance(); db = FirebaseFirestore.getInstance(); storage = FirebaseStorage.getInstance()
    }

    private fun showLogin() {
        val root = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(24,60,24,24); setBackgroundColor(Color.rgb(244,246,244)) }
        val title = TextView(this).apply { text="Samuel Frutas"; textSize=28f; setTextColor(green); gravity=Gravity.CENTER; setTypeface(null,1) }
        val sub = TextView(this).apply { text="Painel de Pedidos"; textSize=16f; setTextColor(secondary); gravity=Gravity.CENTER; setPadding(0,6,0,30) }
        val card = MaterialCardView(this).apply { radius=24f; cardElevation=4f; setCardBackgroundColor(Color.WHITE) }
        val box=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,22,20,20)}
        val email=field("E-mail", "admin@exemplo.com"); val pass=field("Senha", "", true)
        val button=MaterialButton(this).apply{text="Entrar";setTextColor(Color.WHITE);setBackgroundColor(green);minimumHeight=52}
        val msg=TextView(this).apply{setTextColor(red);setPadding(0,12,0,0);visibility=View.GONE}
        button.setOnClickListener{button.isEnabled=false;auth.signInWithEmailAndPassword(email.text.toString().trim(),pass.text.toString()).addOnCompleteListener{t->button.isEnabled=true;if(t.isSuccessful)showAdmin()else{msg.text="E-mail ou senha inválidos.";msg.visibility=View.VISIBLE}}}
        box.addView(email);box.addView(pass);box.addView(button);box.addView(msg);card.addView(box)
        root.addView(title);root.addView(sub);root.addView(card,LinearLayout.LayoutParams(-1,-2));setContentView(root)
    }

    private fun field(label:String,hint:String,password:Boolean=false):EditText{
        val e=EditText(this);e.hint=hint;e.setTextSize(15f);e.setPadding(14,0,14,0);e.backgroundTintList=android.content.res.ColorStateList.valueOf(border);e.layoutParams=LinearLayout.LayoutParams(-1,56).apply{bottomMargin=14};if(password)e.inputType=0x81;return e
    }

    private fun showAdmin(){
        val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setBackgroundColor(Color.rgb(244,246,244))}
        val header=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL;gravity=Gravity.CENTER_VERTICAL;setPadding(16,16,12,16);setBackgroundColor(green)}
        val brand=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;layoutParams=LinearLayout.LayoutParams(0,-2,1f)}
        brand.addView(TextView(this).apply{text="Samuel Frutas";textSize=21f;setTextColor(Color.WHITE);setTypeface(null,1)})
        brand.addView(TextView(this).apply{text="Painel de administração";textSize=12f;setTextColor(Color.WHITE)})
        val store=MaterialButton(this).apply{text="🛒 Ver loja";textSize=11f;setTextColor(Color.WHITE);setBackgroundColor(Color.TRANSPARENT);setOnClickListener{startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://samuelfrutas.github.io/samuel-frutas/")))}}
        header.addView(brand);header.addView(store);root.addView(header)
        val scroll=ScrollView(this);val content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,18,12,40)};scroll.addView(content);root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))
        val title=TextView(this).apply{text="Cadastro de Produtos";textSize=21f;setTextColor(green);setTypeface(null,1)};content.addView(title);content.addView(TextView(this).apply{text="Cadastre, edite, ative ou desative os produtos da loja.";textSize=12f;setTextColor(secondary);setPadding(0,5,0,18)})
        content.addView(buildFormCard());content.addView(buildHelpCard());content.addView(buildProductsCard())
        setContentView(root);listenProducts()
    }

    private fun card():LinearLayout{val c=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(16,16,16,16);setBackgroundColor(Color.WHITE);elevation=5f};val lp=LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=18;c.layoutParams=lp;return c}
    private fun title(text:String)=TextView(this).apply{text=text;textSize=16f;setTextColor(green);setTypeface(null,1);setPadding(0,0,0,14)}
    private fun buildFormCard():View{
        val c=card();c.addView(title("Cadastro / Edição de Produtos"));nameInput=field("Nome do produto","Ex.: Mamão Papaya");c.addView(nameInput)
        c.addView(TextView(this).apply{text="Categoria";textSize=12f;setTypeface(null,1);setTextColor(Color.DKGRAY)});category=Spinner(this);category.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,listOf("Frutas","Legumes","Verduras","Água de coco e ovos caipira"));c.addView(category,LinearLayout.LayoutParams(-1,52).apply{bottomMargin=14})
        c.addView(TextView(this).apply{text="Forma de venda";textSize=12f;setTypeface(null,1);setTextColor(Color.DKGRAY);setPadding(0,0,0,8)})
        val grid=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};val labels=listOf("🧺 Unidade","⚖️ Quilo","🥬 Maço","🥚 Dúzia","📦 Lote","🧺 Bandeja (BDJ)","¼","⅛","½ Metade");unitChecks=labels.mapIndexed{_,s->CheckBox(this).apply{text=s;buttonTintList=android.content.res.ColorStateList.valueOf(green);setTextSize(12f)}};for(i in unitChecks.indices step 2){val row=LinearLayout(this);row.addView(unitChecks[i],LinearLayout.LayoutParams(0,52,1f));if(i+1<unitChecks.size)row.addView(unitChecks[i+1],LinearLayout.LayoutParams(0,52,1f));grid.addView(row)};c.addView(grid)
        loteBox=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;visibility=View.GONE;setPadding(12,8,12,8);setBackgroundColor(Color.rgb(248,250,248))};loteQty=field("Quantidade de unidades por lote","Ex.: 3");loteBox.addView(loteQty);c.addView(loteBox);unitChecks[4].setOnCheckedChangeListener{_,checked->loteBox.visibility=if(checked)View.VISIBLE else View.GONE}
        c.addView(TextView(this).apply{text="Imagem do produto";textSize=12f;setTypeface(null,1);setPadding(0,12,0,8)});val pick=MaterialButton(this).apply{text="📷 Escolher imagem da galeria";setOnClickListener{gallery.launch("image/*")}};c.addView(pick);imagePreview=ImageView(this).apply{visibility=View.GONE;layoutParams=LinearLayout.LayoutParams(110,110).apply{gravity=Gravity.CENTER_HORIZONTAL;topMargin=8;bottomMargin=8};scaleType=ImageView.ScaleType.CENTER_CROP};c.addView(imagePreview)
        active=CheckBox(this).apply{text="Produto ativo na loja";isChecked=true;buttonTintList=android.content.res.ColorStateList.valueOf(green)};c.addView(active)
        val buttons=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};saveButton=MaterialButton(this).apply{text="Salvar Produto";setTextColor(Color.WHITE);setBackgroundColor(green)};cancelButton=MaterialButton(this).apply{text="Cancelar edição";visibility=View.GONE;setOnClickListener{resetForm()}};buttons.addView(saveButton);buttons.addView(cancelButton);c.addView(buttons);saveButton.setOnClickListener{saveProduct()};return c
    }

    private fun buildHelpCard():View{val c=card();c.addView(title("📋 Como funciona"));c.addView(TextView(this).apply{text="1. Cadastre o produto informando nome e categoria.\n\n2. Escolha as formas de venda que o cliente poderá selecionar.\n\n3. Lote permite definir quantas unidades existem em cada lote.\n\n4. Produto inativo permanece cadastrado, mas não aparece para o cliente.";textSize=13f;setTextColor(secondary);setLineSpacing(3f,1f)});return c}

    private fun buildProductsCard():View{val c=card();c.addView(title("📦 Produtos cadastrados"));listContainer=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL};c.addView(listContainer);return c}

    private fun listenProducts(){listener?.remove();listener=db.collection("produtos").addSnapshotListener{snap,err->if(err!=null)return@addSnapshotListener;listContainer.removeAllViews();if(snap==null||snap.isEmpty){listContainer.addView(TextView(this).apply{text="Nenhum produto cadastrado.";setTextColor(secondary);setPadding(0,20,0,20)});return@addSnapshotListener};for(d in snap.documents)addProductRow(d.id,d.data?:emptyMap())}}

    private fun addProductRow(id:String,p:Map<String,Any>){val box=MaterialCardView(this).apply{radius=16f;cardElevation=2f;setCardBackgroundColor(Color.WHITE)};val row=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(12,12,12,12)};val name=TextView(this).apply{text=p["nome"]?.toString()? : "";textSize=16f;setTypeface(null,1);setTextColor(Color.DKGRAY)};row.addView(name);val cat=p["categoria"]?.toString()? : "frutas";val u=p["unidadesMedida"] as? Map<*,*>;val units=mutableListOf<String>();if(u?.get("unidade")==true)units.add("Unidade");if(u?.get("quilo")==true)units.add("Quilo");if(u?.get("maco")==true)units.add("Maço");if(u?.get("duzia")==true)units.add("Dúzia");if(u?.get("bdj")==true)units.add("BDJ");if(u?.get("umQuarto")==true)units.add("1/4");if(u?.get("umOitavo")==true)units.add("1/8");if(u?.get("metade")==true)units.add("Metade");if(u?.get("lote")==true)units.add("Lote C/${u["quantidadePorLote"]?:"?"} un.");row.addView(TextView(this).apply{text="${cat.uppercase()} • ${if(units.isEmpty())"-" else units.joinToString(" • ")}";textSize=11f;setTextColor(secondary);setPadding(0,5,0,5)});val status=if(p["ativo"]==false)"Inativo" else "Ativo";row.addView(TextView(this).apply{text=status;setTextColor(if(status=="Ativo")green else red);setTypeface(null,1);textSize=12f});val actions=LinearLayout(this).apply{orientation=LinearLayout.HORIZONTAL};val edit=MaterialButton(this).apply{text="✏️ Editar";textSize=11f;setOnClickListener{fillForm(id,p)}};val del=MaterialButton(this).apply{text="🗑️ Excluir";textSize=11f;setTextColor(red);setOnClickListener{deleteProduct(id)}};actions.addView(edit,LinearLayout.LayoutParams(0,48,1f));actions.addView(del,LinearLayout.LayoutParams(0,48,1f));row.addView(actions);box.addView(row);listContainer.addView(box,LinearLayout.LayoutParams(-1,-2).apply{bottomMargin=10})}

    private fun saveProduct(){val name=nameInput.text.toString().trim();if(name.isEmpty()){nameInput.error="Informe o nome";return};val units=mapOf("unidade" to unitChecks[0].isChecked,"quilo" to unitChecks[1].isChecked,"maco" to unitChecks[2].isChecked,"duzia" to unitChecks[3].isChecked,"lote" to unitChecks[4].isChecked,"bdj" to unitChecks[5].isChecked,"umQuarto" to unitChecks[6].isChecked,"umOitavo" to unitChecks[7].isChecked,"metade" to unitChecks[8].isChecked,"quantidadePorLote" to (loteQty.text.toString().toIntOrNull()?:0));val cats=listOf("frutas","legumes","verduras","aguaOvos");val data=hashMapOf<String,Any>("nome" to name,"categoria" to cats[category.selectedItemPosition],"unidadesMedida" to units,"ativo" to active.isChecked);saveButton.isEnabled=false;val id=editingId?:UUID.randomUUID().toString();fun write(url:String){if(url.isNotEmpty())data["imagemUrl"]=url;db.collection("produtos").document(id).set(data,SetOptions.merge()).addOnCompleteListener{saveButton.isEnabled=true;if(it.isSuccessful)resetForm()else Toast.makeText(this,"Erro ao salvar produto",Toast.LENGTH_LONG).show()}};if(imageUri!=null){val ref=storage.reference.child("produtos/$id-${UUID.randomUUID()}.jpg");ref.putFile(imageUri!!).continueWithTask{ref.downloadUrl}.addOnSuccessListener{write(it.toString())}.addOnFailureListener{saveButton.isEnabled=true;Toast.makeText(this,"Erro no upload da imagem",Toast.LENGTH_LONG).show()}}else write(existingImage)}

    private fun fillForm(id:String,p:Map<String,Any>){editingId=id;nameInput.setText(p["nome"]?.toString()? : "");val cats=listOf("frutas","legumes","verduras","aguaOvos");category.setSelection(cats.indexOf(p["categoria"]?.toString()).coerceAtLeast(0));active.isChecked=p["ativo"]!=false;val u=p["unidadesMedida"] as? Map<*,*>;unitChecks.forEach{it.isChecked=false};val keys=listOf("unidade","quilo","maco","duzia","lote","bdj","umQuarto","umOitavo","metade");keys.forEachIndexed{i,k->unitChecks[i].isChecked=u?.get(k)==true};loteQty.setText(u?.get("quantidadePorLote")?.toString()? : "");existingImage=p["imagemUrl"]?.toString()? : "";imageUri=null;saveButton.text="Atualizar Produto";cancelButton.visibility=View.VISIBLE;nameInput.requestFocus()}
    private fun resetForm(){editingId=null;existingImage="";imageUri=null;nameInput.setText("");category.setSelection(0);active.isChecked=true;unitChecks.forEach{it.isChecked=false};loteQty.setText("");loteBox.visibility=View.GONE;imagePreview.visibility=View.GONE;saveButton.text="Salvar Produto";cancelButton.visibility=View.GONE}
    private fun deleteProduct(id:String){db.collection("produtos").document(id).delete().addOnFailureListener{Toast.makeText(this,"Não foi possível excluir",Toast.LENGTH_LONG).show()}}

    override fun onDestroy(){listener?.remove();super.onDestroy()}
}
