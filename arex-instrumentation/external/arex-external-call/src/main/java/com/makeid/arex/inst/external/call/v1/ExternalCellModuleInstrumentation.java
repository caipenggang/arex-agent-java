package com.makeid.arex.inst.external.call.v1;

import static java.util.Collections.singletonList;

import com.google.auto.service.AutoService;
import io.arex.agent.bootstrap.model.ComparableVersion;
import io.arex.inst.extension.ModuleDescription;
import io.arex.inst.extension.ModuleInstrumentation;
import io.arex.inst.extension.TypeInstrumentation;
import java.util.List;

@AutoService(ModuleInstrumentation.class)
public class ExternalCellModuleInstrumentation extends ModuleInstrumentation {
    public ExternalCellModuleInstrumentation() {
        super("webclient-v2", ModuleDescription.builder()
                .supportFrom(ComparableVersion.of("5.2.0")).build());
    }

    @Override
    public List<TypeInstrumentation> instrumentationTypes() {
        return singletonList(new ExternalCallInstrumentation());
    }
}