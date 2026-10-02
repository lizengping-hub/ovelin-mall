package com.ovelin.mall.common.test.environment.support;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

public final class DockerTestEnvironmentExtension implements BeforeAllCallback {

    @Override
    public void beforeAll(ExtensionContext context) {
        TestEnvironment.resetOnce();
    }
}
