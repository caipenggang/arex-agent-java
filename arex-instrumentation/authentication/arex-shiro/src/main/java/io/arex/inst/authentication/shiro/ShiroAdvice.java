package io.arex.inst.authentication.shiro;

import io.arex.agent.bootstrap.model.MockResult;
import io.arex.agent.bootstrap.model.Mocker;
import io.arex.inst.runtime.context.ContextManager;
import io.arex.inst.runtime.serializer.Serializer;
import io.arex.inst.runtime.util.MockUtils;

/**
 * @author CPG
 * @since 2025-07-09
 */
public class ShiroAdvice {
    private static final String OPERATION_NAME = "org.apache.shiro.subject.support.DelegatingSubject#getPrincipal";

    public static void record(Object result) {
        if (!ContextManager.needRecord()) return;

        Mocker mocker = buildMocker();
        mocker.setNeedMerge(true);
        mocker.getTargetResponse().setBody(Serializer.serialize(result));
        if (result != null) {
            mocker.getTargetResponse().setType(result.getClass().getName());
        }
        MockUtils.recordMocker(mocker);
    }

    public static MockResult replay() {
        if (!ContextManager.needReplay()) return null;

        Mocker mocker = buildMocker();
        Object result = MockUtils.replayBody(mocker);
        return MockResult.success(result);
    }

    private static Mocker buildMocker() {
        return MockUtils.createRedis(OPERATION_NAME);
    }
}
