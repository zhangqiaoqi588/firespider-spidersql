package com.firespider.spidersql.action;

import com.firespider.spidersql.action.model.Param;
import com.firespider.spidersql.lang.json.GenJsonElement;

import java.io.IOException;
import java.nio.channels.CompletionHandler;

/**
 * Created by xiaotong.shi on 2017/9/14.
 */
public class SaveAction extends Action {
    SaveAction(Integer id, Param param, CompletionHandler<GenJsonElement, Integer> handler) {
        super(id, param, handler);
    }

    @Override
    void handle() throws IOException, InterruptedException {

    }
}
