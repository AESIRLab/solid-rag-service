package org.aesirlab.usingcustomprocessorandroid.rag

import com.google.ai.edge.localagents.rag.memory.ColumnConfig
import com.google.ai.edge.localagents.rag.memory.TableConfig
import com.google.ai.edge.localagents.rag.memory.VectorStore
import com.google.ai.edge.localagents.rag.memory.VectorStoreRecord
import com.google.ai.edge.localagents.rag.memory.proto.KeyValuePair
import com.google.ai.edge.localagents.rag.memory.proto.MemoryRecord
import com.google.ai.edge.localagents.rag.memory.proto.Metadata
import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableMap
import com.google.common.collect.UnmodifiableIterator
import com.google.common.primitives.Floats
import com.google.protobuf.ExtensionRegistryLite
import com.google.protobuf.InvalidProtocolBufferException

class SaveableVectorStore(private val numEmbeddingDimensions: Int): VectorStore<String> {
    val DEFAULT_DATABASE_PATH: String = ""
    val DEFAULT_TABLE_NAME: String = "rag_vector_store"
    val DEFAULT_TEXT_COLUMN_NAME: String = "text"
    val DEFAULT_EMBEDDINGS_COLUMN_NAME: String = "embeddings"
    val DEFAULT_TABLE_CONFIG = TableConfig.create(
        "rag_vector_store",
        ImmutableList.of(
            ColumnConfig.create(
                "ROWID",
                "INTEGER",
                ColumnConfig.KeyType.PRIMARY_KEY,
                true,
                false
            ), ColumnConfig.create("text", "TEXT"), ColumnConfig.create("embeddings", "REAL")
        )
    )
    private var jniHandle: Long = 0

    constructor(numEmbeddingDimensions: Int, databasePath: String): this(numEmbeddingDimensions) {
        this.jniHandle = nativeCreateSqliteVectorStore(
            numEmbeddingDimensions,
            databasePath,
            this.DEFAULT_TEXT_COLUMN_NAME,
            this.DEFAULT_EMBEDDINGS_COLUMN_NAME,
            toTableConfigProtoBytes(this.DEFAULT_TABLE_CONFIG)
        )
    }

    constructor(
        numEmbeddingDimensions: Int,
        databasePath: String,
        textColumnName: String,
        embeddingColumnName: String,
        tableConfig: TableConfig
    ): this(numEmbeddingDimensions, databasePath) {
        this.jniHandle = nativeCreateSqliteVectorStore(
            numEmbeddingDimensions,
            databasePath,
            textColumnName,
            embeddingColumnName,
            toTableConfigProtoBytes(tableConfig)
        )
    }

    override fun insert(record: VectorStoreRecord<String>) {
        nativeInsert(this.jniHandle, toMemoryRecordProtoBytes(record))
    }

    override fun getNearestRecords(
        queryEmbeddings: List<Float>,
        topK: Int,
        minSimilarityScore: Float
    ): ImmutableList<VectorStoreRecord<String>> {
        return toVectorStoreRecordList(
            nativeGetNearestRecords(
                this.jniHandle,
                Floats.toArray(queryEmbeddings),
                topK,
                minSimilarityScore
            )
        )
    }

    fun sqlQuery(query: String) {
        nativeSqlQuery(this.jniHandle, query)
    }

    private fun toTableConfigProtoBytes(tableConfig: TableConfig): ByteArray {
        val builder = com.google.ai.edge.localagents.rag.memory.proto.TableConfig.newBuilder()
            .setName(tableConfig.name)
        val var2: UnmodifiableIterator<*> = tableConfig.columns.iterator()

        while (var2.hasNext()) {
            val columnConfig = var2.next() as ColumnConfig
            val column =
                com.google.ai.edge.localagents.rag.memory.proto.TableConfig.ColumnConfig.newBuilder()
                    .setName(columnConfig.name).setSqlType(columnConfig.sqlType)
                    .setKeyType(toKeyTypeEnum(columnConfig.keyType))
                    .setAutoIncrement(columnConfig.autoIncrement)
                    .setIsNullable(columnConfig.isNullable)
                    .build() as com.google.ai.edge.localagents.rag.memory.proto.TableConfig.ColumnConfig
            builder.addColumns(column)
        }

        return (builder.build() as com.google.ai.edge.localagents.rag.memory.proto.TableConfig).toByteArray()
    }

