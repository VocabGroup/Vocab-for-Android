package com.example.util

object ArticleCoverage {
    private val irregular=mapOf("be" to listOf("am","is","are","was","were","been","being"),"go" to listOf("goes","went","gone","going"),"have" to listOf("has","had","having"),"do" to listOf("does","did","done","doing"),"take" to listOf("takes","took","taken","taking"),"write" to listOf("writes","wrote","written","writing"),"make" to listOf("makes","made","making"),"run" to listOf("runs","ran","running"),"see" to listOf("sees","saw","seen","seeing"),"get" to listOf("gets","got","gotten","getting"),"buy" to listOf("buys","bought","buying"),"think" to listOf("thinks","thought","thinking"),"child" to listOf("children"),"person" to listOf("people"))
    fun variants(word:String):List<String>{
        irregular[word]?.let{return listOf(word)+it}
        val result=mutableListOf(word,word+"s",word+"es",word+"ed",word+"ing")
        if(word.endsWith("e")){result.add(word+"d");result.add(word.dropLast(1)+"ing")}
        if(Regex("[^aeiou]y$").containsMatchIn(word)){result.add(word.dropLast(1)+"ies");result.add(word.dropLast(1)+"ied")}
        if(word.length<=5&&Regex("[aeiou][b-df-hj-np-tv-z]$").containsMatchIn(word)){result.add(word+word.last()+"ed");result.add(word+word.last()+"ing")}
        return result
    }
    fun pattern(word:String):Regex {
        val pattern=word.trim().lowercase().split(Regex("\\s+")).mapIndexed{index,part->"(?:"+(if(index==0)variants(part) else listOf(part)).joinToString("|"){Regex.escape(it)}+")"}.joinToString("[\\s-]+")
        return Regex("(?<![a-z])$pattern(?![a-z])",RegexOption.IGNORE_CASE)
    }
    fun missing(article:String,words:List<String>):List<String> = words.filter{!pattern(it).containsMatchIn(article.replace('’','\''))}
}
