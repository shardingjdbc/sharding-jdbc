/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.core.optimize.statement.impl;

import com.google.common.base.Optional;
import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.ToString;
import org.apache.shardingsphere.core.metadata.table.TableMetas;
import org.apache.shardingsphere.core.optimize.segment.select.groupby.GroupByContext;
import org.apache.shardingsphere.core.optimize.segment.select.groupby.engine.GroupByContextEngine;
import org.apache.shardingsphere.core.optimize.segment.select.item.impl.AggregationSelectItem;
import org.apache.shardingsphere.core.optimize.segment.select.item.SelectItem;
import org.apache.shardingsphere.core.optimize.segment.select.item.SelectItems;
import org.apache.shardingsphere.core.optimize.segment.select.item.engine.SelectItemsEngine;
import org.apache.shardingsphere.core.optimize.segment.select.orderby.OrderByContext;
import org.apache.shardingsphere.core.optimize.segment.select.orderby.OrderByItem;
import org.apache.shardingsphere.core.optimize.segment.select.orderby.engine.OrderByContextEngine;
import org.apache.shardingsphere.core.optimize.segment.select.pagination.Pagination;
import org.apache.shardingsphere.core.optimize.segment.select.pagination.engine.PaginationEngine;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.ColumnOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.ExpressionOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.IndexOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.order.item.TextOrderByItemSegment;
import org.apache.shardingsphere.core.parse.sql.segment.dml.predicate.SubqueryPredicateSegment;
import org.apache.shardingsphere.core.parse.sql.statement.dml.SelectStatement;
import org.apache.shardingsphere.core.parse.util.SQLUtil;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Select SQL statement context.
 *
 * @author zhangliang
 */
@Getter
@ToString(callSuper = true)
public final class SelectSQLStatementContext extends CommonSQLStatementContext {
    
    private final GroupByContext groupByContext;
    
    private final OrderByContext orderByContext;
    
    private final SelectItems selectItems;
    
    private final Pagination pagination;
    
    private final boolean containsSubquery;
    
    // TODO to be remove, for test case only
    public SelectSQLStatementContext(final SelectStatement sqlStatement, 
                                     final GroupByContext groupByContext, final OrderByContext orderByContext, final SelectItems selectItems, final Pagination pagination) {
        super(sqlStatement);
        this.groupByContext = groupByContext;
        this.orderByContext = orderByContext;
        this.selectItems = selectItems;
        this.pagination = pagination;
        containsSubquery = containsSubquery();
    }
    
    public SelectSQLStatementContext(final TableMetas tableMetas, final String sql, final List<Object> parameters, final SelectStatement sqlStatement) {
        super(sqlStatement);
        groupByContext = new GroupByContextEngine().createGroupByContext(sqlStatement);
        orderByContext = new OrderByContextEngine().createOrderBy(sqlStatement, groupByContext);
        selectItems = new SelectItemsEngine(tableMetas).createSelectItems(sql, sqlStatement, groupByContext, orderByContext);
        pagination = new PaginationEngine().createPagination(sqlStatement, selectItems, parameters);
        containsSubquery = containsSubquery();
    }
    
    private boolean containsSubquery() {
        Collection<SubqueryPredicateSegment> subqueryPredicateSegments = getSqlStatement().findSQLSegments(SubqueryPredicateSegment.class);
        for (SubqueryPredicateSegment each : subqueryPredicateSegments) {
            if (!each.getAndPredicates().isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Set index for select items.
     *
     * @param columnLabelIndexMap map for column label and index
     */
    public void setIndexForItems(final Map<String, Integer> columnLabelIndexMap) {
        setIndexForAggregationItem(columnLabelIndexMap);
        setIndexForOrderItem(columnLabelIndexMap, orderByContext.getItems());
        setIndexForOrderItem(columnLabelIndexMap, groupByContext.getItems());
    }
    
    private void setIndexForAggregationItem(final Map<String, Integer> columnLabelIndexMap) {
        for (AggregationSelectItem each : selectItems.getAggregationSelectItems()) {
            Preconditions.checkState(columnLabelIndexMap.containsKey(each.getColumnLabel()), "Can't find index: %s, please add alias for aggregate selections", each);
            each.setIndex(columnLabelIndexMap.get(each.getColumnLabel()));
            for (AggregationSelectItem derived : each.getDerivedAggregationItems()) {
                Preconditions.checkState(columnLabelIndexMap.containsKey(derived.getColumnLabel()), "Can't find index: %s", derived);
                derived.setIndex(columnLabelIndexMap.get(derived.getColumnLabel()));
            }
        }
    }
    
    private void setIndexForOrderItem(final Map<String, Integer> columnLabelIndexMap, final Collection<OrderByItem> orderByItems) {
        for (OrderByItem each : orderByItems) {
            if (each.getSegment() instanceof IndexOrderByItemSegment) {
                each.setIndex(((IndexOrderByItemSegment) each.getSegment()).getColumnIndex());
                continue;
            }
            if (each.getSegment() instanceof ColumnOrderByItemSegment && ((ColumnOrderByItemSegment) each.getSegment()).getColumn().getOwner().isPresent()) {
                Optional<Integer> itemIndex = selectItems.findItemIndex(((ColumnOrderByItemSegment) each.getSegment()).getText());
                if (itemIndex.isPresent()) {
                    each.setIndex(itemIndex.get());
                    continue;
                }
            }
            Optional<String> alias = getAlias(((TextOrderByItemSegment) each.getSegment()).getText());
            String columnLabel = alias.isPresent() ? alias.get() : getOrderItemText((TextOrderByItemSegment) each.getSegment());
            Preconditions.checkState(columnLabelIndexMap.containsKey(columnLabel), "Can't find index: %s", each);
            if (columnLabelIndexMap.containsKey(columnLabel)) {
                each.setIndex(columnLabelIndexMap.get(columnLabel));
            }
        }
    }
    
    private Optional<String> getAlias(final String name) {
        if (selectItems.isUnqualifiedShorthandItem()) {
            return Optional.absent();
        }
        String rawName = SQLUtil.getExactlyValue(name);
        for (SelectItem each : selectItems.getItems()) {
            if (SQLUtil.getExactlyExpression(rawName).equalsIgnoreCase(SQLUtil.getExactlyExpression(SQLUtil.getExactlyValue(each.getExpression())))) {
                return each.getAlias();
            }
            if (rawName.equalsIgnoreCase(each.getAlias().orNull())) {
                return Optional.of(rawName);
            }
        }
        return Optional.absent();
    }
    
    private String getOrderItemText(final TextOrderByItemSegment orderByItemSegment) {
        return orderByItemSegment instanceof ColumnOrderByItemSegment
                ? ((ColumnOrderByItemSegment) orderByItemSegment).getColumn().getName() : ((ExpressionOrderByItemSegment) orderByItemSegment).getExpression();
    }
    
    /**
     * Get column labels.
     *
     * @param tableMetas table metas
     * @return column labels
     */
    public List<String> getColumnLabels(final TableMetas tableMetas) {
        return selectItems.getColumnLabels(tableMetas, ((SelectStatement) getSqlStatement()).getTables());
    }
    
    /**
     * Judge group by and order by sequence is same or not.
     *
     * @return group by and order by sequence is same or not
     */
    public boolean isSameGroupByAndOrderByItems() {
        return !groupByContext.getItems().isEmpty() && groupByContext.getItems().equals(orderByContext.getItems());
    }
}