    private fun toMemoryRecordProtoBytes(record: VectorStoreRecord<String>): ByteArray {
        return (MemoryRecord.newBuilder().setText(record.data as String)
            .addAllEmbeddings(record.embeddings).setMetadata(toMetadataProto(record.metadata))
            .build() as MemoryRecord).toByteArray()
    }

    private fun toMetadataProto(metadata: Map<String, Any>): Metadata {
        val builder = Metadata.newBuilder()
        val var2: Iterator<Map.Entry<String, Any>> = metadata.entries.iterator()

        while (var2.hasNext()) {
            val keyValuePair: Map.Entry<String, Any> = var2.next()
            builder.addKeyValuePairs(
                KeyValuePair.newBuilder().setKey(
                    keyValuePair.key
                ).setValue(keyValuePair.value.toString()).build() as KeyValuePair
            )
        }

        return builder.build() as Metadata
    }

    private fun toKeyTypeEnum(keyType: ColumnConfig.KeyType): com.google.ai.edge.localagents.rag.memory.proto.TableConfig.ColumnConfig.KeyType {
        return when (keyType) {
            ColumnConfig.KeyType.PRIMARY_KEY -> {
                com.google.ai.edge.localagents.rag.memory.proto.TableConfig.ColumnConfig.KeyType.PRIMARY_KEY
            }

            ColumnConfig.KeyType.DEFAULT_NOT_KEY -> {
                com.google.ai.edge.localagents.rag.memory.proto.TableConfig.ColumnConfig.KeyType.DEFAULT_NOT_KEY
            }

            else -> throw IllegalArgumentException("Unknown key type: $keyType")
        }
    }

    private fun toVectorStoreRecordList(memoryRecords: List<ByteArray>): ImmutableList<VectorStoreRecord<String>> {
        return memoryRecords.map { toVectorStoreRecord(it) } as ImmutableList
    }

    private fun toVectorStoreRecord(memoryRecordBytes: ByteArray): VectorStoreRecord<String> {
        try {
            val memoryRecord =
                MemoryRecord.parseFrom(memoryRecordBytes, ExtensionRegistryLite.getEmptyRegistry())
            val metadata = toMetadataMap(memoryRecord.metadata)
            return VectorStoreRecord.create(
                memoryRecord.text,
                ImmutableList.copyOf(memoryRecord.embeddingsList),
                metadata
            )
        } catch (e: InvalidProtocolBufferException) {
            throw IllegalArgumentException("Failed to parse memory record", e)
        }
    }

    private fun toMetadataMap(metadata: Metadata): ImmutableMap<String, Any> {
        return metadata.keyValuePairsList.associate { it.key to it.value } as ImmutableMap<String, Any>
    }

    private external fun nativeCreateSqliteVectorStore(
        numEmbeddingDimensions: Int,
        databasePath: String,
        textColumnName: String,
        embeddingColumnName: String,
        tableConfigBytes: ByteArray
    ): Long

    private external fun nativeInsert(jniHandle: Long, memoryRecordBytes: ByteArray)

    private external fun nativeGetNearestRecords(
        jniHandle: Long,
        queryEmbeddings: FloatArray,
        topK: Int,
        minSimilarityScore: Float
    ): List<ByteArray>

    private external fun nativeSqlQuery(jniHandle: Long, query: String)

    companion object {
        init {
            System.loadLibrary("sqlite_vector_store_jni");
        }

    }
}