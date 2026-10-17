package com.ovelin.mall.auth.application.command;

import com.ovelin.mall.auth.domain.model.enums.IdentityType;

public record RegisterCommand(IdentityType type, String identifier, String password,
                              String displayName, String registerSource) {
}
