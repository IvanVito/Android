package com.example.homesaver;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.ForwardingSink;
import okio.Okio;
import okio.Sink;
public class ProgressRequestBody extends RequestBody {
    public interface ProgressCallback {
        void onProgress(long bytesWritten, long contentLength);
    }
    private final RequestBody delegate;
    private final ProgressCallback callback;

    public ProgressRequestBody(RequestBody delegate, ProgressCallback callback) {
        this.delegate = delegate;
        this.callback = callback;
    }

    @Nullable
    @Override
    public MediaType contentType() {
        return delegate.contentType();
    }

    @Override
    public long contentLength() throws IOException {
        return delegate.contentLength();
    }

    @Override
    public void writeTo(@NonNull BufferedSink sink) throws IOException {
        CountingSink countingSink = new CountingSink(sink);
        BufferedSink bufferedSink = Okio.buffer(countingSink);
        delegate.writeTo(bufferedSink);
        bufferedSink.flush();
    }

    private final class CountingSink extends ForwardingSink {
        CountingSink(Sink delegate) {
            super(delegate);
        }

        @Override
        public void write(@NonNull Buffer source, long byteCount) throws IOException {
            super.write(source, byteCount);
            if (callback != null) {
                try {
                    long total = contentLength();
                    callback.onProgress(byteCount, total);
                } catch (IOException e) {
                    callback.onProgress(byteCount, -1);
                }
            }
        }
    }
}
