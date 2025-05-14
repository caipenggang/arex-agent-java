package com.makeid.arex.inst.external.call.v1;

import static java.util.Collections.singletonList;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArgument;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.arex.inst.extension.MethodInstrumentation;
import io.arex.inst.extension.TypeInstrumentation;
import io.arex.inst.runtime.context.ArexContext;
import io.arex.inst.runtime.context.ContextManager;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * @author CPG
 * @since 2025-05-14
 */
public class ExternalCallInstrumentation extends TypeInstrumentation {

    @Override
    protected ElementMatcher<TypeDescription> typeMatcher() {
        return named("com.makeid.external.core.spi.ExternalDockingService");
    }

    @Override
    public List<MethodInstrumentation> methodAdvices() {
        MethodInstrumentation executeMethod = new MethodInstrumentation(
                named("call")
                        .and(takesArguments(1))
                        .and(takesArgument(0, named("java.lang.Object"))),
                CallAdvice.class.getName());
        return singletonList(executeMethod);
    }

    public static final class CallAdvice {
        private CallAdvice() {}
        @Advice.OnMethodEnter(suppress = Throwable.class)
        public static void enter(@Advice.Argument(0) Object request,
                @Advice.Local("arexContext") ArexContext arexContext) {
            ArexContext arexContext1 = ContextManager.currentContext();
            if (arexContext != null) {
                if (arexContext.isRecord()) {
                    arexContext.setAttachment("request", request);
                }
            }
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void exit(@Advice.Return(readOnly = false) Object response,
                @Advice.Thrown Throwable throwable) {
            ArexContext arexContext = ContextManager.currentContext();
            if (arexContext != null) {
                // 在录制模式下，设置响应
                if (arexContext.isRecord()) {
                    arexContext.setAttachment("response", response);
                }

                // 在回放模式下，自动 mock 返回值
                if (arexContext.isReplay()) {
                    Object mockResult = arexContext.getAttachment("response");
                    if (mockResult != null) {
                        response = mockResult; // 使用缓存的响应进行回放
                    }
                }

                // 处理异常情况
                if (throwable != null) {
                    arexContext.setAttachment("error", throwable);
                }
            }
        }
    }
}
