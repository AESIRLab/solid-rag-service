package org.aesirlab.usingcustomprocessorandroid.rag;

import java.util.List;

public final class TextChunker {

    static {
        System.loadLibrary("text_chunker_jni");
    }

    private final long jniHandle;

    public TextChunker() {
        this.jniHandle = nativeCreateTextChunker();
    }

    /**
     * Simply splits the text into chunks of fixed size with an overlap. The text is split into tokens
     * using whitespace as the delimiter.
     *
     * @param text The text to chunk.
     * @param chunkSize The size of each chunk.
     * @param chunkOverlap The overlap between chunks.
     * @return A list of chunks.
     */
    public List<String> chunk(String text, int chunkSize, int chunkOverlap) {
        return nativeChunk(jniHandle, text, chunkSize, chunkOverlap);
    }

    /**
     * Chunks the text on sentence boundaries. The text is split into sentences. This only supports
     * English at the moment. Each sentence is split into tokens using whitespace as the delimiter.
     *
     * @param text The text to chunk.
     * @param chunkSize The size of each chunk.
     * @return A list of chunks.
     */
    public List<String> chunkBySentences(String text, int chunkSize) {
        return nativeChunkBySentences(jniHandle, text, chunkSize);
    }

    private static native long nativeCreateTextChunker();

    private static native List<String> nativeChunk(
            long jniHandle, String text, int chunkSize, int chunkOverlap);

    private static native List<String> nativeChunkBySentences(
            long jniHandle, String text, int chunkSize);
}
