package com.pitchrec.nativetest;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Wczytuje zapisany plik (WAV albo MP3) z powrotem do LiveAudioData, żeby PitchWaveView mógł
// go wyświetlić — dekoduje do PCM, liczy obwiednię i pitch (YIN) dla CAŁEGO pliku,
// "odtwarzając" tę samą analizę, którą normalnie robi serwis podczas nagrywania na żywo.
public class AudioFileLoader {

    private static final int YIN_WINDOW = 2048;

    public static void loadIntoLiveData(File file) throws IOException {
        LiveAudioData.reset();
        short[] samples = file.getName().endsWith(".mp3") ? decodeMp3(file) : decodeWav(file);

        float[] yinWindow = new float[YIN_WINDOW];
        int yinFillCount = 0;
        long samplePos = 0;

        int chunkSize = 2048;
        for (int offset = 0; offset < samples.length; offset += chunkSize) {
            int len = Math.min(chunkSize, samples.length - offset);
            short[] chunk = new short[len];
            System.arraycopy(samples, offset, chunk, 0, len);
            LiveAudioData.appendSamples(chunk, len);

            for (int i = 0; i < len; i++) {
                yinWindow[yinFillCount] = chunk[i] / 32768f;
                yinFillCount++;
                if (yinFillCount >= YIN_WINDOW) {
                    float freq = YinPitchDetector.detect(yinWindow);
                    long windowStartSample = samplePos + i - YIN_WINDOW + 1;
                    if (freq > 70 && freq < 1000) {
                        LiveAudioData.appendPitch(windowStartSample, freq);
                    } else {
                        LiveAudioData.appendPitch(windowStartSample, -1);
                    }
                    yinFillCount = 0;
                }
            }
            samplePos += len;
        }
    }

    private static short[] decodeWav(File file) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long dataSize = raf.length() - 44;
            raf.seek(44);
            byte[] bytes = new byte[(int) dataSize];
            raf.readFully(bytes);
            short[] samples = new short[bytes.length / 2];
            ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
            return samples;
        }
    }

    // Dekoduje MP3 przez wbudowany w Androida MediaExtractor/MediaCodec — Android wspiera
    // DEKODOWANIE MP3 natywnie (tylko nie kodowanie), więc to bezpieczniejsza, dobrze
    // dokumentowana droga niż zgadywanie niepewnego API zewnętrznego dekodera.
    private static short[] decodeMp3(File file) throws IOException {
        android.media.MediaExtractor extractor = new android.media.MediaExtractor();
        extractor.setDataSource(file.getAbsolutePath());

        int audioTrackIndex = -1;
        android.media.MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            android.media.MediaFormat f = extractor.getTrackFormat(i);
            String mime = f.getString(android.media.MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                audioTrackIndex = i;
                format = f;
                break;
            }
        }
        if (audioTrackIndex < 0 || format == null) {
            throw new IOException("Nie znaleziono strumienia audio w pliku MP3");
        }
        extractor.selectTrack(audioTrackIndex);

        android.media.MediaCodec codec;
        try {
            codec = android.media.MediaCodec.createDecoderByType(format.getString(android.media.MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();
        } catch (Exception e) {
            throw new IOException("Nie udalo sie utworzyc dekodera MP3: " + e.getMessage());
        }

        java.io.ByteArrayOutputStream pcmOut = new java.io.ByteArrayOutputStream();
        android.media.MediaCodec.BufferInfo info = new android.media.MediaCodec.BufferInfo();
        boolean inputDone = false, outputDone = false;

        while (!outputDone) {
            if (!inputDone) {
                int inIdx = codec.dequeueInputBuffer(10000);
                if (inIdx >= 0) {
                    java.nio.ByteBuffer inBuf = codec.getInputBuffer(inIdx);
                    int sampleSize = inBuf != null ? extractor.readSampleData(inBuf, 0) : -1;
                    if (sampleSize < 0) {
                        codec.queueInputBuffer(inIdx, 0, 0, 0, android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        codec.queueInputBuffer(inIdx, 0, sampleSize, extractor.getSampleTime(), 0);
                        extractor.advance();
                    }
                }
            }

            int outIdx = codec.dequeueOutputBuffer(info, 10000);
            if (outIdx >= 0) {
                java.nio.ByteBuffer outBuf = codec.getOutputBuffer(outIdx);
                if (outBuf != null && info.size > 0) {
                    byte[] chunk = new byte[info.size];
                    outBuf.get(chunk);
                    pcmOut.write(chunk, 0, chunk.length);
                }
                codec.releaseOutputBuffer(outIdx, false);
                if ((info.flags & android.media.MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                    outputDone = true;
                }
            }
        }

        codec.stop();
        codec.release();
        extractor.release();

        byte[] pcmBytes = pcmOut.toByteArray();
        short[] samples = new short[pcmBytes.length / 2];
        ByteBuffer.wrap(pcmBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(samples);
        return samples;
    }
}
