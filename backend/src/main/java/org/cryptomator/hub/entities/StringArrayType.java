package org.cryptomator.hub.entities;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
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
	public String[] nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		Array array = rs.getArray(position);
		return array != null ? (String[]) array.getArray() : null;
	}

	@Override
	public void nullSafeSet(PreparedStatement st, String[] value, int index, SharedSessionContractImplementor session) {
		throw new UnsupportedOperationException("Read Only");
	}

	@Override
	public boolean isMutable() {
		return false;
	}

	@Override
	public String[] deepCopy(String[] value) {
		return value; // value is immutable
	}

	@Override
	public Serializable disassemble(String[] value) {
		return value; // value is immutable
	}

	@Override
	public String[] assemble(Serializable cached, Object owner) {
		return (String[]) cached; // value is immutable
	}
}
