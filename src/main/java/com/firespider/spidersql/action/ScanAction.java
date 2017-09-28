package com.firespider.spidersql.action;

import com.firespider.spidersql.action.model.ScanParam;
import com.firespider.spidersql.aio.net.http.HttpAsyncClient;
import com.firespider.spidersql.lang.json.GenJsonElement;
import com.firespider.spidersql.lang.json.GenJsonObject;
import com.sun.xml.internal.ws.util.StringUtils;

import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Created by xiaotong.shi on 2017/9/14.
 */
public class ScanAction extends Action {
    private final HttpAsyncClient client;

    private final static int thread = 100;

    public ScanAction(Integer id, ScanParam param, CompletionHandler<GenJsonElement, Integer> handler) throws IOException {
        super(id, param, handler);
        this.client = new HttpAsyncClient(thread);
    }

    public void handle() throws IOException, InterruptedException {
        Set<String> hosts = parseHosts(((ScanParam)param).getHost());
        Set<String> ports = parsePorts(((ScanParam)param).getPort());
        CountDownLatch latch = new CountDownLatch(hosts.size() * ports.size());
        for (String host : hosts) {
            for (String port : ports) {
                client.handleScanPort(host, port, new CompletionHandler<Boolean, String[]>() {
                    @Override
                    public void completed(Boolean result, String[] attachment) {
                        GenJsonObject gen = new GenJsonObject();
                        gen.addPrimitive("ip", attachment[0]);
                        gen.addPrimitive("host", attachment[1]);
                        gen.addPrimitive("port", attachment[2]);
                        if (result) {
                            gen.addPrimitive("result", true);
                        } else {
                            gen.addPrimitive("result", false);
                        }
                        handler.completed(gen, attachment.hashCode());
                        latch.countDown();
                    }

                    @Override
                    public void failed(Throwable exc, String[] attachment) {
                        GenJsonObject gen = new GenJsonObject();
                        gen.addPrimitive("ip", attachment[0]);
                        gen.addPrimitive("host", attachment[1]);
                        gen.addPrimitive("port", attachment[2]);
                        gen.addPrimitive("result", false);
                        handler.completed(gen, attachment.hashCode());
                        latch.countDown();
                    }
                });
            }
        }
        latch.await();
        client.close();
    }

    Set<String> parseHosts(String host) {
        Set<String> res = new LinkedHashSet<>();
        for(String h : host.split(",")){
            if(Character.isDigit(h.getBytes()[0])){
                String[] segments = h.split("\\.");
                Set<String> segments1 = split(segments[0]);
                Set<String> segments2 = split(segments[1]);
                Set<String> segments3 = split(segments[2]);
                Set<String> segments4 = split(segments[3]);
                for (String segment1 : segments1) {
                    for (String segment2 : segments2) {
                        for (String segment3 : segments3) {
                            for (String segment4 : segments4) {
                                res.add(String.join(".", segment1, segment2, segment3, segment4));
                            }
                        }
                    }
                }
            }else{
                res.add(h);
            }
        }
        return res;
    }

    Set<String> parsePorts(String port) {
        Set<String> res = new HashSet<>();
        for(String p : port.split(",")){
            res.addAll(split(p));
        }
        return res;
    }

    Set<String> split(String str) {
        String[] nums;
        Set<String> res = new HashSet<>();
        if (str.contains("-")) {
            nums = str.split("-");
        } else if (str.contains("/")) {
            nums = str.split("/");
        } else {
            res.add(str);
            return res;
        }
        int start = Integer.parseInt(nums[0]);
        int end = Integer.parseInt(nums[1]);
        for (int i = start; i <= end; i++) {
            res.add(String.valueOf(i));
        }
        return res;
    }


}
