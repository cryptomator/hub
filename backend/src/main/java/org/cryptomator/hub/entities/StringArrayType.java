package org.cryptomator.hub.entities;

import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.usertype.UserType;

import java.io.Serializable;
import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Arrays;

public class StringArrayType implements UserType<String[]> {

	@Override
	public int getSqlType() {
		return Types.ARRAY;
	}

	@Override
	public Class<String[]> returnedClass() {
		return String[].class;
	}

	@Override
	public boolean equals(String[] x, String[] y) {
		return Arrays.equals(x, y);
	}

	@Override
	public int hashCode(String[] x) {
		return Arrays.hashCode(x);
	}

	@Override
	public String[] nullSafeGet(ResultSet rs, int position, WrapperOptions options) throws SQLException {
		Array array = rs.getArray(position);
		return array != null ? (String[]) array.getArray() : new String[0];
	}

	@Override
	public void nullSafeSet(PreparedStatement st, String[] value, int index, WrapperOptions options) {
		options.getSession().doWork(connection -> {
			var jdbcArray = connection.createArrayOf("VARCHAR", value == null ? new String[0] : value);
			st.setArray(index, jdbcArray);
		});
	}

	@Override
	public boolean isMutable() {
		return true;
	}

	@Override
	public String[] deepCopy(String[] value) {
		return value == null ? null : value.clone();
	}

	@Override
	public Serializable disassemble(String[] value) {
		return deepCopy(value);
	}

	@Override
	public String[] assemble(Serializable cached, Object owner) {
		if (cached instanceof String[] s) {
			return s.clone();
		} else {
			throw new IllegalArgumentException("Cached value is not of type String[]");
		}
	}
}
