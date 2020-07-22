package sqlancer.noisepage.gen;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.noisepage.NoisePageErrors;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageSchema.NoisePageColumn;
import sqlancer.noisepage.NoisePageSchema.NoisePageTable;
import sqlancer.noisepage.NoisePageToStringVisitor;
import sqlancer.gen.AbstractInsertGenerator;

public class NoisePageInsertGenerator extends AbstractInsertGenerator<NoisePageColumn> {

    private final NoisePageGlobalState globalState;
    private final Set<String> errors = new HashSet<>();

    public NoisePageInsertGenerator(NoisePageGlobalState globalState) {
        this.globalState = globalState;
    }

    public static Query getQuery(NoisePageGlobalState globalState) {
        return new NoisePageInsertGenerator(globalState).generate();
    }

    private Query generate() {
        sb.append("INSERT INTO ");
        NoisePageTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        List<NoisePageColumn> columns = table.getRandomNonEmptyColumnSubset();
        sb.append(table.getName());
        sb.append("(");
        sb.append(columns.stream().map(c -> c.getName()).collect(Collectors.joining(", ")));
        sb.append(")");
        sb.append(" VALUES ");
        insertColumns(columns);
        NoisePageErrors.addInsertErrors(errors);
        return new QueryAdapter(sb.toString(), errors);
    }

    @Override
    protected void insertValue(NoisePageColumn tiDBColumn) {
        // TODO: select a more meaningful value
        if (Randomly.getBooleanWithRatherLowProbability()) {
            sb.append("DEFAULT");
        } else {
            sb.append(NoisePageToStringVisitor.asString(new NoisePageExpressionGenerator(globalState).generateConstant()));
        }
    }

}
