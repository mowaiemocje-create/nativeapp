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

    // Zagnieżdżona klasa Encoding — brakujący element powodujący
    // NoClassDefFoundError w statycznym inicjalizatorze LameEncoder. Odtworzona zgodnie z
    // prawdziwym API javax.sound.sampled.AudioFormat.Encoding (zestaw predefiniowanych,
    // nazwanych stałych, nie realne I/O — bezpieczne do odtworzenia).
    public static class Encoding {
        public static final Encoding PCM_SIGNED = new Encoding("PCM_SIGNED");
        public static final Encoding PCM_UNSIGNED = new Encoding("PCM_UNSIGNED");
        public static final Encoding PCM_FLOAT = new Encoding("PCM_FLOAT");
        public static final Encoding ULAW = new Encoding("ULAW");
        public static final Encoding ALAW = new Encoding("ALAW");

        private final String name;

        public Encoding(String name) {
            this.name = name;
        }

        @Override
        public final String toString() {
            return name;
        }

        @Override
        public final boolean equals(Object obj) {
            if (obj == this) return true;
            if (!(obj instanceof Encoding)) return false;
            return name.equals(((Encoding) obj).name);
        }

        @Override
        public final int hashCode() {
            return name.hashCode();
        }
    }
}
