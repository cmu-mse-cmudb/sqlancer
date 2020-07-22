package sqlancer.noisepage.gen;

import java.util.HashSet;
import java.util.Set;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.noisepage.NoisePageErrors;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.NoisePageSchema.NoisePageTable;
import sqlancer.noisepage.NoisePageToStringVisitor;

public final class NoisePageDeleteGenerator {

    private NoisePageDeleteGenerator() {
    }

    public static Query generate(NoisePageGlobalState globalState) {
        StringBuilder sb = new StringBuilder("DELETE FROM ");
        Set<String> errors = new HashSet<>();
        NoisePageTable table = globalState.getSchema().getRandomTable(t -> !t.isView());
        sb.append(table.getName());
        if (Randomly.getBoolean()) {
            sb.append(" WHERE ");
            sb.append(NoisePageToStringVisitor.asString(
                    new NoisePageExpressionGenerator(globalState).setColumns(table.getColumns()).generateExpression()));
        }
        NoisePageErrors.addExpressionErrors(errors);
        return new QueryAdapter(sb.toString(), errors);
    }

}
