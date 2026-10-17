package com.ovelin.mall.auth.infrastructure.persistence.job;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ovelin.mall.auth.infrastructure.persistence.gateway.IdentityLookupGateway;
import com.ovelin.mall.auth.infrastructure.persistence.mapper.UserIdentityMapper;
import com.ovelin.mall.auth.infrastructure.persistence.po.UserIdentityPO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * 回收崩溃遗留的 PENDING 占位。需要启用 @EnableScheduling。
 * 阈值必须明显大于一次注册/绑定的最长耗时，否则正常流程的占位会被误回收，导致 confirm 失败。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IdentityLookupReclaimJob {

    private final IdentityLookupGateway gateway;
    private final UserIdentityMapper identityMapper;

    @Value("${auth.identity-lookup.reclaim.stale-after:PT10M}")
    private Duration staleAfter;

    @Value("${auth.identity-lookup.reclaim.batch-size:200}")
    private int batchSize;

    @Scheduled(fixedDelayString = "${auth.identity-lookup.reclaim.interval:PT5M}")
    public void reclaim() {
        int handled = gateway.reclaimReservedStale(staleAfter, batchSize, this::identityRowExists);
        if (handled > 0) {
            log.info("identity_lookup 回收 {} 条遗留占位", handled);
        }
    }

    /** 身份行是否已落库：按 user_id（分片键）+ 类型 + 标识查询 */
    private boolean identityRowExists(IdentityLookupGateway.PendingLookup pending) {
        return identityMapper.exists(Wrappers.<UserIdentityPO>lambdaQuery()
                .eq(UserIdentityPO::getUserId, pending.userId().value())
                .eq(UserIdentityPO::getIdentityType, pending.identifier().type().getCode())
                .eq(UserIdentityPO::getIdentifier, pending.identifier().value()));
    }
}