package sqlancer.noisepage.gen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.ast.newast.Node;
import sqlancer.noisepage.NoisePageErrors;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageSchema.NoisePageColumn;
import sqlancer.noisepage.NoisePageSchema.NoisePageTable;
import sqlancer.noisepage.NoisePageToStringVisitor;
import sqlancer.noisepage.ast.NoisePageExpression;

public final class NoisePageUpdateGenerator {

    private NoisePageUpdateGenerator() {
    }

    public static Query getQuery(NoisePageGlobalState globalState) {
        StringBuilder sb = new StringBuilder("UPDATE ");
        Set<String> errors = new HashSet<>();
        NoisePageTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        sb.append(table.getName());
        NoisePageExpressionGenerator gen = new NoisePageExpressionGenerator(globalState).setColumns(table.getColumns());
        sb.append(" SET ");
        List<NoisePageColumn> columns = table.getRandomNonEmptyColumnSubset();
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
            sb.append("=");
            Node<NoisePageExpression> expr;
            if (Randomly.getBooleanWithSmallProbability()) {
                expr = gen.generateExpression();
                NoisePageErrors.addExpressionErrors(errors);
            } else {
                expr = gen.generateConstant();
            }
            sb.append(NoisePageToStringVisitor.asString(expr));
        }
        NoisePageErrors.addInsertErrors(errors);
        return new QueryAdapter(sb.toString(), errors);
    }

}
