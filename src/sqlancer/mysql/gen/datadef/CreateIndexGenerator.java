package sqlancer.mysql.gen.datadef;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

import sqlancer.Query;
import sqlancer.QueryAdapter;
import sqlancer.Randomly;
import sqlancer.mysql.MySQLSchema;
import sqlancer.mysql.MySQLSchema.MySQLColumn;
import sqlancer.mysql.MySQLSchema.MySQLDataType;
import sqlancer.mysql.MySQLSchema.MySQLTable;
import sqlancer.mysql.MySQLSchema.MySQLTable.MySQLEngine;
import sqlancer.mysql.MySQLVisitor;
import sqlancer.mysql.ast.MySQLExpression;
import sqlancer.mysql.gen.MySQLRandomExpressionGenerator;

public class CreateIndexGenerator {

	private final Randomly r;
	private StringBuilder sb = new StringBuilder();
	private int indexNr;
	private boolean columnIsPrimaryKey;
	private boolean containsInPlace;
	private MySQLSchema schema;

	public CreateIndexGenerator(MySQLSchema schema, Randomly r) {
		this.schema = schema;
		this.r = r;
	}

	public static Query create(Randomly r, MySQLSchema schema) {
		return new CreateIndexGenerator(schema, r).create();
	}

	public Query create() {

		sb.append("CREATE ");
		if (Randomly.getBoolean()) {
			// "FULLTEXT" TODO Column 'c3' cannot be part of FULLTEXT index
			// A SPATIAL index may only contain a geometrical type column
			sb.append(Randomly.fromOptions("UNIQUE"));
			sb.append(" ");
		}
		sb.append("INDEX i" + indexNr++);
		indexType();
		sb.append(" ON ");
		MySQLTable table = schema.getRandomTable();
		sb.append(table.getName());
		sb.append("(");
		if (table.getEngine() == MySQLEngine.INNO_DB && Randomly.getBoolean()) {
			for (int i = 0; i < Randomly.smallNumber() + 1; i++) {
				if (i != 0) {
					sb.append(", ");
				}
				sb.append("(");
				MySQLExpression randExpr = MySQLRandomExpressionGenerator.generateRandomExpression(table.getColumns(),
						null, r);
				sb.append(MySQLVisitor.asString(randExpr));
				sb.append(")");

			}
		} else {
			List<MySQLColumn> randomColumn = table.getRandomNonEmptyColumnSubset();
			int i = 0;
			for (MySQLColumn c : randomColumn) {
				if (i++ != 0) {
					sb.append(", ");
				}
				if (c.isPrimaryKey()) {
					columnIsPrimaryKey = true;
				}
				sb.append(c.getName());
				if (Randomly.getBoolean() && c.getColumnType() != MySQLDataType.INT) {
					sb.append("(");
					// TODO for string
					sb.append(r.getInteger(1, 5));
					sb.append(")");
				}
				if (Randomly.getBoolean()) {
					sb.append(" ");
					sb.append(Randomly.fromOptions("ASC", "DESC"));
				}
			}
		}
		sb.append(")");
		indexOption();
		algorithmOption();
		String string = sb.toString();
		sb = new StringBuilder();
		return new QueryAdapter(string) {
			@Override
			public boolean execute(Connection con) throws SQLException {
				try {
					super.execute(con);
					return true;
				} catch (java.sql.SQLIntegrityConstraintViolationException e) {
					// IGNORE;
				} catch (SQLException e) {
					if (e.getMessage().startsWith("ALGORITHM=INPLACE is not supported") && containsInPlace) {
						// ignore
					} else if (e.getMessage().startsWith("A primary key index cannot be invisible")) {
						// ignore
					} else if (e.getMessage().startsWith("Table handler doesn't support NULL in given index.")
							&& table.getEngine() == MySQLEngine.ARCHIVE) {
						// ignore
					} else if (e.getMessage().startsWith(
							"Functional index on a column is not supported. Consider using a regular index instead.")) {
						// ignore
						// TODO: what does this mean?
					} else if (e.getMessage()
							.startsWith("Incorrect usage of spatial/fulltext/hash index and explicit index order")) {
						// TODO what does this mean?
					} else if (e.getMessage()
							.startsWith("The storage engine for the table doesn't support descending indexes")) {
						// TODO what does this mean?
					} else if (e.getMessage().contains("must include all columns")) {
						// partitioning functions
					} else if (e.getMessage().contains("cannot index the expression")) {
						// index NULL
					} else if (e.getMessage().contains("Data truncation: Truncated incorrect ")) {

					} else if (e.getMessage().contains("a disallowed function.")
							&& (string.contains("BENCHMARK") || string.contains("EXISTS"))) {

					} else if (e.getMessage().contains("Data truncation")) {

					} else if (e.getMessage().contains(
							"Cannot create a functional index on an expression that returns a BLOB or TEXT.")) {

					} else if (e.getMessage().contains("used in key specification without a key length")) {

					} else if (e.getMessage().contains("can't be used in key specification with the used table type")) {

					} else if (e.getMessage().contains("Specified key was too long")) {

					} else if (e.getMessage().contains("out of range")) {

					} else if (e.getMessage().contains("Data truncated for functional index")) {

					} else {
						throw e;
					}
				}
				return false;
			}

		};
	}

	private void algorithmOption() {
		if (Randomly.getBoolean()) {
			sb.append(" ALGORITHM");
			if (Randomly.getBoolean()) {
				sb.append("=");
			}
			sb.append(" ");
			String fromOptions = Randomly.fromOptions("DEFAULT", "INPLACE", "COPY");
			if (fromOptions.contentEquals("INPLACE")) {
				containsInPlace = true;
			}
			sb.append(fromOptions);
		}
	}

	private void indexOption() {
		if (Randomly.getBoolean()) {
			sb.append(" ");
			if (columnIsPrimaryKey) {
				// The explicit primary key cannot be made invisible.
				sb.append("VISIBLE");
			} else {
				sb.append(Randomly.fromOptions("VISIBLE", "INVISIBLE"));
			}
		}
	}

	private void indexType() {
		if (Randomly.getBoolean()) {
			sb.append(" USING ");
			sb.append(Randomly.fromOptions("BTREE", "HASH"));
		}
	}

	public void setNewSchema(MySQLSchema schema) {
		this.schema = schema;
	}
}