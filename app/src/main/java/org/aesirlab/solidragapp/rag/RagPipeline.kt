package org.aesirlab.solidragapp.rag


import android.app.Application
import android.util.Log
import com.google.ai.edge.localagents.rag.chains.ChainConfig
import com.google.ai.edge.localagents.rag.chains.RetrievalAndInferenceChain
import com.google.ai.edge.localagents.rag.memory.DefaultSemanticTextMemory
import com.google.ai.edge.localagents.rag.memory.SqliteVectorStore
import com.google.ai.edge.localagents.rag.models.AsyncProgressListener
import com.google.ai.edge.localagents.rag.models.Embedder
import com.google.ai.edge.localagents.rag.models.GeckoEmbeddingModel
import com.google.ai.edge.localagents.rag.models.GeminiEmbedder
import com.google.ai.edge.localagents.rag.models.LanguageModelResponse
import com.google.ai.edge.localagents.rag.models.MediaPipeLlmBackend
import com.google.mediapipe.tasks.genai.llminference.LlmInference.LlmInferenceOptions
import com.google.ai.edge.localagents.rag.prompt.PromptBuilder
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig
import com.google.ai.edge.localagents.rag.retrieval.RetrievalConfig.TaskType
import com.google.ai.edge.localagents.rag.retrieval.RetrievalRequest
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.Executors
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.guava.await
import java.io.InputStream
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

/** The RAG pipeline for LLM generation. */
private const val TAG = "RAG_PIPELINE"
class RagPipeline(application: Application) {

    private val mediaPipeLanguageModelOptions: LlmInferenceOptions =
        LlmInferenceOptions.builder().setModelPath(
            GEMMA_MODEL_PATH
        ).setPreferredBackend(LlmInference.Backend.GPU).setMaxTokens(1024).build()
    private val mediaPipeLanguageModelSessionOptions: LlmInferenceSession.LlmInferenceSessionOptions =
        LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(0.7f)
            .setTopP(0.95f)
            .setTopK(16)
            .build()
    private val mediaPipeLanguageModel: MediaPipeLlmBackend =
        MediaPipeLlmBackend(
            application.applicationContext, mediaPipeLanguageModelOptions,
            mediaPipeLanguageModelSessionOptions
        )

    private val embedder: Embedder<String> = if (COMPUTE_EMBEDDINGS_LOCALLY) {
        GeckoEmbeddingModel(
            GECKO_MODEL_PATH,
            Optional.of(TOKENIZER_MODEL_PATH),
            USE_GPU_FOR_EMBEDDINGS,
        )
    } else {
        GeminiEmbedder(
            GEMINI_EMBEDDING_MODEL,
            GEMINI_API_KEY
        )
    }

    private val defaultSemanticTextMemory = DefaultSemanticTextMemory(SqliteVectorStore(768), embedder)

    private val config = ChainConfig.create(
        mediaPipeLanguageModel, PromptBuilder(PROMPT_TEMPLATE),
        defaultSemanticTextMemory
    )
    private val retrievalAndInferenceChain = RetrievalAndInferenceChain(config)

    init {
        Futures.addCallback(
            mediaPipeLanguageModel.initialize(),
            object : FutureCallback<Boolean> {
                override fun onSuccess(result: Boolean) {
                    // no-op
                }

                override fun onFailure(t: Throwable) {
                    // no-op
                }
            },
            Executors.newSingleThreadExecutor(),
        )
    }

    fun forget() {
        config.semanticMemory.getOrNull()?.recordBatchedMemoryItems(ImmutableList.copyOf(listOf()))
    }

    fun memorizeChunks(data: InputStream) {

        val buffer = ByteArray(256)
        var bytesRead: Int
        val texts = mutableListOf<String>()
        while (data.read(buffer). also { bytesRead = it } != -1) {
            texts.add(String(buffer, 0, bytesRead))
        }

        Log.d(TAG, "${texts.size}")
        if (texts.isNotEmpty()) {
            memorize(texts)
            Log.d(TAG, "after memorized: ${System.currentTimeMillis()}")
        }
        data.close()
    }

    /** Stores input texts in the semantic text memory. */
    private fun memorize(facts: List<String>) {
        config.semanticMemory.getOrNull()?.recordBatchedMemoryItems(ImmutableList.copyOf(facts))
    }

    /** Generates the response from the LLM. */
    suspend fun generateResponse(
        prompt: String,
        callback: AsyncProgressListener<LanguageModelResponse>?,
    ): String =
        coroutineScope {
            val retrievalRequest =
                RetrievalRequest.create(
                    prompt,
                    RetrievalConfig.create(3, 0.0f, TaskType.QUESTION_ANSWERING)
                )
            retrievalAndInferenceChain.invoke(retrievalRequest, callback).await().text
        }

    companion object {
        private const val COMPUTE_EMBEDDINGS_LOCALLY = true
        private const val USE_GPU_FOR_EMBEDDINGS = true
        private const val CHUNK_SEPARATOR = "<chunk_splitter>"

        private const val GEMMA_MODEL_PATH = "/data/local/tmp/gemma3-1b-it-int4.task"
        private const val TOKENIZER_MODEL_PATH = "/data/local/tmp/sentencepiece.model"
        private const val GECKO_MODEL_PATH = "/data/local/tmp/gecko.tflite"
        private const val GEMINI_EMBEDDING_MODEL = "models/text-embedding-004"
        private const val GEMINI_API_KEY = "..."

        // The prompt template for the RetrievalAndInferenceChain. It takes two inputs: {0}, which is the retrieved context, and {1}, which is the user's query.
        private const val PROMPT_TEMPLATE: String =
            "You are an assistant for question-answering tasks. Here are the things I want to remember: {0} Use the things I want to remember, answer the following question the user has: {1}"
    }
}
