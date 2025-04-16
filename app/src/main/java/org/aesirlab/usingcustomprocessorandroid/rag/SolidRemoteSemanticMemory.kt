package org.aesirlab.usingcustomprocessorandroid.rag

//import android.content.Context
//import android.net.ConnectivityManager
//import com.google.ai.edge.localagents.rag.memory.SemanticMemory
//import com.google.ai.edge.localagents.rag.memory.VectorStore
//import com.google.ai.edge.localagents.rag.memory.VectorStoreRecord
//import com.google.ai.edge.localagents.rag.models.EmbedData
//import com.google.ai.edge.localagents.rag.models.Embedder
//import com.google.ai.edge.localagents.rag.models.EmbeddingRequest
//import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
//import com.google.ai.edge.localagents.rag.retrieval.RetrievalResponse
//import com.google.ai.edge.localagents.rag.retrieval.SemanticDataEntry
//import com.google.common.collect.ImmutableList
//import com.google.common.util.concurrent.Futures
//import com.google.common.util.concurrent.ListenableFuture
//import com.nimbusds.jose.jwk.ECKey
//import okhttp3.MediaType.Companion.toMediaType
//import okhttp3.RequestBody.Companion.toRequestBody
//import org.aesirlab.mylibrary.sharedfunctions.createUnsafeOkHttpClient
//import org.aesirlab.mylibrary.websockets.generatePostRequest
//import org.json.JSONObject
//import java.util.concurrent.Executor
//import java.util.function.Function
//
//class SolidRemoteSemanticMemory(
//    val vectorStore: VectorStore<String>,
//    val embeddingModel: Embedder<String>,
//    val workerExecutor: Executor,
//    val queryPostLocation: String,
//    var accessToken: String,
//    var expirationTime: Long,
//    var signingJwk: ECKey,
//    val context: Context
//    ): SemanticMemory<String> {
//    private val connectivityManager =
//        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
//
//    private fun isNetworkAvailable(): Boolean {
//        val networkInfo = connectivityManager.activeNetwork
//        if (networkInfo != null) {
//            val nwc = connectivityManager.getNetworkCapabilities(networkInfo)
//            nwc!!.signalStrength
//        }
//        return networkInfo != null
//    }
//
//
//    override fun retrieveResults(query: RetrievalRequest<String>): ListenableFuture<RetrievalResponse<String>> {
//        val entities = mutableListOf<String>()
//        if (isNetworkAvailable() && expirationTime < System.currentTimeMillis()) {
//            val client = createUnsafeOkHttpClient()
//            val jsonObj = JSONObject()
//            val innerObj = JSONObject()
//            innerObj.put("task_type", query.config.task.name)
//            innerObj.put("query", query.query)
//            innerObj.put("", query.config.topK)
//            innerObj.put("", query.config.minSimilarityScore)
//            val postBody = jsonObj.put("retrieve", innerObj)
//                .toString().toRequestBody(contentType = "application/json".toMediaType())
//            val postRequest = generatePostRequest(queryPostLocation, accessToken, signingJwk, postBody, contentType = "application/json")
//            val response = client.newCall(postRequest).execute()
//        }
//        return RetrievalResponse.create(entities)
//    }
//
//    override fun recordMemoryItem(item: String): ListenableFuture<Boolean> {
//        val dataEntry = SemanticDataEntry.create(item)
//        return this.recordMemoryEntry(dataEntry)
//    }
//
//    override fun recordMemoryEntry(entry: SemanticDataEntry<String>): ListenableFuture<Boolean> {
//        if (isNetworkAvailable() && expirationTime < System.currentTimeMillis()) {
//            val client = createUnsafeOkHttpClient()
//            val jsonObj = JSONObject()
//            val itemObj = jsonObj.put("item", entry.data)
//            val postBody = jsonObj.put("record", itemObj)
//                .toString().toRequestBody(contentType = "application/json".toMediaType())
//            val postRequest = generatePostRequest(queryPostLocation, accessToken, signingJwk, postBody, contentType = "application/json")
//            val response = client.newCall(postRequest).execute()
//        }
//    }
//
//    override fun recordBatchedMemoryItems(items: ImmutableList<String>): ListenableFuture<Boolean> {
//        return this.recordBatchedMemoryEntries(items.map { SemanticDataEntry.create(it) } as ImmutableList<SemanticDataEntry<String>>)
//    }
//
//    override fun recordBatchedMemoryEntries(entries: ImmutableList<SemanticDataEntry<String>>): ListenableFuture<Boolean> {
//        if (entries.isEmpty()) {
//            return Futures.immediateFuture(false)
//        } else {
//
//        }
//    }
//}