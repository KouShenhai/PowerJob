package com.github.kfcfans.powerjob.worker.core.processor;

import com.github.kfcfans.powerjob.common.WorkflowContextConstant;
import com.github.kfcfans.powerjob.common.utils.JsonUtils;
import com.github.kfcfans.powerjob.worker.OhMyWorker;
import com.google.common.collect.Maps;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * 工作流上下文
 *
 * @author Echo009
 * @since 2021/2/19
 */
@Getter
@Slf4j
public class WorkflowContext {
    /**
     * 任务实例 ID
     */
    private final Long instanceId;
    /**
     * 当前工作流上下文数据
     * 这里的 data 实际上等价于 {@link TaskContext} 中的 instanceParams
     */
    private final Map<String, String> data = Maps.newHashMap();
    /**
     * 追加的上下文信息
     */
    private final Map<String, String> appendedContextData = Maps.newConcurrentMap();

    @SuppressWarnings({"rawtypes", "unchecked"})
    public WorkflowContext(Long instanceId, String data) {
        this.instanceId = instanceId;
        try {
            Map originMap = JsonUtils.parseObject(data, Map.class);
            originMap.forEach((k, v) -> this.data.put(String.valueOf(k), v == null ? null : String.valueOf(v)));
        } catch (Exception exception) {
            log.warn("[WorkflowContext-{}] parse workflow context failed, {}", instanceId, exception);
        }
    }

    /**
     * 获取工作流上下文 (MAP)，本质上是将 data 解析成 MAP
     * 初始参数的 key 为 {@link WorkflowContextConstant#CONTEXT_INIT_PARAMS_KEY}
     * 注意，在没有传递初始参数时，通过 CONTEXT_INIT_PARAMS_KEY 获取到的是 null
     *
     * @return 工作流上下文
     * @author Echo009
     * @since 2021/02/04
     */
    public Map<String, String> fetchWorkflowContext() {
        return data;
    }

    /**
     * 往工作流上下文添加数据
     * 注意：如果 key 在当前上下文中已存在，那么会直接覆盖
     */
    public void appendData2WfContext(String key, Object value) {
        String finalValue;
        try {
            // 先判断当前上下文大小是否超出限制
            final int sizeThreshold = OhMyWorker.getConfig().getMaxAppendedWfContextSize();
            if (appendedContextData.size() >= sizeThreshold) {
                log.warn("[WorkflowContext-{}] appended workflow context data size must be lesser than {}, current appended workflow context data(key={}) will be ignored!", instanceId, sizeThreshold, key);
                return;
            }
            finalValue = JsonUtils.toJSONStringUnsafe(value);
            final int lengthThreshold = OhMyWorker.getConfig().getMaxAppendedWfContextLength();
            // 判断 key & value 是否超长度限制
            if (key.length() > lengthThreshold || finalValue.length() > lengthThreshold) {
                log.warn("[WorkflowContext-{}] appended workflow context data length must be shorter than {}, current appended workflow context data(key={}) will be ignored!", instanceId, lengthThreshold, key);
                return;
            }
        } catch (Exception e) {
            log.warn("[WorkflowContext-{}] fail to append data to workflow context, key : {}", instanceId, key);
            return;
        }
        appendedContextData.put(key, JsonUtils.toJSONString(value));
    }


}