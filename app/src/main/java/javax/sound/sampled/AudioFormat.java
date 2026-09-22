package javax.sound.sampled;

// Minimalna, zgodna binarnie zaślepka javax.sound.sampled.AudioFormat — ta klasa w
// prawdziwej Javie SE jest tylko "trzymaczem danych" (sample rate, bity, kanały, znak,
// endianness), NIE robi żadnego realnego I/O audio z systemem operacyjnym. Bezpieczne do
// odtworzenia na Androidzie, gdzie pakietu javax.sound.sampled w ogóle nie ma. Wymagane
// przez bibliotekę java-lame (LameEncoder), która była pisana pod pełną Javę SE.
public class AudioFormat {
    private final float sampleRate;
    private final int sampleSizeInBits;
    private final int channels;
    private final boolean signed;
    private final boolean bigEndian;

    public AudioFormat(float sampleRate, int sampleSizeInBits, int channels, boolean signed, boolean bigEndian) {
        this.sampleRate = sampleRate;
        this.sampleSizeInBits = sampleSizeInBits;
        this.channels = channels;
        this.signed = signed;
        this.bigEndian = bigEndian;
    }

    public float getSampleRate() {
        return sampleRate;
    }

    public int getSampleSizeInBits() {
        return sampleSizeInBits;
    }

    public int getChannels() {
        return channels;
    }

    public boolean isBigEndian() {
        return bigEndian;
    }

    public int getFrameSize() {
        return (sampleSizeInBits / 8) * channels;
    }
}
