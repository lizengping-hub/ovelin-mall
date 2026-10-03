package com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.it;

import com.ovelin.mall.common.sharding.starter.test.TestSQLExecutionHook;
import com.ovelin.mall.id.generator.starter.domain.module.Segment;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.mapper.IdSequenceMapper;
import com.ovelin.mall.id.generator.starter.infrastructure.persistence.po.IdSequencePO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;;

@SpringBootTest
public class IdSequenceMapperIT {
    @Autowired
    private IdSequenceMapper idSequenceMapper;
    @Autowired
    JdbcTemplate jdbcTemplate;
    @BeforeEach
    public void setUp() {
        jdbcTemplate.execute("TRUNCATE TABLE id_sequence");
    }
    @Test
    public void testSelectBySequenceNameAndShardId() {
        TestSQLExecutionHook.clear();
        var sequenceName = "test_sequence";
        var shardId = 1;
        idSequenceMapper.insertIfAbsent(sequenceName, shardId);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(1);
        IdSequencePO sequencePO = idSequenceMapper.selectBySequenceNameAndShardId(sequenceName, shardId);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(2);
        Segment segment = Segment.fromStartAndSize(
                sequencePO.nextValue(),
               100);
        int updatedRows = idSequenceMapper.advance(sequenceName, shardId, 100);
        assertThat(updatedRows).isEqualTo(1);
        assertThat(TestSQLExecutionHook.getActualSqls().size()).isEqualTo(3);

    }
}
