package com.radardosjogos.app

import android.app.*
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class Game(val name:String, val provider:String, val note:String="")
data class Spin(val game:String, val bet:Double, val prize:Double, val date:Long, val note:String="") {
    val net:Double get() = prize-bet
}

class MainActivity : Activity() {
    private val bg=android.graphics.Color.rgb(17,19,27)
    private val panel=android.graphics.Color.rgb(29,32,44)
    private val gold=android.graphics.Color.rgb(222,179,65)
    private val white=android.graphics.Color.rgb(244,244,248)
    private val muted=android.graphics.Color.rgb(170,174,188)
    private lateinit var root:LinearLayout
    private val games= mutableListOf<Game>()
    private val spins= mutableListOf<Spin>()
    private var tab="Painel"
    private val money= { v:Double -> "R$ "+String.format(Locale("pt","BR"),"%.2f",v) }

    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor=bg
        window.navigationBarColor=bg
        load()
        if(games.isEmpty()){
            games.add(Game("Fortune Tiger","PG Soft","Cadastro manual"))
            games.add(Game("Fortune Rabbit","PG Soft"))
            games.add(Game("Fortune Dragon","PG Soft"))
            save()
        }
        show()
    }

    private fun dp(v:Int)= (v*resources.displayMetrics.density).toInt()

    private fun text(s:String,size:Float=14f,color:Int=white,bold:Boolean=false):TextView=
        TextView(this).apply {
            text=s
            textSize=size
            setTextColor(color)
            if(bold) setTypeface(null, android.graphics.Typeface.BOLD)
            setPadding(dp(2),dp(3),dp(2),dp(3))
        }

    private fun button(s:String, action:()->Unit):Button=
        Button(this).apply {
            text=s
            textSize=13f
            setTextColor(white)
            setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.rgb(65,58,39)))
            setOnClickListener{action()}
        }

    private fun card():LinearLayout=
        LinearLayout(this).apply {
            orientation=LinearLayout.VERTICAL
            setPadding(dp(14),dp(12),dp(14),dp(12))
            background=android.graphics.drawable.GradientDrawable().apply{
                setColor(panel)
                cornerRadius=dp(16).toFloat()
            }
        }

    private fun addSpace(parent:LinearLayout,h:Int=10){
        parent.addView(View(this),LinearLayout.LayoutParams(1,dp(h)))
    }

    private fun show(){
        root=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setBackgroundColor(bg)
        }

        val header=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(18),dp(12),dp(18),dp(8))
        }
        header.addView(text("RADAR DOS JOGOS",20f,gold,true))
        header.addView(text("Estatísticas observadas • sem previsão de vitória",11f,muted))
        root.addView(header)

        val scroll=ScrollView(this)
        val body=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(16),dp(4),dp(16),dp(18))
        }
        scroll.addView(body)
        root.addView(scroll,LinearLayout.LayoutParams(-1,0,1f))

        when(tab){
            "Painel" -> dashboard(body)
            "Histórico" -> history(body)
            "Comparar" -> compare(body)
            else -> gameList(body)
        }

        val nav=LinearLayout(this).apply{
            setPadding(dp(5),dp(5),dp(5),dp(7))
            setBackgroundColor(panel)
            gravity=Gravity.CENTER
        }
        listOf("Painel","Histórico","Comparar","Jogos").forEach { t ->
            val b=TextView(this).apply{
                text=t
                textSize=12f
                gravity=Gravity.CENTER
                setTextColor(if(tab==t)gold else muted)
                setPadding(dp(8),dp(10),dp(8),dp(10))
                setOnClickListener{tab=t;show()}
            }
            nav.addView(b,LinearLayout.LayoutParams(0,-2,1f))
        }
        root.addView(nav)
        setContentView(root)
    }

    private fun addTitle(body:LinearLayout,title:String,sub:String=""){
        body.addView(text(title,18f,white,true))
        if(sub.isNotEmpty()) body.addView(text(sub,12f,muted))
        addSpace(body,8)
    }

    private fun metric(body:LinearLayout,label:String,value:String,detail:String){
        val c=card()
        c.addView(text(label,12f,muted))
        c.addView(text(value,23f,gold,true))
        c.addView(text(detail,11f,muted))
        body.addView(c)
        addSpace(body,8)
    }

    private fun stats():Triple<Double,Double,Double>{
        val bet=spins.sumOf{it.bet}
        val prize=spins.sumOf{it.prize}
        return Triple(bet,prize,prize-bet)
    }

    private fun dashboard(body:LinearLayout){
        addTitle(body,"Seu resumo","Os dados são locais e inseridos por você.")
        val (bet,prize,net)=stats()
        metric(body,"TOTAL APOSTADO REGISTRADO",money(bet),"Soma dos valores inseridos")
        metric(body,"PRÊMIOS REGISTRADOS",money(prize),"Soma dos prêmios inseridos")
        metric(body,"RESULTADO LÍQUIDO",money(net),if(net>=0)"Saldo observado positivo" else "Saldo observado negativo")
        val ret=if(bet>0) prize/bet*100 else 0.0
        metric(body,"RETORNO OBSERVADO",String.format(Locale("pt","BR"),"%.1f%%",ret),"Prêmios ÷ apostas. Não é previsão nem RTP oficial.")

        val c=card()
        c.addView(text("Histórico recente",16f,white,true))
        addSpace(c,5)
        if(spins.isEmpty()){
            c.addView(text("Ainda sem registros. Toque em \"Adicionar rodada\" para começar.",13f,muted))
        } else {
            spins.takeLast(5).reversed().forEach {
                c.addView(text("${it.game} • aposta ${money(it.bet)} • prêmio ${money(it.prize)} • líquido ${money(it.net)}",
                    12f,if(it.net>=0)android.graphics.Color.rgb(103,211,151) else android.graphics.Color.rgb(255,125,125)))
            }
        }
        body.addView(c)
        addSpace(body,10)
        body.addView(button("+ Adicionar rodada"){addSpinDialog()})
        addSpace(body,6)
        body.addView(button("+ Cadastrar jogo"){addGameDialog()})
        addSpace(body,12)
        body.addView(text("Importante: resultados anteriores não indicam qual será o próximo prêmio. Compare apenas períodos e amostras identificados.",11f,muted))
    }

    private fun history(body:LinearLayout){
        addTitle(body,"Histórico de rodadas","Registros manuais salvos neste aparelho.")
        body.addView(button("+ Adicionar rodada"){addSpinDialog()})
        addSpace(body,8)
        if(spins.isEmpty()){
            body.addView(text("Nenhuma rodada cadastrada.",14f,muted))
        } else {
            spins.sortedByDescending{it.date}.forEach { s ->
                val c=card()
                c.addView(text(s.game,15f,white,true))
                c.addView(text(SimpleDateFormat("dd/MM/yyyy HH:mm",Locale("pt","BR")).format(Date(s.date)),11f,muted))
                c.addView(text("Aposta: ${money(s.bet)}   Prêmio: ${money(s.prize)}",13f))
                c.addView(text("Resultado líquido: ${money(s.net)}",13f,if(s.net>=0)android.graphics.Color.rgb(103,211,151) else android.graphics.Color.rgb(255,125,125),true))
                if(s.note.isNotBlank()) c.addView(text(s.note,11f,muted))
                c.addView(button("Excluir registro"){
                    AlertDialog.Builder(this)
                        .setMessage("Excluir esta rodada?")
                        .setNegativeButton("Cancelar",null)
                        .setPositiveButton("Excluir"){_,_->
                            spins.remove(s)
                            save()
                            show()
                        }.show()
                })
                body.addView(c)
                addSpace(body,7)
            }
        }
        if(spins.isNotEmpty()){
            body.addView(button("Apagar todo o histórico"){
                AlertDialog.Builder(this)
                    .setMessage("Apagar todos os registros?")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Apagar"){_,_->
                        spins.clear()
                        save()
                        show()
                    }.show()
            })
        }
    }

    private fun compare(body:LinearLayout){
        addTitle(body,"Comparador","Ordenado pelo retorno observado dos registros manuais.")
        if(games.isEmpty()){
            body.addView(text("Cadastre jogos primeiro.",14f,muted))
            return
        }
        games.sortedByDescending { g ->
            val ss=spins.filter{it.game==g.name}
            val b=ss.sumOf{it.bet}
            if(b>0) ss.sumOf{it.prize}/b else -1.0
        }.forEach { g ->
            val ss=spins.filter{it.game==g.name}
            val b=ss.sumOf{it.bet}
            val p=ss.sumOf{it.prize}
            val rate=if(b>0) p/b*100 else null
            val c=card()
            c.addView(text(g.name,16f,white,true))
            c.addView(text(g.provider,11f,muted))
            c.addView(text(if(rate==null)"Sem dados registrados" else String.format(Locale("pt","BR"),"Retorno observado: %.1f%%",rate),15f,gold,true))
            c.addView(text("${ss.size} rodadas • Apostas ${money(b)} • Prêmios ${money(p)}",12f,muted))
            body.addView(c)
            addSpace(body,7)
        }
        body.addView(text("Atenção: uma amostra pequena pode variar muito. Maior retorno passado não significa maior chance futura.",11f,muted))
    }

    private fun gameList(body:LinearLayout){
        addTitle(body,"Meus jogos","Cadastre qualquer título; nenhum dado é importado automaticamente.")
        body.addView(button("+ Cadastrar jogo"){addGameDialog()})
        addSpace(body,8)
        games.forEach { g ->
            val c=card()
            c.addView(text(g.name,16f,white,true))
            c.addView(text("Provedor/categoria: ${g.provider}",12f,muted))
            if(g.note.isNotBlank()) c.addView(text(g.note,11f,muted))
            c.addView(button("Excluir jogo"){
                AlertDialog.Builder(this)
                    .setMessage("Excluir ${g.name}? Os registros desse jogo também serão removidos.")
                    .setNegativeButton("Cancelar",null)
                    .setPositiveButton("Excluir"){_,_->
                        games.remove(g)
                        spins.removeAll{it.game==g.name}
                        save()
                        show()
                    }.show()
            })
            body.addView(c)
            addSpace(body,7)
        }
    }

    private fun addGameDialog(){
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(20),dp(8),dp(20),0)
        }
        val name=EditText(this).apply{hint="Nome do jogo"}
        val provider=EditText(this).apply{hint="Provedor ou categoria (ex.: PG Soft)"}
        val note=EditText(this).apply{hint="Observação opcional"}
        box.addView(name)
        box.addView(provider)
        box.addView(note)
        AlertDialog.Builder(this)
            .setTitle("Cadastrar jogo")
            .setView(box)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar"){_,_->
                val n=name.text.toString().trim()
                if(n.isNotEmpty()){
                    games.add(Game(n,provider.text.toString().trim().ifEmpty{"Não informado"},note.text.toString().trim()))
                    save()
                    show()
                }else{
                    Toast.makeText(this,"Digite o nome do jogo.",Toast.LENGTH_SHORT).show()
                }
            }.show()
    }

    private fun addSpinDialog(){
        if(games.isEmpty()){
            Toast.makeText(this,"Cadastre um jogo primeiro.",Toast.LENGTH_SHORT).show()
            return
        }
        val box=LinearLayout(this).apply{
            orientation=LinearLayout.VERTICAL
            setPadding(dp(20),dp(8),dp(20),0)
        }
        val spinner=Spinner(this)
        spinner.adapter=ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,games.map{it.name})
        val bet=EditText(this).apply{hint="Valor apostado (ex.: 0,40)";inputType=8194}
        val prize=EditText(this).apply{hint="Prêmio recebido (ex.: 0,00)";inputType=8194}
        val note=EditText(this).apply{hint="Observação opcional"}
        box.addView(text("Jogo",13f,muted))
        box.addView(spinner)
        box.addView(bet)
        box.addView(prize)
        box.addView(note)
        AlertDialog.Builder(this)
            .setTitle("Adicionar rodada")
            .setView(box)
            .setNegativeButton("Cancelar",null)
            .setPositiveButton("Salvar"){_,_->
                val b=parseMoney(bet.text.toString())
                val p=parseMoney(prize.text.toString())
                if(b==null||p==null||b<0||p<0){
                    Toast.makeText(this,"Informe valores válidos e não negativos.",Toast.LENGTH_LONG).show()
                }else{
                    spins.add(Spin(games[spinner.selectedItemPosition].name,b,p,System.currentTimeMillis(),note.text.toString().trim()))
                    save()
                    show()
                }
            }.show()
    }

    private fun parseMoney(s:String):Double?=
        try{
            s.trim().replace("R$","").replace(" ","").replace(".","").replace(",",".").toDouble()
        }catch(_:Exception){null}

    private fun save(){
        val gj=JSONArray()
        games.forEach{gj.put(JSONObject().put("name",it.name).put("provider",it.provider).put("note",it.note))}
        val sj=JSONArray()
        spins.forEach{sj.put(JSONObject().put("game",it.game).put("bet",it.bet).put("prize",it.prize).put("date",it.date).put("note",it.note))}
        getSharedPreferences("radar",Context.MODE_PRIVATE).edit()
            .putString("games",gj.toString())
            .putString("spins",sj.toString())
            .apply()
    }

    private fun load(){
        val pref=getSharedPreferences("radar",Context.MODE_PRIVATE)
        try{
            val a=JSONArray(pref.getString("games","[]"))
            for(i in 0 until a.length()){
                val o=a.getJSONObject(i)
                games.add(Game(o.optString("name"),o.optString("provider"),o.optString("note")))
            }
            val b=JSONArray(pref.getString("spins","[]"))
            for(i in 0 until b.length()){
                val o=b.getJSONObject(i)
                spins.add(Spin(o.optString("game"),o.optDouble("bet"),o.optDouble("prize"),o.optLong("date"),o.optString("note")))
            }
        }catch(_:Exception){}
    }
}
