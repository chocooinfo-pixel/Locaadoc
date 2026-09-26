package com.archidoc.cartonlocator

import android.app.*
import android.content.*
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

private data class Carton(val code:String,val client:String,val transfert:String,val dossier:String,val site:String,val zone:String,val rayonnage:String,val niveau:String,val position:String,val updated:String)
private data class Move(val code:String,val from:String,val to:String,val date:String)

class MainActivity: AppCompatActivity(){
 private lateinit var db:CartonDb; private lateinit var content:LinearLayout
 private var pendingImport=false
 override fun onCreate(b:Bundle?){super.onCreate(b);db=CartonDb(this);buildShell();showScan()}
 private fun buildShell(){
  val root=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(20,16,20,12)}
  val head=TextView(this).apply{text="📦  CARTON LOCATOR V2";textSize=24f;setTypeface(null,Typeface.BOLD);setPadding(0,0,0,12)};root.addView(head)
  val tabs=LinearLayout(this); listOf("Scanner","Recherche","Tableau de bord","Historique","Données").forEachIndexed{i,t->tabs.addView(Button(this).apply{text=t;setOnClickListener{when(i){0->showScan();1->showSearch();2->showDashboard();3->showHistory();4->showData()}}},LinearLayout.LayoutParams(0,56,1f))};root.addView(tabs)
  content=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(0,14,0,0)};root.addView(content,LinearLayout.LayoutParams(-1,0,1f));setContentView(root)
 }
 private fun clear(){content.removeAllViews()}
 private fun field(h:String)=EditText(this).apply{hint=h;isSingleLine=true;textSize=17f;imeOptions=EditorInfo.IME_ACTION_NEXT}
 private fun showScan(){clear();
  val note=TextView(this).apply{text="WORKFLOW\n1. Scanner le site/zone/rayonnage/niveau/position\n2. Scanner le carton\n3. Enregistrer\n\nUne douchette Bluetooth/USB configurée en mode clavier fonctionne directement.";textSize=15f};content.addView(note)
  val site=field("Site (ex. EL AGBA)");val zone=field("Zone (ex. Z01)");val ray=field("Rayonnage (ex. R01)");val niv=field("Niveau (ex. N03)");val pos=field("Position (ex. P05)");val code=field("SCAN CARTON");val client=field("Client (facultatif)");val trans=field("Transfert (facultatif)");val dossier=field("N° dossier (facultatif)")
  listOf(site,zone,ray,niv,pos,code,client,trans,dossier).forEach{content.addView(it,LinearLayout.LayoutParams(-1,55))}
  val out=TextView(this).apply{textSize=16f};content.addView(out)
  content.addView(Button(this).apply{text="💾 ENREGISTRER";setOnClickListener{val c=code.text.toString().trim();if(c.isBlank()){out.text="⚠️ Scanner le carton";return@setOnClickListener};val loc=Location(site.text.toString(),zone.text.toString(),ray.text.toString(),niv.text.toString(),pos.text.toString());val old=db.find(c);db.save(Carton(c,client.text.toString(),trans.text.toString(),dossier.text.toString(),loc.site,loc.zone,loc.ray,loc.niv,loc.pos,now()));out.text=if(old==null)"✅ Carton $c localisé à ${loc.label()}" else "🔄 Déplacement : ${old.location()} → ${loc.label()}";code.text.clear();code.requestFocus()}})
 }
 private fun showSearch(){clear();val q=field("Scanner ou saisir : carton / client / dossier / emplacement");content.addView(q,LinearLayout.LayoutParams(-1,60));val out=TextView(this).apply{textSize=15f};content.addView(ScrollView(this).apply{addView(out)},LinearLayout.LayoutParams(-1,0,1f));fun run(){val rows=db.search(q.text.toString().trim());out.text=if(rows.isEmpty())"❌ Aucun résultat" else rows.joinToString("\n\n"){format(it)}};content.addView(Button(this).apply{text="🔎 RECHERCHER";setOnClickListener{run()}});q.setOnEditorActionListener{_,_,_->run();true}}
 private fun showDashboard(){clear();val s=db.stats();content.addView(TextView(this).apply{text="TABLEAU DE BORD";textSize=22f;setTypeface(null,Typeface.BOLD)});content.addView(TextView(this).apply{text="\n📦 Cartons localisés : ${s[0]}\n🏢 Sites : ${s[1]}\n🗄️ Rayonnages utilisés : ${s[2]}\n🔄 Mouvements enregistrés : ${s[3]}\n⚠️ Cartons sans emplacement : ${s[4]}\n\nDernières opérations :";textSize=18f});content.addView(TextView(this).apply{text=db.history(8).joinToString("\n"){ "• ${it.code} : ${it.to} — ${it.date}" };textSize=15f})}
 private fun showHistory(){clear();val list=db.history(300);content.addView(ScrollView(this).apply{addView(TextView(this@MainActivity).apply{text=if(list.isEmpty())"Aucun mouvement." else list.joinToString("\n\n"){ "📦 ${it.code}\n${it.from.ifBlank{"—"}} → ${it.to}\n🕒 ${it.date}"};textSize=15f})},LinearLayout.LayoutParams(-1,0,1f))}
 private fun showData(){clear();content.addView(TextView(this).apply{text="DONNÉES\n\nImportez une base XLSX contenant au minimum la colonne Code carton. Les colonnes reconnues : Client, Transfert, Dossier, Site, Zone, Rayonnage, Niveau, Position.";textSize=16f});content.addView(Button(this).apply{text="📥 IMPORTER EXCEL (.XLSX)";setOnClickListener{startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply{type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";addCategory(Intent.CATEGORY_OPENABLE)},100)}});content.addView(Button(this).apply{text="📤 EXPORTER EXCEL (.XLSX)";setOnClickListener{exportXlsx()}});content.addView(Button(this).apply{text="🗑️ VIDER LA BASE";setOnClickListener{AlertDialog.Builder(this@MainActivity).setTitle("Confirmation").setMessage("Supprimer tous les cartons et mouvements ?").setNegativeButton("Annuler",null).setPositiveButton("Supprimer"){_,_->db.clear();showData()}.show()}})}
 override fun onActivityResult(r:Int,c:Int,d:Intent?){super.onActivityResult(r,c,d);if(r==100&&c==RESULT_OK)d?.data?.let{importXlsx(it)}}
 private fun importXlsx(uri:Uri){try{contentResolver.openInputStream(uri).use{ins->val wb=WorkbookFactory.create(ins);val sh=wb.getSheetAt(0);var n=0;for(row in sh){if(row.rowNum==0)continue;fun v(vararg names:String):String{for(i in 0 until row.lastCellNum){val h=sh.getRow(0)?.getCell(i)?.toString()?.trim()?.lowercase(Locale.ROOT)?:"";if(names.any{h.contains(it)})return row.getCell(i)?.toString()?.trim()? :""};return ""};val code=v("code carton","carton","code");if(code.isNotBlank()){db.save(Carton(code,v("client"),v("transfert"),v("dossier"),v("site"),v("zone"),v("rayonnage","rayon"),v("niveau"),v("position"),now()));n++}};wb.close();Toast.makeText(this,"Import terminé : $n cartons",Toast.LENGTH_LONG).show()} }catch(e:Exception){Toast.makeText(this,"Erreur import : ${e.message}",Toast.LENGTH_LONG).show()}}
 private fun exportXlsx(){try{val wb=XSSFWorkbook();val sh=wb.createSheet("Cartons");val heads=listOf("Code carton","Client","Transfert","Dossier","Site","Zone","Rayonnage","Niveau","Position","Dernière mise à jour");heads.forEachIndexed{i,h->sh.createRow(0).createCell(i).setCellValue(h)};db.all().forEachIndexed{r,c->val row=sh.createRow(r+1);listOf(c.code,c.client,c.transfert,c.dossier,c.site,c.zone,c.rayonnage,c.niveau,c.position,c.updated).forEachIndexed{i,v->row.createCell(i).setCellValue(v)}};val f=File(cacheDir,"cartons_${System.currentTimeMillis()}.xlsx");FileOutputStream(f).use{wb.write(it)};wb.close();val uri=FileProvider.getUriForFile(this,"$packageName.provider",f);startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply{type="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";putExtra(Intent.EXTRA_STREAM,uri);addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)},"Exporter la base Excel"))}catch(e:Exception){Toast.makeText(this,"Erreur export : ${e.message}",Toast.LENGTH_LONG).show()}}
 private fun format(c:Carton)="📦 ${c.code}\n👤 ${c.client.ifBlank{"Client non renseigné"}}\n📄 ${c.dossier.ifBlank{"Dossier —"}}\n📍 ${c.location()}\n🕒 ${c.updated}"
 private fun now()=SimpleDateFormat("dd/MM/yyyy HH:mm",Locale.getDefault()).format(Date())
 private data class Location(val site:String,val zone:String,val ray:String,val niv:String,val pos:String){fun label()=listOf(site,zone,ray,niv,pos).filter{it.isNotBlank()}.joinToString(" / ")}
}
private fun Carton.location()=listOf(site,zone,rayonnage,niveau,position).filter{it.isNotBlank()}.joinToString(" / ")

class CartonDb(ctx:Context){
 private val p=ctx.getSharedPreferences("carton_v2",Context.MODE_PRIVATE)
 private val h=ctx.getSharedPreferences("moves_v2",Context.MODE_PRIVATE)
 private fun enc(c:Carton) = listOf(c.client,c.transfert,c.dossier,c.site,c.zone,c.rayonnage,c.niveau,c.position,c.updated).joinToString("§")
 private fun dec(code:String,s:String):Carton?{val a=s.split("§");if(a.size<9)return null;return Carton(code,a[0],a[1],a[2],a[3],a[4],a[5],a[6],a[7],a[8])}
 fun find(code:String)=p.getString(code,null)?.let{dec(code,it)}
 fun save(c:Carton){val old=find(c.code);p.edit().putString(c.code,enc(c)).apply();val move=Move(c.code,old?.location().orEmpty(),c.location(),c.updated);val prev=h.getString("all","").orEmpty();h.edit().putString("all",listOf(move.code,move.from,move.to,move.date).joinToString("§")+"\n"+prev).apply()}
 fun all()=p.all.mapNotNull{(k,v)->if(v is String)dec(k,v)else null}
 fun search(q:String):List<Carton>{val x=q.lowercase();return all().filter{listOf(it.code,it.client,it.transfert,it.dossier,it.site,it.zone,it.rayonnage,it.niveau,it.position).any{v->v.lowercase().contains(x)}}.sortedBy{it.code}}
 fun history(limit:Int):List<Move>{return h.getString("all","").orEmpty().lines().filter{it.isNotBlank()}.take(limit).mapNotNull{val a=it.split("§");if(a.size>=4)Move(a[0],a[1],a[2],a[3])else null}}
 fun stats():List<Int>{val a=all();return listOf(a.size,a.map{it.site}.filter{it.isNotBlank()}.distinct().size,a.map{it.rayonnage}.filter{it.isNotBlank()}.distinct().size,history(100000).size,a.count{it.location().isBlank()})}
 fun clear(){p.edit().clear().apply();h.edit().clear().apply()}
}
