package com.cmder.nv21merger;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;

public class NV21Merger {

    public static byte[] mergeNV21Frames(byte[][] frames, int frameWidth, int frameHeight) {
        if (frames.length != 6) {
            throw new IllegalArgumentException("Exactly 6 frames are required.");
        }

        int mergedWidth = frameWidth * 3; // 3 frames horizontally
        int mergedHeight = frameHeight * 2; // 2 frames vertically

        int frameSize = frameWidth * frameHeight;
        int chromaSize = frameSize / 2;

        byte[] mergedFrame = new byte[mergedWidth * mergedHeight * 3 / 2];

        // Process Y plane
        for (int i = 0; i < 6; i++) {
            int srcOffsetY = 0;
            int dstOffsetY = ((i / 3) * frameHeight * mergedWidth) + (i % 3) * frameWidth;
            for (int y = 0; y < frameHeight; y++) {
                System.arraycopy(frames[i], srcOffsetY, mergedFrame, dstOffsetY, frameWidth);
                srcOffsetY += frameWidth;
                dstOffsetY += mergedWidth;
            }
        }

        // Process VU plane
        for (int i = 0; i < 6; i++) {
            int srcOffsetVU = frameSize;
            int dstOffsetVU = mergedWidth * mergedHeight + ((i / 3) * frameHeight * mergedWidth / 2) + (i % 3) * frameWidth;
            for (int y = 0; y < frameHeight / 2; y++) {
                System.arraycopy(frames[i], srcOffsetVU, mergedFrame, dstOffsetVU, frameWidth);
                srcOffsetVU += frameWidth;
                dstOffsetVU += mergedWidth;
            }
        }

        return mergedFrame;
    }

    public static boolean isMerging = false;

    public static void encodeToMp4(String outputPath, ArrayBlockingQueue<byte[]> frameQueue, int width, int height, int frameRate, int bitRate) throws IOException {
        MediaFormat format = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, width, height);
        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);
        format.setInteger(MediaFormat.KEY_FRAME_RATE, frameRate);
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // I-frame interval in seconds

        MediaCodec encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        encoder.start();

        MediaMuxer muxer = new MediaMuxer(outputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        int trackIndex = -1;
        boolean muxerStarted = false;

        MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
        long presentationTimeUs = 0;

        while (isMerging) {
            byte[] frameData = frameQueue.poll();
            if (frameData == null) {
                continue;
            }

            ByteBuffer[] inputBuffers = encoder.getInputBuffers();
            int inputBufferIndex = encoder.dequeueInputBuffer(-1);
            if (inputBufferIndex >= 0) {
                ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
                inputBuffer.clear();
                inputBuffer.put(frameData);
                encoder.queueInputBuffer(inputBufferIndex, 0, frameData.length, presentationTimeUs, 0);
                presentationTimeUs += 1000000 / frameRate;
            }

            ByteBuffer[] outputBuffers = encoder.getOutputBuffers();
            int outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            while (outputBufferIndex >= 0) {
                ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];

                if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG) {
                    MediaFormat newFormat = encoder.getOutputFormat();
                    trackIndex = muxer.addTrack(newFormat);
                    muxer.start();
                    muxerStarted = true;
                } else if (bufferInfo.size != 0) {
                    if (!muxerStarted) {
                        throw new IllegalStateException("Muxer hasn't started");
                    }

                    outputBuffer.position(bufferInfo.offset);
                    outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
                    muxer.writeSampleData(trackIndex, outputBuffer, bufferInfo);
                }

                encoder.releaseOutputBuffer(outputBufferIndex, false);
                outputBufferIndex = encoder.dequeueOutputBuffer(bufferInfo, 0);
            }
        }

        encoder.stop();
        encoder.release();
        if (muxerStarted) {
            muxer.stop();
        }
        muxer.release();
    }
}
