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

package org.gradle.caching.internal.operations;

/**
 * Marks that the remote build cache has been disabled due to a failure.
 * <p>
 * There might be multiple disabled events from build cache operations happening in parallel.
 *
 * @since 8.5
 */
public interface BuildCacheRemoteDisableProgressDetails {
    /**
     * The build path of the configuration of the remote build cache.
     */
    String getBuildPath();

    /**
     * The cache key.
     */
    String getCacheKey();

    /**
     * The failure that caused the build cache to be disabled.
     */
    Throwable getFailure();

    /**
     * The type of operation that had the failure.
     */
    BuildCacheOperationType getOperationType();

    /**
     * The type of build cache operation.
     */
    enum BuildCacheOperationType {
        LOAD,
        STORE
    }
}
