package sqlancer.noisepage;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import sqlancer.Randomly;
import sqlancer.noisepage.NoisePageSchema.NoisePageTable;
import sqlancer.schema.AbstractSchema;
import sqlancer.schema.AbstractTable;
import sqlancer.schema.AbstractTableColumn;
import sqlancer.schema.AbstractTables;
import sqlancer.schema.TableIndex;

public class NoisePageSchema extends AbstractSchema<NoisePageTable> {

    public enum NoisePageDataType {

        INT, VARCHAR, BOOLEAN, FLOAT, DATE, TIMESTAMP;

        public static NoisePageDataType getRandom() {
            return Randomly.fromOptions(values());
        }

    }

    public static class NoisePageCompositeDataType {

        private final NoisePageDataType dataType;

        private final int size;

        public NoisePageCompositeDataType(NoisePageDataType dataType) {
            this.dataType = dataType;
            this.size = -1;
        }

        public NoisePageCompositeDataType(NoisePageDataType dataType, int size) {
            this.dataType = dataType;
            this.size = size;
        }

        public NoisePageDataType getPrimitiveDataType() {
            return dataType;
        }

        public int getSize() {
            if (size == -1) {
                throw new AssertionError(this);
            }
            return size;
        }

        public static NoisePageCompositeDataType getRandom() {
            NoisePageDataType type = NoisePageDataType.getRandom();
            int size = -1;
            switch (type) {
            case INT:
                size = Randomly.fromOptions(1, 2, 4, 8);
                break;
            case FLOAT:
                size = Randomly.fromOptions(4, 8);
                break;
            case BOOLEAN:
            case VARCHAR:
            case DATE:
            case TIMESTAMP:
                size = 0;
                break;
            default:
                throw new AssertionError(type);
            }

            return new NoisePageCompositeDataType(type, size);
        }

        public static NoisePageCompositeDataType getInt(int size) {
            return new NoisePageCompositeDataType(NoisePageDataType.INT, size);
        }

        @Override
        public String toString() {
            switch (getPrimitiveDataType()) {
            case INT:
                switch (size) {
                case 8:
                    return Randomly.fromOptions("BIGINT", "INT8");
                case 4:
                    return Randomly.fromOptions("INTEGER", "INT", "INT4", "SIGNED");
                case 2:
                    return Randomly.fromOptions("SMALLINT", "INT2");
                case 1:
                    return Randomly.fromOptions("TINYINT", "INT1");
                default:
                    throw new AssertionError(size);
                }
            case VARCHAR:
                return "VARCHAR";
            case FLOAT:
                switch (size) {
                case 8:
                    return Randomly.fromOptions("DOUBLE", "NUMERIC");
                case 4:
                    return Randomly.fromOptions("REAL", "FLOAT4");
                default:
                    throw new AssertionError(size);
                }
            case BOOLEAN:
                return Randomly.fromOptions("BOOLEAN", "BOOL");
            case TIMESTAMP:
                return Randomly.fromOptions("TIMESTAMP", "DATETIME");
            case DATE:
                return Randomly.fromOptions("DATE");
            default:
                throw new AssertionError(getPrimitiveDataType());
            }
        }

    }

    public static class NoisePageColumn extends AbstractTableColumn<NoisePageTable, NoisePageCompositeDataType> {

        private final boolean isPrimaryKey;
        private final boolean isNullable;

        public NoisePageColumn(String name, NoisePageCompositeDataType columnType, boolean isPrimaryKey, boolean isNullable) {
            super(name, null, columnType);
            this.isPrimaryKey = isPrimaryKey;
            this.isNullable = isNullable;
        }

        public boolean isPrimaryKey() {
            return isPrimaryKey;
        }

        public boolean isNullable() {
            return isNullable;
        }

    }

    public static class NoisePageTables extends AbstractTables<NoisePageTable, NoisePageColumn> {

        public NoisePageTables(List<NoisePageTable> tables) {
            super(tables);
        }

    }

    public NoisePageSchema(List<NoisePageTable> databaseTables) {
        super(databaseTables);
    }

