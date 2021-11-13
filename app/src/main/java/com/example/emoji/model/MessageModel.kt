package com.example.emoji.model

data class MessageModel(
    val id : Int,
    val userId : Int,
    val name : String,
    val message : String,
    val picture : String,
    val date : String,
    val month : String,
    val isMe : Boolean,
    val listReactions : List<Reaction>
){
    var countedReactions : Map<String, Int> = mapOf()
}
