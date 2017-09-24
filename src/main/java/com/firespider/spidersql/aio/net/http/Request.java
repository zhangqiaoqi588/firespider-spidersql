package com.firespider.spidersql.aio.net.http;


import java.util.Map;
import java.util.TreeMap;

/**
 * Created by stone on 2017/9/16.
 */
public class Request extends HttpMessage {
//    private HttpMessage message;

    private Map<String, String> params;

    private URL url;

    private String httpVersion = "HTTP/1.1";

    private String method = "GET";

    private final String[] userAgents = new String[]{
            "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.1; SV1; AcooBrowser; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
            "Mozilla/4.0 (compatible; MSIE 7.0; Windows NT 6.0; Acoo Browser; SLCC1; .NET CLR 2.0.50727; Media Center PC 5.0; .NET CLR 3.0.04506)",
            "Mozilla/4.0 (compatible; MSIE 7.0; AOL 9.5; AOLBuild 4337.35; Windows NT 5.1; .NET CLR 1.1.4322; .NET CLR 2.0.50727)",
            "Mozilla/5.0 (Windows; U; MSIE 9.0; Windows NT 9.0; en-US)",
            "Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Win64; x64; Trident/5.0; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 2.0.50727; Media Center PC 6.0)",
            "Mozilla/5.0 (compatible; MSIE 8.0; Windows NT 6.0; Trident/4.0; WOW64; Trident/4.0; SLCC2; .NET CLR 2.0.50727; .NET CLR 3.5.30729; .NET CLR 3.0.30729; .NET CLR 1.0.3705; .NET CLR 1.1.4322)",
            "Mozilla/4.0 (compatible; MSIE 7.0b; Windows NT 5.2; .NET CLR 1.1.4322; .NET CLR 2.0.50727; InfoPath.2; .NET CLR 3.0.04506.30)",
            "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN) AppleWebKit/523.15 (KHTML, like Gecko, Safari/419.3) Arora/0.3 (Change: 287 c9dfb30)",
            "Mozilla/5.0 (X11; U; Linux; en-US) AppleWebKit/527+ (KHTML, like Gecko, Safari/419.3) Arora/0.6",
            "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US; rv:1.8.1.2pre) Gecko/20070215 K-Ninja/2.1.1",
            "Mozilla/5.0 (Windows; U; Windows NT 5.1; zh-CN; rv:1.9) Gecko/20080705 Firefox/3.0 Kapiko/3.0",
            "Mozilla/5.0 (X11; Linux i686; U;) Gecko/20070322 Kazehakase/0.4.5",
            "Mozilla/5.0 (X11; U; Linux i686; en-US; rv:1.9.0.8) Gecko Fedora/1.9.0.8-1.fc10 Kazehakase/0.5.6",
            "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.11 (KHTML, like Gecko) Chrome/17.0.963.56 Safari/535.11",
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_7_3) AppleWebKit/535.20 (KHTML, like Gecko) Chrome/19.0.1036.7 Safari/535.20",
            "Opera/9.80 (Macintosh; Intel Mac OS X 10.6.8; U; fr) Presto/2.9.168 Version/11.52"
    };

    public Request(String url) {
        this(url, null);

    }

    public Request(String url, Map<String, String> header) {
        super();
        this.url = new URL(url);
        this.setStatusLine(parseStatusLine());
        this.setHeader(parseHeader(header));
        this.setBody(null);
        this.reset();
    }

    public Map<String, String> getHeader() {
        return super.getHeader();
    }

    private String parseStatusLine() {
        return this.method + " " + this.url.getPath() + " " + this.httpVersion;
    }

    private Map<String, String> parseHeader(Map<String, String> header) {
        Map<String, String> resMap = new TreeMap<>();
        resMap.put("Host", this.url.getHost());
        resMap.put("Connection", "Keep-Alive");
        resMap.put("User-Agent", userAgents[(int) (System.currentTimeMillis() & (userAgents.length - 1))]);
        resMap.put("Accept-Language", "en-us");
        if (header != null)
            resMap.putAll(header);
        return resMap;
    }

    private String parseBody(Map<String, String> params) {
        // TODO: 2017/9/23 parseBody by content-type 
        return "";
    }

    public String getHost() {
        return this.url.getHost();
    }

    public int getPort() {
        return this.url.getPort();
    }

    public String getProtocol() {
        return this.url.getProtocol();
    }

    public String getUrl() {
        return url.getUrl();
    }
}
