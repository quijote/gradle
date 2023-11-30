/*
 * Copyright 2023 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.api.problems.internal;

import org.gradle.api.problems.Problem;
import org.gradle.api.problems.ProblemBuilder;
import org.gradle.api.problems.ProblemBuilderSpec;

import java.util.List;

public class DefaultProblemReporter implements InternalProblemReporter {

    private final ProblemEmitter emitter;
    private final List<ProblemTransformer> transformers;
    private final String namespace;

    public DefaultProblemReporter(ProblemEmitter emitter, List<ProblemTransformer> transformers, String namespace) {
        this.emitter = emitter;
        this.transformers = transformers;
        this.namespace = namespace;
    }

    @Override
    public void reporting(ProblemBuilderSpec action) {
        DefaultBasicProblemBuilder defaultProblemBuilder = createProblemBuilder();
        action.apply(defaultProblemBuilder);
        report(defaultProblemBuilder.build());
    }

    @Override
    public RuntimeException throwing(ProblemBuilderSpec action) {
        DefaultBasicProblemBuilder defaultProblemBuilder = createProblemBuilder();
        action.apply(defaultProblemBuilder);
        Problem problem = defaultProblemBuilder.build();
        throw throwError(problem.getException(), problem);
    }

    public RuntimeException throwError(RuntimeException exception, Problem problem) {
        report(problem);
        throw exception;
    }

    @Override
    public RuntimeException rethrowing(RuntimeException e, ProblemBuilderSpec action) {
        DefaultBasicProblemBuilder defaultProblemBuilder = createProblemBuilder();
        ProblemBuilder problemBuilder = action.apply(defaultProblemBuilder);
        problemBuilder.withException(e);
        throw throwError(e, defaultProblemBuilder.build());
    }

    @Override
    public Problem create(ProblemBuilderSpec action) {
        DefaultBasicProblemBuilder defaultProblemBuilder = createProblemBuilder();
        action.apply(defaultProblemBuilder);
        return defaultProblemBuilder.build();
    }

    @Override
    public DefaultBasicProblemBuilder createProblemBuilder() {
        return new DefaultBasicProblemBuilder(namespace);
    }

    @Override
    public void report(Problem problem) {
        // Transform the problem with all registered transformers
        for (ProblemTransformer transformer : transformers) {
            problem = transformer.transform(problem);
        }

        emitter.emit(problem);
    }
}