    public NoisePageTables getRandomTableNonEmptyTables() {
        return new NoisePageTables(Randomly.nonEmptySubset(getDatabaseTables()));
    }

    private static NoisePageCompositeDataType getColumnType(String typeString) {
        NoisePageDataType primitiveType;
        int size = -1;
        switch (typeString) {
        case "INTEGER":
            primitiveType = NoisePageDataType.INT;
            size = 4;
            break;
        case "SMALLINT":
            primitiveType = NoisePageDataType.INT;
            size = 2;
            break;
        case "BIGINT":
            primitiveType = NoisePageDataType.INT;
            size = 8;
            break;
        case "TINYINT":
            primitiveType = NoisePageDataType.INT;
            size = 1;
            break;
        case "VARCHAR":
            primitiveType = NoisePageDataType.VARCHAR;
            break;
        case "FLOAT":
            primitiveType = NoisePageDataType.FLOAT;
            size = 4;
            break;
        case "DOUBLE":
            primitiveType = NoisePageDataType.FLOAT;
            size = 8;
            break;
        case "BOOLEAN":
            primitiveType = NoisePageDataType.BOOLEAN;
            break;
        case "DATE":
            primitiveType = NoisePageDataType.DATE;
            break;
        case "TIMESTAMP":
            primitiveType = NoisePageDataType.TIMESTAMP;
            break;
        default:
            throw new AssertionError(typeString);
        }
        return new NoisePageCompositeDataType(primitiveType, size);
    }

    public static class NoisePageTable extends AbstractTable<NoisePageColumn, TableIndex> {

        public NoisePageTable(String tableName, List<NoisePageColumn> columns, boolean isView) {
            super(tableName, columns, Collections.emptyList(), isView);
        }

        public boolean hasPrimaryKey() {
            return getColumns().stream().anyMatch(c -> c.isPrimaryKey());
        }

    }

    public static NoisePageSchema fromConnection(Connection con, String databaseName) throws SQLException {
        List<NoisePageTable> databaseTables = new ArrayList<>();
        List<String> tableNames = getTableNames(con);
        for (String tableName : tableNames) {
            List<NoisePageColumn> databaseColumns = getTableColumns(con, tableName);
            boolean isView = tableName.startsWith("v");
            NoisePageTable t = new NoisePageTable(tableName, databaseColumns, isView);
            for (NoisePageColumn c : databaseColumns) {
                c.setTable(t);
            }
            databaseTables.add(t);

        }
        return new NoisePageSchema(databaseTables);
    }

    private static List<String> getTableNames(Connection con) throws SQLException {
        List<String> tableNames = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery("SELECT * FROM sqlite_master()")) {
                while (rs.next()) {
                    tableNames.add(rs.getString("name"));
                }
            }
        }
        return tableNames;
    }

    private static List<NoisePageColumn> getTableColumns(Connection con, String tableName) throws SQLException {
        List<NoisePageColumn> columns = new ArrayList<>();
        try (Statement s = con.createStatement()) {
            try (ResultSet rs = s.executeQuery(String.format("SELECT * FROM pragma_table_info('%s');", tableName))) {
                while (rs.next()) {
                    String columnName = rs.getString("name");
                    String dataType = rs.getString("type");
                    boolean isNullable = rs.getString("notnull").contentEquals("false");
                    boolean isPrimaryKey = rs.getString("pk").contains("true");
                    NoisePageColumn c = new NoisePageColumn(columnName, getColumnType(dataType), isPrimaryKey, isNullable);
                    columns.add(c);
                }
            }
        }
        if (columns.stream().noneMatch(c -> c.isPrimaryKey())) {
            // https://github.com/cwida/duckdb/issues/589
            // https://github.com/cwida/duckdb/issues/588
            // TODO: implement an option to enable/disable rowids
            columns.add(new NoisePageColumn("rowid", new NoisePageCompositeDataType(NoisePageDataType.INT, 4), false, false));
        }
        return columns;
    }

}
