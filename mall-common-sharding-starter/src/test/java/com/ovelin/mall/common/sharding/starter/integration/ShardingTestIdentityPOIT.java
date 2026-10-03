package com.ovelin.mall.common.sharding.starter.integration;

import com.ovelin.mall.common.sharding.core.ov.ShardId;
import com.ovelin.mall.common.sharding.core.ov.ShardedId;
import com.ovelin.mall.common.sharding.starter.integration.po.ShardingTestIdentityPO;
import com.ovelin.mall.common.sharding.starter.test.TestSQLExecutionHook;
import com.ovelin.mall.common.sharding.starter.utils.SequenceShardKeyResolver;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
@SpringBootTest
public class ShardingTestIdentityPOIT extends AbstractShardingTestPOIT {

    @Test
    void hardingTestIdentityTest() throws SQLException {
        List<SequenceShardKeyResolver.ShardKeyRecord> shardKeyRecords = SequenceShardKeyResolver.getSequenceShardKeys(4);
        for (SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord : shardKeyRecords) {
            hardingTestIdentityTest(shardKeyRecord);
        }
    }


    void hardingTestIdentityTest(SequenceShardKeyResolver.ShardKeyRecord shardKeyRecord) throws SQLException {

        String identifier = shardKeyRecord.shardKey();
        ShardId shardId = shardKeyRecord.shardId();
        ShardedId identityShardedId = ShardedId.of(shardId, System.currentTimeMillis());
        ShardedId userShardedId = ShardedId.of(shardId, System.currentTimeMillis());
        long identityId = identityShardedId.value();
        long userId = userShardedId.value();
        ShardingTestIdentityPO identity = new ShardingTestIdentityPO();
        identity.setId(identityId);
        identity.setUserId(userId);
        identity.setIdentityType("test");
        identity.setIdentifier(identifier);

        TestSQLExecutionHook.clear();

        userIdentityMapper.insert(identity);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(1);

        ShardingTestIdentityPO identityPOByIdWithMapper = userIdentityMapper.selectById(identityId);
        assertThat(identityPOByIdWithMapper).isNotNull();
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(2);

        ShardingTestIdentityPO identityPOByIdentifierWithMapper = userIdentityMapper.selectByIdentifier(identifier);
        assertThat(identityPOByIdentifierWithMapper).isNotNull();
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(3);

        JdbcTemplate unshardedJdbcTemplate = unshardedJdbcTemplates.get("identity-ds"+dataSourceIndexResolver.resolve(shardId,2,2));
        int tableIndex = tableIndexResolver.resolve(shardId,2,2);

        ShardingTestIdentityPO identityPOByIdWithUnshardedJdbc = unshardedJdbcTemplate.queryForObject("SELECT id, user_id, identity_type, identifier FROM sharding_test_identity_" + tableIndex + " WHERE id = ?", IDENTITY_ROW_MAPPER, identityId);
        ShardingTestIdentityPO identityPOByIdentifierWithUnshardedJdbc = unshardedJdbcTemplate.queryForObject("SELECT id, user_id, identity_type, identifier FROM sharding_test_identity_" + tableIndex + " WHERE identifier = ?", IDENTITY_ROW_MAPPER, identifier);

        assertThat(identityPOByIdWithUnshardedJdbc).isNotNull();
        assertThat(identityPOByIdentifierWithUnshardedJdbc).isNotNull();

    }
}
