package io.arex.inst.httpclient.webclient.v5;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.arex.agent.bootstrap.model.ComparableVersion;
import io.arex.inst.extension.ModuleDescription;
import io.arex.inst.extension.ModuleInstrumentation;
import io.arex.inst.extension.TypeInstrumentation;
import java.util.List;

@AutoService(ModuleInstrumentation.class)
public class WebClientModuleInstrumentation extends ModuleInstrumentation {
    public WebClientModuleInstrumentation() {
        super("webclient-v5", ModuleDescription.builder().name("spring.webflux")
                .supportFrom(ComparableVersion.of("5.3.8")).build());
    }

    @Override
    public List<TypeInstrumentation> instrumentationTypes() {
        return singletonList(new WebClientInstrumentation());
    }
}
