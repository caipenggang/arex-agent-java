package io.arex.inst.authentication.shiro;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

import io.arex.inst.extension.MethodInstrumentation;
import io.arex.inst.extension.TypeInstrumentation;
import io.arex.inst.runtime.context.ContextManager;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.asm.Advice;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * ShiroDelegatingSubjectInstrumentation
 */
public class ShiroDelegatingSubjectInstrumentation extends TypeInstrumentation {

    @Override
    public ElementMatcher<TypeDescription> typeMatcher() {
        return named("org.apache.shiro.subject.support.DelegatingSubject")
                .or(named("org.apache.shiro.web.subject.support.WebDelegatingSubject"));
    }

    @Override
    public List<MethodInstrumentation> methodAdvices() {
        List<MethodInstrumentation> methods = new ArrayList<>();
        methods.add(new MethodInstrumentation(
                named("assertAuthzCheckPossible"),
                AssertAuthzCheckPossibleAdvice.class.getName()));
        // 新增 getPrincipal() 拦截
        methods.add(new MethodInstrumentation(
                named("getPrincipal").and(takesArguments(0)),  // 无参方法
                GetPrincipalAdvice.class.getName()));
        return methods;
    }

    public static class AssertAuthzCheckPossibleAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, suppress = Throwable.class)
        public static boolean onEnter() {
            return ContextManager.needReplay();
        }
    }

    public static class GetPrincipalAdvice {
        @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class, suppress = Throwable.class)
        public static boolean onEnter() {
            return ContextManager.needReplay();
        }

        @Advice.OnMethodExit(onThrowable = Throwable.class, suppress = Throwable.class)
        public static void onExit(@Advice.Return(readOnly = false) Object result) {
            if (ContextManager.needReplay()) {
                // mock replay
                result = ContextManager.currentContext().getAttachment("shiro.principal");
            } else if (ContextManager.needRecord()) {
                // record time
                ContextManager.setAttachment("shiro.principal", result);
            }
        }
    }
}
