package com.firespider.spidersql.action;

import com.firespider.spidersql.action.model.ScanParam;
import com.firespider.spidersql.lang.json.GenJsonArray;
import com.firespider.spidersql.lang.json.GenJsonElement;
import com.firespider.spidersql.lang.json.GenJsonNull;
import com.firespider.spidersql.lang.json.GenJsonObject;
import com.firespider.spidersql.action.model.GetParam;
import com.firespider.spidersql.queue.QueueManager;

import java.io.IOException;
import java.nio.channels.CompletionHandler;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.*;

public class ActionManager {
    //变量与ID的映射关系，需要保序
    private final Map<String, Integer> varIdMap = new LinkedHashMap<>();
    //ID与数据结果映射关系，线程安全
    private final Map<Integer, GenJsonElement> idData = new ConcurrentHashMap<>();

    private final ActionChecker checker;

    private final ExecutorService service;

    private final QueueManager queueManager;

    public ActionManager(int threadNum) {
        this.service = Executors.newFixedThreadPool(threadNum);
        this.checker = new ActionChecker();
        this.queueManager = new QueueManager();
    }

    public enum TYPE {
        GET, SCAN, DESC, PRINT, SAVE;
    }


    /***
     * 接收并执行动作
     * @param element
     * @param type
     * @return
     */
    public Integer accept(GenJsonElement element, TYPE type) {
        Integer id = 0;
        if (!checker.check(element, type)) {
            return id;
        }
        // TODO: 2017/9/27 完善剩余action内容
        try {
            switch (type) {
                case GET:
                    id = acceptGet((GenJsonObject) element);
                    break;
                case SCAN:
                    id = acceptScan((GenJsonObject) element);
                    break;
                case DESC:
                    break;
                case PRINT:
                    acceptPrint(element);
                    break;
                case SAVE:
                    break;
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println(type + " init error!");
        }
        return id;
    }

    private Integer acceptGet(GenJsonObject element) throws IOException {
        Integer id = element.hashCode();
        if (idData.containsKey(id)) {
            return id;
        }
        GenJsonArray value = new GenJsonArray();
        // TODO: 2017/9/27 确认是否会出现回调地狱
        // 在handler中对value赋值，该块堆内存会在另外N份线程中做add操作
        Action action = new GetAction(id, new GetParam(element), new CompletionHandler<GenJsonElement, Boolean>() {
            @Override
            public void completed(GenJsonElement result, Boolean attachment) {
                if (attachment) {
                    if (result instanceof GenJsonArray) {
                        value.addAll((GenJsonArray) result);
                    } else {
                        value.add(result);
                    }
                } else {
                    value.add(GenJsonNull.INSTANCE);
                }
            }

            @Override
            public void failed(Throwable exc, Boolean attachment) {
                value.add(GenJsonNull.INSTANCE);
            }
        });
        // 将value的指针与查询ID绑定
        idData.put(id, value);
        service.execute(action);
        return id;
    }

    private Integer acceptScan(GenJsonObject element) throws IOException {
        Integer id = element.hashCode();
        GenJsonArray value = new GenJsonArray();
        Action action = new ScanAction(id, new ScanParam(element), new CompletionHandler<GenJsonElement, Boolean>() {
            @Override
            public void completed(GenJsonElement result, Boolean attachment) {
                if (attachment) {
                    System.out.println(result);
                    value.add(result);
                }
            }

            @Override
            public void failed(Throwable exc, Boolean attachment) {

            }
        });
        idData.put(id, value);
        service.execute(action);
        return id;
    }

    private void acceptPrint(GenJsonElement element) {
        System.out.println(element.toString());
    }

    /***
     * 绑定变量名与动作ID
     * @param var
     * @param id
     */
    public void bind(String var, Integer id) {
        varIdMap.put(var, id);
    }

    /***
     * 等待线程池中任务全部执行完毕
     * @param timeout
     * @return
     */
    public boolean await(int timeout) {
        try {
            service.shutdown();
            if (!service.isTerminated()) {
                service.awaitTermination(timeout, TimeUnit.SECONDS);
            }
            return true;
        } catch (InterruptedException e) {
            e.printStackTrace();
            service.shutdownNow();
            return false;
        }
    }

    public Map<String, GenJsonElement> getAll() {
        Map<String, GenJsonElement> resMap = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> map : varIdMap.entrySet()) {
            resMap.put(map.getKey(), idData.get(map.getValue()));
        }
        return resMap;
    }

    public void clear() {
        // TODO: 2017/9/27 清空service 避免内存泄露
        varIdMap.clear();
        idData.clear();
    }

    public void close() {
        service.shutdownNow();
    }
}
