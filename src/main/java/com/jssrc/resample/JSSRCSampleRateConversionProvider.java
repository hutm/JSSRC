package com.jssrc.resample;


import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.spi.FormatConversionProvider;

/**
 * @author Maksim Khadkevich
 */

public class JSSRCSampleRateConversionProvider extends FormatConversionProvider {
    private static final AudioFormat.Encoding[] inputEncodings = {
            AudioFormat.Encoding.PCM_SIGNED,
    };

    private static final AudioFormat.Encoding[] outputEncodings = {
            AudioFormat.Encoding.PCM_SIGNED,
    };


    public AudioFormat.Encoding[] getSourceEncodings() {
        AudioFormat.Encoding[] encodings = new AudioFormat.Encoding[inputEncodings.length];
        System.arraycopy(inputEncodings, 0, encodings, 0, inputEncodings.length);
        return encodings;
    }

    public AudioFormat.Encoding[] getTargetEncodings() {
        AudioFormat.Encoding[] encodings = new AudioFormat.Encoding[outputEncodings.length];
        System.arraycopy(outputEncodings, 0, encodings, 0, outputEncodings.length);
        return encodings;
    }


    public AudioFormat.Encoding[] getTargetEncodings(AudioFormat sourceFormat) {
        if (sourceFormat.getEncoding().equals(AudioFormat.Encoding.PCM_SIGNED)) {

            AudioFormat.Encoding encs[] = new AudioFormat.Encoding[1];
            encs[0] = AudioFormat.Encoding.PCM_SIGNED;
            return encs;
        } else {
            return new AudioFormat.Encoding[0];
        }
    }


    public AudioFormat[] getTargetFormats(AudioFormat.Encoding targetEncoding, AudioFormat sourceFormat) {
        return new AudioFormat[0];
    }


    /**
     */
    public AudioInputStream getAudioInputStream(AudioFormat.Encoding targetEncoding, AudioInputStream sourceStream) {

        if (isConversionSupported(targetEncoding, sourceStream.getFormat())) {

            AudioFormat sourceFormat = sourceStream.getFormat();
            AudioFormat targetFormat = new AudioFormat(targetEncoding,
                    sourceFormat.getSampleRate(),
                    sourceFormat.getSampleSizeInBits(),
                    sourceFormat.getChannels(),
                    sourceFormat.getFrameSize(),
                    sourceFormat.getFrameRate(),
                    sourceFormat.isBigEndian());

            return getAudioInputStream(targetFormat, sourceStream);

        } else {
            throw new IllegalArgumentException("Unsupported conversion: " + sourceStream.getFormat().toString() + " to " + targetEncoding.toString());
        }

    }


    public AudioInputStream getAudioInputStream(AudioFormat targetFormat, AudioInputStream sourceStream) {

        AudioFormat inputFormat = sourceStream.getFormat();
        AudioFormat outputFormat = targetFormat;

        if (isConversionSupported(inputFormat, outputFormat)) {
            JSSRCResampler resampler = new JSSRCResampler(inputFormat, outputFormat, sourceStream);
            // set sample size from source stream if possible
            long length = AudioSystem.NOT_SPECIFIED;
            if (AudioSystem.NOT_SPECIFIED != sourceStream.getFrameLength()) {
                length = (long) (sourceStream.getFrameLength() * targetFormat.getSampleRate() / inputFormat.getSampleRate());
            }
            return new AudioInputStream(resampler, outputFormat, length);
        }
        throw new IllegalArgumentException("Unsupported conversion: " + sourceStream.getFormat().toString() + " to " + targetFormat.toString());
    }


    /**
     * Determines whether the codec supports conversion from one
     * particular format to another.
     *
     * @param inputFormat format of the incoming data
     * @return true if the conversion is supported, otherwise false
     */
    public boolean isConversionSupported(AudioFormat inputFormat, AudioFormat outputFormat) {

        AudioFormat.Encoding inputEncoding = inputFormat.getEncoding();
        AudioFormat.Encoding outputEncoding = outputFormat.getEncoding();
        int inputSampleSize = inputFormat.getSampleSizeInBits();
        int outputSampleSize = outputFormat.getSampleSizeInBits();
        boolean inputIsBigEndian = inputFormat.isBigEndian();
        boolean outputIsBigEndian = outputFormat.isBigEndian();

        if (inputFormat.getSampleRate() == outputFormat.getSampleRate()) {
            return false;
        }

        if (inputIsBigEndian || outputIsBigEndian) {
            return false;
        }


        if (((inputSampleSize == 8) && (outputSampleSize == 8)) ||
                ((inputSampleSize == 16) && (outputSampleSize == 16))) {

            if ((inputEncoding == AudioFormat.Encoding.PCM_SIGNED)) {
                if ((outputEncoding == AudioFormat.Encoding.PCM_SIGNED)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


}

