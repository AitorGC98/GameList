package com.agc.gamelist.api

data class NewsResponse(
    val status: String,
    val totalResults: Int,
    val articles: List<Article>
)

data class Article(
    val title: String,
    val url: String,
    val urlToImage: String?,
    val description: String,
    val publishedAt: String
)
