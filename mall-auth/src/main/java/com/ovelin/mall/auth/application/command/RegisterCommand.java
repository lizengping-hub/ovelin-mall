package com.ovelin.mall.auth.application.command;


import java.time.Instant;

public record RegisterCommand(String type, String identifier, String password, String displayName,
                              String registerSource, boolean addMfaFactor, String verifyCode) {
}
