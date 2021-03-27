package org.apache.shardingsphere.infra.optimize.planner;

import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.core.Join;
import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexInputRef;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.sql.SqlNode;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.shardingsphere.infra.metadata.schema.ShardingSphereSchema;
import org.apache.shardingsphere.infra.optimize.converter.RelNodeConverter;
import org.apache.shardingsphere.infra.optimize.rel.physical.SSCalc;
import org.apache.shardingsphere.infra.optimize.rel.physical.SSMergeSort;
import org.apache.shardingsphere.infra.optimize.rel.physical.SSScan;
import org.apache.shardingsphere.infra.optimize.schema.AbstractSchemaTest;
import org.apache.shardingsphere.infra.optimize.tools.OptimizerContext;
import org.apache.shardingsphere.infra.optimize.util.ShardingRuleConfigUtil;
import org.apache.shardingsphere.infra.optimize.util.SqlParserFacade;
import org.apache.shardingsphere.infra.parser.ShardingSphereSQLParserEngine;
import org.apache.shardingsphere.sharding.rule.ShardingRule;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

public class DefaultPlannerTest extends AbstractSchemaTest {
    
    RelNodeConverter relNodeConverter;
    
    ShardingSphereSQLParserEngine sqlStatementParserEngine;
    
    ShardingSphereSchema schema;
    
    DefaultPlanner defaultPlanner;
    
    @Before
    public void init() {
        schema = buildSchema();
        
    
        relNodeConverter = new RelNodeConverter("logical_db", schema);
    
        defaultPlanner = new DefaultPlanner();
    
        ShardingRule shardingRule = ShardingRuleConfigUtil.createMaximumShardingRule();
        OptimizerContext.create(shardingRule);
    }
    
    private RelNode parseAndOptimize(String sql) {
        SqlNode sqlNode = SqlParserFacade.parse(sql);
        RelNode relNode = relNodeConverter.validateAndConvert(sqlNode);
    
        return defaultPlanner.getPhysicPlan(relNode);
    }
    
    @Test
    public void testSingleTable() {
        String sql = "select user_id, user_name from t_user ";
        RelNode physicalRelNode = parseAndOptimize(sql);
        Assert.assertNotNull(physicalRelNode);
    }
    
    @Test
    public void testFilterIntoJoin() {
        String sql = "select o1.user_id, o2.status from t_user o1 join t_order_item o2 on "
                + "o1.user_id = o2.user_id where o1.user_name='JACK' and o2.order_item_id > 1024";
        RelNode physicalRelNode = parseAndOptimize(sql);
        
        Assert.assertNotNull(physicalRelNode);
        Assert.assertTrue(physicalRelNode instanceof SSCalc);
        Assert.assertTrue(((SSCalc) physicalRelNode).getInput() instanceof Join);
    
        Join join = (Join) ((SSCalc) physicalRelNode).getInput();
        Assert.assertTrue(join.getCondition() instanceof RexCall);
        RexCall rexCall = (RexCall) join.getCondition();
        List<RexNode> operands = rexCall.getOperands();
        Assert.assertEquals(SqlStdOperatorTable.EQUALS, rexCall.getOperator());
        Assert.assertEquals(2, operands.size());
        Assert.assertTrue(operands.get(0) instanceof RexInputRef);
        Assert.assertTrue(operands.get(1) instanceof RexInputRef);
    }
    
    @Test
    public void testPushdownJoinWithMultiRoute() {
        String sql = "select o1.order_id, o1.order_id, o1.user_id, o2.status from t_order o1 join t_order_item o2 on "
                + "o1.order_id = o2.order_id where o1.status='FINISHED' and o2.order_item_id > 1024 and 1=1 order by "
                + "o1.order_id desc";
    
        SqlNode sqlNode = SqlParserFacade.parse(sql);
        RelNode relNode = relNodeConverter.validateAndConvert(sqlNode);
        
        RelNode physicalRelNode = defaultPlanner.getPhysicPlan(relNode);
        Assert.assertTrue(physicalRelNode instanceof SSMergeSort);
        Assert.assertTrue(((SSMergeSort) physicalRelNode).getInput() instanceof SSScan);
    }
}
