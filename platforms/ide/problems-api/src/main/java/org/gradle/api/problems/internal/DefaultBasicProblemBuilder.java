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

import org.gradle.api.problems.DocLink;
import org.gradle.api.problems.Problem;
import org.gradle.api.problems.ProblemCategory;
import org.gradle.api.problems.Severity;
import org.gradle.api.problems.locations.FileLocation;
import org.gradle.api.problems.locations.PluginIdLocation;
import org.gradle.api.problems.locations.ProblemLocation;
import org.gradle.api.problems.locations.TaskPathLocation;
import org.gradle.internal.operations.BuildOperationRef;
import org.gradle.internal.operations.CurrentBuildOperationRef;
import org.gradle.internal.operations.OperationIdentifier;
import org.gradle.util.Path;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DefaultBasicProblemBuilder implements BasicProblemBuilder {

    private final String namespace;
    private String label;
    private ProblemCategory problemCategory;
    private Severity severity;
    private List<ProblemLocation> locations;
    private String details;
    private DocLink docLink;
    private List<String> solutions;
    private RuntimeException exception;
    private final Map<String, Object> additionalData;
    private boolean collectLocation = false;
    @Nullable private OperationIdentifier currentOperationId = null;

    public DefaultBasicProblemBuilder(Problem problem) {
        this.label = problem.getLabel();
        this.problemCategory = problem.getProblemCategory();
        this.severity = problem.getSeverity();
        this.locations = new ArrayList<ProblemLocation>(problem.getLocations());
        this.details = problem.getDetails();
        this.docLink = problem.getDocumentationLink();
        this.solutions = new ArrayList<String>(problem.getSolutions());
        this.exception = problem.getException();
        this.additionalData = new HashMap<String, Object>(problem.getAdditionalData());

        if (problem instanceof DefaultProblem) {
            this.currentOperationId = ((DefaultProblem) problem).getBuildOperationId();
        }
        this.namespace = problem.getProblemCategory().getNamespace();
    }

    public DefaultBasicProblemBuilder(String namespace) {
        this.namespace = namespace;
        this.locations = new ArrayList<ProblemLocation>();
        this.additionalData = new HashMap<String, Object>();
    }

    @Override
    public Problem build() {
        // TODO (donat) can we use the fields directly in this method?
        // TODO (donat) add test coverage
        if (label == null) {
            throw new IllegalStateException("Label must be specified");
        } else if (problemCategory == null) {
            throw new IllegalStateException("Category must be specified");
        }
        return new DefaultProblem(
            label,
            getSeverity(getSeverity()),
            getLocations(),
            getDocLink(),
            getDetails(),
            getSolutions(),
            getExceptionForProblemInstantiation(), // TODO: don't create exception if already reported often
            getProblemCategory(),
            getAdditionalData(),
            getCurrentOperationId()
        );
    }

    @Nullable
    public OperationIdentifier getCurrentOperationId() {
        if (currentOperationId != null) {
            // If we have a carried over operation id, use it
            return currentOperationId;
        } else {
            // Otherwise, try to get the current operation id
            BuildOperationRef buildOperationRef = CurrentBuildOperationRef.instance().get();
            if (buildOperationRef == null) {
                return null;
            } else {
                return buildOperationRef.getId();
            }
        }
    }

    public RuntimeException getExceptionForProblemInstantiation() {
        return getException() == null && isCollectLocation() ? new RuntimeException() : getException();
    }

    protected Severity getSeverity(@Nullable Severity severity) {
        if (severity != null) {
            return severity;
        }
        return getSeverity();
    }

    protected Severity getSeverity() {
        if (this.severity == null) {
            return Severity.WARNING;
        }
        return this.severity;
    }

    @Override
    public BasicProblemBuilder label(String label, Object... args) {
        this.label = String.format(label, args);
        return this;
    }

    @Override
    public BasicProblemBuilder severity(Severity severity) {
        this.severity = severity;
        return this;
    }

    public BasicProblemBuilder taskPathLocation(Path taskPath) {
        this.getLocations().add(new TaskPathLocation(taskPath));
        return this;
    }

    public BasicProblemBuilder location(String path, @javax.annotation.Nullable Integer line) {
        location(path, line, null);
        return this;
    }

    public BasicProblemBuilder location(String path, @javax.annotation.Nullable Integer line, @javax.annotation.Nullable Integer column) {
        this.getLocations().add(new FileLocation(path, line, column, 0));
        return this;
    }

    @Override
    public BasicProblemBuilder fileLocation(String path, @javax.annotation.Nullable Integer line, @javax.annotation.Nullable Integer column, @javax.annotation.Nullable Integer length) {
        this.getLocations().add(new FileLocation(path, line, column, length));
        return this;
    }

    @Override
    public BasicProblemBuilder pluginLocation(String pluginId) {
        this.getLocations().add(new PluginIdLocation(pluginId));
        return this;
    }

    @Override
    public BasicProblemBuilder stackLocation() {
        this.collectLocation = true;
        return this;
    }

    @Override
    public BasicProblemBuilder details(String details) {
        this.details = details;
        return this;
    }

    @Override
    public BasicProblemBuilder documentedAt(DocLink doc) {
        this.docLink = doc;
        return this;
    }

    @Override
    public BasicProblemBuilder documentedAt(String url) {
        this.docLink = new DefaultDocLink(url);
        return this;
    }

    @Override
    public BasicProblemBuilder category(String category, String... details) {
        this.problemCategory = DefaultProblemCategory.create(namespace, category, details);
        return this;
    }

    @Override
    public BasicProblemBuilder solution(@Nullable String solution) {
        if (this.getSolutions() == null) {
            this.solutions = new ArrayList<String>();
        }
        this.getSolutions().add(solution);
        return this;
    }

    @Override
    public BasicProblemBuilder additionalData(String key, Object value) {
        validateAdditionalDataValueType(value);
        this.getAdditionalData().put(key, value);
        return this;
    }

    private void validateAdditionalDataValueType(Object value) {
        if (!(value instanceof String)) {
            throw new RuntimeException("ProblemBuilder.additionalData() supports values of type String, but " + value.getClass().getName() + " as given.");
        }
    }

    @Override
    public BasicProblemBuilder withException(RuntimeException e) {
        this.exception = e;
        return this;
    }

    @Nullable
    RuntimeException getException() {
        return exception;
    }

    protected String getLabel() {
        return label;
    }

    protected ProblemCategory getProblemCategory() {
        return problemCategory;
    }

    protected List<ProblemLocation> getLocations() {
        return locations;
    }

    protected String getDetails() {
        return details;
    }

    protected DocLink getDocLink() {
        return docLink;
    }

    protected List<String> getSolutions() {
        return solutions;
    }

    protected Map<String, Object> getAdditionalData() {
        return additionalData;
    }

    protected boolean isCollectLocation() {
        return collectLocation;
    }
}
