package com.example.selector.utils;

/**
 * 实现监控进度的关键部分就在于记录已传输字节数，
 * 所以我们需重载FilterOutputStream，重写其中的关键方法，
 * 实现进度监听的功能
 *
 * ProgressOutHttpEntity：输出流(OutputStream)时记录已发送字节数
 *
 * HttpEntityWrapper，也就是将需发送的HttpEntity打包，以便计算总字节数
 * Created by pbq on 2016/7/17.
 */
import org.apache.http.HttpEntity;
import org.apache.http.entity.HttpEntityWrapper;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class ProgressOutHttpEntity extends HttpEntityWrapper {
    /**进度监听对象**/
    private final ProgressListener listener;
    public ProgressOutHttpEntity(final HttpEntity entity,final ProgressListener listener) {
        super(entity);
        this.listener = listener;
    }

    public static class CountingOutputStream extends FilterOutputStream {

        private final ProgressListener listener;
        private long transferred;

        CountingOutputStream(final OutputStream out,
                             final ProgressListener listener) {
            super(out);
            this.listener = listener;
            this.transferred = 0;
        }

        @Override
        public void write(final byte[] b, final int off, final int len)
                throws IOException {
            out.write(b, off, len);
            this.transferred += len;
            this.listener.transferred(this.transferred);
        }

        @Override
        public void write(final int b) throws IOException {
            out.write(b);
            this.transferred++;
            this.listener.transferred(this.transferred);
        }

    }

    @Override
    public void writeTo(final OutputStream out) throws IOException {
        this.wrappedEntity.writeTo(out instanceof CountingOutputStream ? out
                : new CountingOutputStream(out, this.listener));
    }
    /**
     * 进度监听器接口
     */
    public interface ProgressListener {
        public void transferred(long transferedBytes);
    }
}