package sqlancer.noisepage;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import sqlancer.AbstractAction;
import sqlancer.CompositeTestOracle;
import sqlancer.GlobalState;
import sqlancer.IgnoreMeException;
import sqlancer.ProviderAdapter;
import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.QueryProvider;
import sqlancer.Randomly;
import sqlancer.StatementExecutor;
import sqlancer.TestOracle;
import sqlancer.noisepage.NoisePageProvider.NoisePageGlobalState;
import sqlancer.noisepage.gen.NoisePageDeleteGenerator;
import sqlancer.noisepage.gen.NoisePageIndexGenerator;
import sqlancer.noisepage.gen.NoisePageInsertGenerator;
import sqlancer.noisepage.gen.NoisePageRandomQuerySynthesizer;
import sqlancer.noisepage.gen.NoisePageTableGenerator;
import sqlancer.noisepage.gen.NoisePageUpdateGenerator;
import sqlancer.noisepage.gen.NoisePageViewGenerator;

public class NoisePageProvider extends ProviderAdapter<NoisePageGlobalState, NoisePageOptions> {

    public NoisePageProvider() {
        super(NoisePageGlobalState.class, NoisePageOptions.class);
    }

    public enum Action implements AbstractAction<NoisePageGlobalState> {

        INSERT(NoisePageInsertGenerator::getQuery), //
        CREATE_INDEX(NoisePageIndexGenerator::getQuery), //
        VACUUM((g) -> new QueryAdapter("VACUUM;")), //
        ANALYZE((g) -> new QueryAdapter("ANALYZE;")), //
        DELETE(NoisePageDeleteGenerator::generate), //
        UPDATE(NoisePageUpdateGenerator::getQuery), //
        CREATE_VIEW(NoisePageViewGenerator::generate), //
        EXPLAIN((g) -> {
            Set<String> errors = new HashSet<>();
            NoisePageErrors.addExpressionErrors(errors);
            NoisePageErrors.addGroupByErrors(errors);
            return new QueryAdapter(
                    "EXPLAIN " + NoisePageToStringVisitor
                            .asString(NoisePageRandomQuerySynthesizer.generateSelect(g, Randomly.smallNumber() + 1)),
                    errors);
        });

        private final QueryProvider<NoisePageGlobalState> queryProvider;

        Action(QueryProvider<NoisePageGlobalState> queryProvider) {
            this.queryProvider = queryProvider;
        }

        @Override
        public Query getQuery(NoisePageGlobalState state) throws SQLException {
            return queryProvider.getQuery(state);
        }
    }

    private static int mapActions(NoisePageGlobalState globalState, Action a) {
        Randomly r = globalState.getRandomly();
        switch (a) {
        case INSERT:
            return r.getInteger(0, globalState.getOptions().getMaxNumberInserts());
        case CREATE_INDEX:
            if (!globalState.getDmbsSpecificOptions().testIndexes) {
                return 0;
            }
            // fall through
        case UPDATE:
            return r.getInteger(0, globalState.getDmbsSpecificOptions().maxNumUpdates + 1);
        case VACUUM: // seems to be ignored
        case ANALYZE: // seems to be ignored
        case EXPLAIN:
            return r.getInteger(0, 2);
        case DELETE:
            return r.getInteger(0, globalState.getDmbsSpecificOptions().maxNumDeletes + 1);
        case CREATE_VIEW:
            return r.getInteger(0, globalState.getDmbsSpecificOptions().maxNumViews + 1);
        default:
            throw new AssertionError(a);
        }
    }

    public static class NoisePageGlobalState extends GlobalState<NoisePageOptions, NoisePageSchema> {

        @Override
        protected void updateSchema() throws SQLException {
            setSchema(NoisePageSchema.fromConnection(getConnection(), getDatabaseName()));
        }

    }

    @Override
    public void generateDatabase(NoisePageGlobalState globalState) throws SQLException {
        for (int i = 0; i < Randomly.fromOptions(1, 2); i++) {
            boolean success = false;
            do {
                Query qt = new NoisePageTableGenerator().getQuery(globalState);
                success = globalState.executeStatement(qt);
            } while (!success);
        }
        if (globalState.getSchema().getDatabaseTables().isEmpty()) {
            throw new IgnoreMeException(); // TODO
        }
        StatementExecutor<NoisePageGlobalState, Action> se = new StatementExecutor<>(globalState, Action.values(),
                NoisePageProvider::mapActions, (q) -> {
                    if (globalState.getSchema().getDatabaseTables().isEmpty()) {
                        throw new IgnoreMeException();
                    }
                });
        se.executeStatements();
    }

    @Override
    protected TestOracle getTestOracle(NoisePageGlobalState globalState) throws SQLException {
        return new CompositeTestOracle(globalState.getDmbsSpecificOptions().oracle.stream().map(o -> {
            try {
                return o.create(globalState);
            } catch (SQLException e1) {
                throw new AssertionError(e1);
            }
        }).collect(Collectors.toList()));
    }

    @Override
    public Connection createDatabase(NoisePageGlobalState globalState) throws SQLException {
        String url = "jdbc:duckdb:";
        return DriverManager.getConnection(url, globalState.getOptions().getUserName(),
                globalState.getOptions().getPassword());
    }

    @Override
    public String getDBMSName() {
        return "duckdb";
    }

}
