package com.jssrc.resample;


import vavi.sound.pcm.resampling.ssrc.SSRC;
import vavi.util.I0Bessel;
import vavi.util.SplitRadixFft;

import javax.sound.sampled.AudioFormat;
import java.io.*;

/**
 * @version 1.0 3/25/11 2:38 PM
 * @author: Maksim Khadkevich
 */
public class JSSRCResampler extends InputStream {


    protected AudioFormat inAudioFormat;
    protected AudioFormat outAudioFormat;


    protected InputStream ssrcInputStream;


    protected DataOutputStream dataOutputStream;
    protected DataInputStream dataInputStream;

    protected PipedInputStream pipedInputStream;
    protected PipedOutputStream pipedOutputStream;


    protected Runnable resamplingRunnable;
    protected Thread resamplingThread;


    protected ByteArrayInputStream byteArrayInputStream;


    public JSSRCResampler(AudioFormat inAudioFormat, AudioFormat outAudioFormat, InputStream inputStream) {
        this.inAudioFormat = inAudioFormat;
        this.outAudioFormat = outAudioFormat;
        this.ssrcInputStream = inputStream;
        initialize();
    }


    protected void initialize() {

        if (outAudioFormat.getSampleRate() == inAudioFormat.getSampleRate()) {
            System.out.println("No sample rate conversion is needed");
            return;
        }
        initializeClasses();
        try {
            pipedInputStream = new PipedInputStream();
            dataInputStream = new DataInputStream(new BufferedInputStream(pipedInputStream));

            pipedOutputStream = new PipedOutputStream(pipedInputStream);
            dataOutputStream = new DataOutputStream(pipedOutputStream);

            ssrcInputStream = new BufferedInputStream(ssrcInputStream);

            resamplingRunnable = new Runnable() {
                public void run() {
                    try {
                        new SSRC(ssrcInputStream, dataOutputStream, (int) inAudioFormat.getSampleRate(), (int) outAudioFormat.getSampleRate(),
                                inAudioFormat.getFrameSize() / inAudioFormat.getChannels(),
                                inAudioFormat.getFrameSize() / inAudioFormat.getChannels(),
                                inAudioFormat.getChannels(), Integer.MAX_VALUE, 0, 0, true);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            };
            resamplingThread = new Thread(resamplingRunnable);
            resamplingThread.start();
            this.byteArrayInputStream = new ByteArrayInputStream(new byte[0]);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void  initializeClasses(){
        try {
            Class.forName(SSRC.class.getName());
            Class.forName(I0Bessel.class.getName());
            Class.forName(SplitRadixFft.class.getName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    @Override
    public int read() throws IOException {
        if (byteArrayInputStream.available() <= 0) {
            fillByteArrayInputStream();
        }
        return byteArrayInputStream.read();
    }


    protected int fillByteArrayInputStream() throws IOException {
        int bytesRead;
        byte[] newData = new byte[65536];
        if (resamplingThread.isAlive()) {
            bytesRead = dataInputStream.read(newData);
        } else {
            return -1;
        }
        if (bytesRead <= 0) {
            return -1;
        } else {
            byteArrayInputStream = new ByteArrayInputStream(newData, 0, bytesRead);
            return bytesRead;
        }
    }

    public void close() throws IOException {
        if (resamplingThread.isAlive()) {
            resamplingThread.stop();
        }
        pipedInputStream.close();
        pipedOutputStream.close();
    }

    @Override
    public int available() throws IOException {
        if (byteArrayInputStream.available() > 0) {
            return byteArrayInputStream.available();
        }
        int filledBytes = fillByteArrayInputStream();
        if (filledBytes > 0) {
            return filledBytes;
        } else {
            return -1;
        }
    }
}
