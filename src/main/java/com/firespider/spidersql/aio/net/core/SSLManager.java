package com.firespider.spidersql.aio.net.core;


import javax.net.ssl.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.concurrent.ExecutionException;

public class SSLManager {
    private KeyManagerFactory keyManagerFactory;
    private TrustManagerFactory trustManagerFactory;
    private SSLContext context;
    private SSLEngine engine;
    private boolean needClientAuth;

    private boolean handShakeDone = false;

    private ByteBuffer appBuffer, netBuffer, readBuffer;

    /**
     * 构造函数
     * 默认使用客户端认证
     *
     * @param protocol 协议类型
     * @throws NoSuchAlgorithmException 无可用协议异常
     */
    public SSLManager(String protocol, String host, int port) throws SSLException {
        this(protocol, host, port, false);
    }

    /**
     * 构造函数
     *
     * @param protocol      协议类型
     * @param useClientAuth 是否使用客户端认证, true:双向认证, false: 单向认证
     * @throws SSLException SSL 异常
     */
    public SSLManager(String protocol, String host, int port, boolean useClientAuth) throws SSLException {
        this.needClientAuth = useClientAuth;
        createSSLEngine(protocol, host, port);
        initBuf();
    }

    /**
     * 读取管理证书
     *
     * @param manageCertFile 证书地址
     * @param certPassword   证书密码
     * @param keyPassword    密钥
     * @throws SSLException SSL 异常
     */
    public void loadCertificate(String manageCertFile, String certPassword, String keyPassword) throws SSLException {

        FileInputStream certFIS = null;
        try {
            keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            trustManagerFactory = TrustManagerFactory.getInstance("SunX509");

            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            certFIS = new FileInputStream(manageCertFile);
            keystore.load(certFIS, certPassword.toCharArray());

            keyManagerFactory.init(keystore, keyPassword.toCharArray());
            trustManagerFactory.init(keystore);
        } catch (CertificateException | IOException | NoSuchAlgorithmException | KeyStoreException | UnrecoverableKeyException e) {
            throw new SSLException("Init SSLContext Error: " + e.getMessage(), e);
        } finally {
            if (certFIS != null) {
                try {
                    certFIS.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 初始化
     *
     * @param protocol 协议名称 SSL/TLS
     * @throws SSLException SSL 异常
     */
    private synchronized void init(String protocol) throws SSLException {
        try {
            context = SSLContext.getInstance(protocol);
            if (keyManagerFactory != null && trustManagerFactory != null) {
                context.init(keyManagerFactory.getKeyManagers(), trustManagerFactory.getTrustManagers(), new SecureRandom());
            } else {
                context.init(null, null, null);
            }
        } catch (Exception e) {

            throw new SSLException("Init SSLContext Error: " + e.getMessage(), e);
        }

    }

    private void initBuf() {
        SSLSession sslSession = engine.getSession();
        int netBufferMax = sslSession.getPacketBufferSize();
        int appBufferMax = sslSession.getApplicationBufferSize();
        this.netBuffer = ByteBuffer.allocateDirect(netBufferMax);
        this.appBuffer = ByteBuffer.allocateDirect(appBufferMax);
        this.readBuffer = ByteBuffer.allocateDirect(netBufferMax);
    }

    /**
     * 构造SSLEngine
     *
     * @throws SSLException SSL 异常
     */
    private synchronized void createSSLEngine(String protocol, String ipAddress, int port) throws SSLException {
        init(protocol);
        engine = context.createSSLEngine(ipAddress, port);
        engine.setUseClientMode(true);
        engine.setNeedClientAuth(needClientAuth);
    }

    public void wrap(ByteBuffer in, ByteBuffer out) throws SSLException {
        out.clear();
        in.flip();
        engine.wrap(in, out);
        out.flip();
    }

    public ByteBuffer wrap(ByteBuffer in) throws SSLException {
        netBuffer.clear();
        engine.wrap(in, netBuffer);
        return netBuffer;
    }

    /***
     * 解析服务器数据
     * ! MAC OS JDK1.8 SSLEngine unwrap 方法有异常，待解决
     * @param in
     * @param out
     * @throws SSLException
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public void unwrap(ByteBuffer in, ByteBuffer out) throws SSLException {
        out.clear();
        // TODO: 2017/9/24 SSLENGINE UNWRAP jdk异常。需要寻找解决方案。
        in.flip();
        engine.unwrap(in, out);
    }

    public ByteBuffer unwrap(ByteBuffer in) throws SSLException {
        appBuffer.clear();
        in.flip();
        engine.wrap(in, appBuffer);
        return appBuffer;
    }

    public boolean doHandShake(Session session) throws IOException, ExecutionException, InterruptedException {

        engine.beginHandshake();
        int handShakeCount = 0;
        SSLEngineResult.HandshakeStatus handshakeStatus = engine.getHandshakeStatus();
        while (!handShakeDone && handShakeCount < 20) {
            handShakeCount++;
            switch (handshakeStatus) {
                case NEED_TASK:
                    handshakeStatus = runDelegatedTasks();
                    break;
                case NEED_WRAP:
                    handshakeStatus = doHandShakeWarp(session);
                    break;
                case NEED_UNWRAP:
                    handshakeStatus = doHandShakeUnwarp(session);
                    break;
                case FINISHED:
                    handshakeStatus = engine.getHandshakeStatus();
                    break;
                case NOT_HANDSHAKING:
                    handShakeDone = true;
                    break;
                default:
                    break;
            }
        }
        return handShakeDone;
    }

    /**
     * 处理握手 Unwarp;
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    private SSLEngineResult.HandshakeStatus doHandShakeUnwarp(Session session) throws IOException, ExecutionException, InterruptedException {
        int length;
//        do {
//            readBuffer.clear();
//            length = session.getSocketChannel().read(readBuffer).get();
//            unwrap(readBuffer, appBuffer);
//            session.getReadFromChannelMessage().put(appBuffer.array(), 0, appBuffer.position());
//        } while (length < readBuffer.capacity());
        readBuffer.clear();
        length = session.getSocketChannel().read(readBuffer).get();
        unwrap(readBuffer, netBuffer);
        //如果有 HandShake Task 则执行
        return engine.getHandshakeStatus();
    }

    /**
     * 处理握手 Warp;
     *
     * @return
     * @throws IOException
     * @throws Exception
     */
    private SSLEngineResult.HandshakeStatus doHandShakeWarp(Session session) throws IOException, ExecutionException, InterruptedException {
        wrap(session.getWriteToChannelMessage().getBuffer(), netBuffer);
        session.getSocketChannel().write(netBuffer).get();
        //如果有 HandShake Task 则执行
        return engine.getHandshakeStatus();
    }

    /**
     * 执行委派任务
     *
     * @throws Exception
     */
    private SSLEngineResult.HandshakeStatus runDelegatedTasks() {
        if (engine.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.NEED_TASK) {
            Runnable runnable;
            while ((runnable = engine.getDelegatedTask()) != null) {
                runnable.run();
            }
        }
        return engine.getHandshakeStatus();
    }
}

