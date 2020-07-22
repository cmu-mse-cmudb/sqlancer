package sqlancer.noisepage.gen;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.ast.newast.Node;
import sqlancer.noisepage.NoisePageErrors;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageSchema.NoisePageColumn;
import sqlancer.noisepage.NoisePageSchema.NoisePageCompositeDataType;
import sqlancer.noisepage.NoisePageSchema.NoisePageDataType;
import sqlancer.noisepage.NoisePageToStringVisitor;
import sqlancer.noisepage.ast.NoisePageExpression;
import sqlancer.gen.UntypedExpressionGenerator;

public class NoisePageTableGenerator {

    public Query getQuery(NoisePageGlobalState globalState) {
        Set<String> errors = new HashSet<>();
        StringBuilder sb = new StringBuilder();
        String tableName = globalState.getSchema().getFreeTableName();
        sb.append("CREATE TABLE ");
        sb.append(tableName);
        sb.append("(");
        List<NoisePageColumn> columns = getNewColumns();
        UntypedExpressionGenerator<Node<NoisePageExpression>, NoisePageColumn> gen = new NoisePageExpressionGenerator(
                globalState).setColumns(columns);
        for (int i = 0; i < columns.size(); i++) {
            if (i != 0) {
                sb.append(", ");
            }
            sb.append(columns.get(i).getName());
            sb.append(" ");
            sb.append(columns.get(i).getType());
            if (globalState.getDmbsSpecificOptions().testCollate && Randomly.getBooleanWithRatherLowProbability()
                    && columns.get(i).getType().getPrimitiveDataType() == NoisePageDataType.VARCHAR) {
                sb.append(" COLLATE ");
                sb.append(getRandomCollate());
            }
            if (globalState.getDmbsSpecificOptions().testIndexes && Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(" UNIQUE");
            }
            if (globalState.getDmbsSpecificOptions().testNotNullConstraints
                    && Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(" NOT NULL");
            }
            if (globalState.getDmbsSpecificOptions().testCheckConstraints
                    && Randomly.getBooleanWithRatherLowProbability()) {
                sb.append(" CHECK(");
                sb.append(NoisePageToStringVisitor.asString(gen.generateExpression()));
                NoisePageErrors.addExpressionErrors(errors);
                sb.append(")");
            }
            if (Randomly.getBoolean() && globalState.getDmbsSpecificOptions().testDefaultValues) {
                sb.append(" DEFAULT(");
                sb.append(NoisePageToStringVisitor.asString(gen.generateConstant()));
                sb.append(")");
            }
        }
        if (globalState.getDmbsSpecificOptions().testIndexes && Randomly.getBoolean()) {
            errors.add("Invalid type for index");
            List<NoisePageColumn> primaryKeyColumns = Randomly.nonEmptySubset(columns);
            sb.append(", PRIMARY KEY(");
            sb.append(primaryKeyColumns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
            sb.append(")");
        }
        sb.append(")");
        return new QueryAdapter(sb.toString(), errors, true);
    }

    public static String getRandomCollate() {
        return Randomly.fromOptions("NOCASE", "NOACCENT", "NOACCENT.NOCASE", "C", "POSIX");
    }

    private static List<NoisePageColumn> getNewColumns() {
        List<NoisePageColumn> columns = new ArrayList<>();
        for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
            String columnName = String.format("c%d", i);
            NoisePageCompositeDataType columnType = NoisePageCompositeDataType.getRandom();
            columns.add(new NoisePageColumn(columnName, columnType, false, false));
        }
        return columns;
    }

}
