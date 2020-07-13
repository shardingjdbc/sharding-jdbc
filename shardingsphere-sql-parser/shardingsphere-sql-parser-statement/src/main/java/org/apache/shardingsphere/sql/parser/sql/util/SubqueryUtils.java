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

package org.apache.shardingsphere.sql.parser.sql.util;

import org.apache.shardingsphere.sql.parser.sql.segment.dml.JoinedTableSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.TableFactorSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.TableReferenceSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.ExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.expr.subquery.SubqueryExpressionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.ProjectionsSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.item.SubqueryProjectionSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.PredicateSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.WhereSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateBetweenRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateCompareRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.dml.predicate.value.PredicateInRightValue;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.SubqueryTableSegment;
import org.apache.shardingsphere.sql.parser.sql.segment.generic.table.TableSegment;
import org.apache.shardingsphere.sql.parser.sql.statement.dml.SelectStatement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Subquery utility class.
 */
public final class SubqueryUtils {
    
    /**
     * Get subquery where segment from SelectStatement.
     *
     * @param selectStatement SelectStatement.
     * @return subquery where segment collection.
     */
    public static Collection<WhereSegment> getSubqueryWhereSegmentsFromSelectStatement(final SelectStatement selectStatement) {
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromProjections(selectStatement.getProjections()));
        subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromTableReferences(selectStatement.getTableReferences()));
        subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromWhere(selectStatement.getWhere().orElse(null)));
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromProjections(final ProjectionsSegment projections) {
        if (null == projections || projections.getProjections().isEmpty()) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        for (ProjectionSegment each : projections.getProjections()) {
            if (!(each instanceof SubqueryProjectionSegment)) {
                continue;
            }
            SelectStatement subquerySelect = ((SubqueryProjectionSegment) each).getSubquery().getSelect();
            subquerySelect.getWhere().ifPresent(subqueryWhereSegments::add);
            subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromSelectStatement(subquerySelect));
        }
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromTableReferences(final Collection<TableReferenceSegment> tableReferences) {
        if (tableReferences.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        for (TableReferenceSegment each : tableReferences) {
            subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromTableFactor(each.getTableFactor()));
            subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromJoinedTable(each.getJoinedTables()));
        }
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromWhere(final WhereSegment where) {
        if (null == where || where.getAndPredicates().isEmpty()) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        List<PredicateSegment> predicateSegments = where.getAndPredicates().stream().flatMap(andPredicate -> andPredicate.getPredicates().stream()).collect(Collectors.toList());
        for (PredicateSegment each : predicateSegments) {
            if (each.getRightValue() instanceof PredicateBetweenRightValue) {
                subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromExpression(((PredicateBetweenRightValue) each.getRightValue()).getBetweenExpression()));
                subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromExpression(((PredicateBetweenRightValue) each.getRightValue()).getAndExpression()));
            }
            if (each.getRightValue() instanceof PredicateCompareRightValue) {
                subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromExpression(((PredicateCompareRightValue) each.getRightValue()).getExpression()));
            }
            if (each.getRightValue() instanceof PredicateInRightValue) {
                for (ExpressionSegment sqlExpression : ((PredicateInRightValue) each.getRightValue()).getSqlExpressions()) {
                    subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromExpression(sqlExpression));
                }
            }
        }
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromTableFactor(final TableFactorSegment tableFactor) {
        if (null == tableFactor) {
            return Collections.emptyList();
        }
        return getSubqueryWhereSegmentsFromTableSegment(tableFactor.getTable());
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromJoinedTable(final Collection<JoinedTableSegment> joinedTables) {
        if (joinedTables.isEmpty()) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        for (JoinedTableSegment joinedTable : joinedTables) {
            if (null == joinedTable.getTableFactor()) {
                continue;
            }
            subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromTableSegment(joinedTable.getTableFactor().getTable()));
        }
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromTableSegment(final TableSegment tableSegment) {
        if (!(tableSegment instanceof SubqueryTableSegment)) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        SelectStatement subquerySelect = ((SubqueryTableSegment) tableSegment).getSubquery().getSelect();
        subquerySelect.getWhere().ifPresent(subqueryWhereSegments::add);
        subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromSelectStatement(subquerySelect));
        return subqueryWhereSegments;
    }
    
    private static Collection<WhereSegment> getSubqueryWhereSegmentsFromExpression(final ExpressionSegment expressionSegment) {
        if (!(expressionSegment instanceof SubqueryExpressionSegment)) {
            return Collections.emptyList();
        }
        Collection<WhereSegment> subqueryWhereSegments = new ArrayList<>();
        SelectStatement subquerySelect = ((SubqueryExpressionSegment) expressionSegment).getSubquery().getSelect();
        subquerySelect.getWhere().ifPresent(subqueryWhereSegments::add);
        subqueryWhereSegments.addAll(getSubqueryWhereSegmentsFromSelectStatement(subquerySelect));
        return subqueryWhereSegments;
    }
}
