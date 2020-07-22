package sqlancer.noisepage.test;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import sqlancer.ComparatorHelper;
import sqlancer.Randomly;
import sqlancer.noisepage.NoisePageErrors;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageToStringVisitor;

public class NoisePageQueryPartitioningWhereTester extends NoisePageQueryPartitioningBase {

    public NoisePageQueryPartitioningWhereTester(NoisePageGlobalState state) {
        super(state);
        NoisePageErrors.addGroupByErrors(errors);
    }

    @Override
    public void check() throws SQLException {
        super.check();
        select.setWhereClause(null);
        String originalQueryString = NoisePageToStringVisitor.asString(select);

        List<String> resultSet = ComparatorHelper.getResultSetFirstColumnAsString(originalQueryString, errors, state);

        boolean orderBy = Randomly.getBooleanWithRatherLowProbability();
        if (orderBy) {
            select.setOrderByExpressions(gen.generateOrderBys());
        }
        select.setWhereClause(predicate);
        String firstQueryString = NoisePageToStringVisitor.asString(select);
        select.setWhereClause(negatedPredicate);
        String secondQueryString = NoisePageToStringVisitor.asString(select);
        select.setWhereClause(isNullPredicate);
        String thirdQueryString = NoisePageToStringVisitor.asString(select);
        List<String> combinedString = new ArrayList<>();
        List<String> secondResultSet = ComparatorHelper.getCombinedResultSet(firstQueryString, secondQueryString,
                thirdQueryString, combinedString, !orderBy, state, errors);
        ComparatorHelper.assumeResultSetsAreEqual(resultSet, secondResultSet, originalQueryString, combinedString,
                state);
    }

}
