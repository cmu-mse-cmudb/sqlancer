package sqlancer.noisepage.gen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.ast.newast.Node;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageSchema.NoisePageColumn;
import sqlancer.noisepage.NoisePageSchema.NoisePageTable;
import sqlancer.noisepage.NoisePageToStringVisitor;
import sqlancer.noisepage.ast.NoisePageExpression;

public final class NoisePageIndexGenerator {

    private NoisePageIndexGenerator() {
    }

    public static Query getQuery(NoisePageGlobalState globalState) {
        Set<String> errors = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        sb.append("CREATE ");
        if (Randomly.getBoolean()) {
            errors.add("Cant create unique index, table contains duplicate data on indexed column(s)");
            sb.append("UNIQUE ");
        }
        sb.append("INDEX ");
        sb.append(Randomly.fromOptions("i0", "i1", "i2", "i3", "i4")); // cannot query this information
        sb.append(" ON ");
        NoisePageTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        sb.append(table.getName());
        sb.append("(");
        List<NoisePageColumn> columns = table.getRandomNonEmptyColumnSubset();
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
            sb.append(" ");
            if (Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(Randomly.fromOptions("ASC", "DESC"));
            }
        }
        sb.append(")");
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            Node<NoisePageExpression> expr = new NoisePageExpressionGenerator(globalState).setColumns(table.getColumns())
                    .generateExpression();
            sb.append(NoisePageToStringVisitor.asString(expr));
        }
        errors.add("already exists!");
        if (globalState.getDmbsSpecificOptions().testRowid) {
            errors.add("Cannot create an index on the rowid!");
        }
        return new QueryAdapter(sb.toString(), errors, true);
    }

}
