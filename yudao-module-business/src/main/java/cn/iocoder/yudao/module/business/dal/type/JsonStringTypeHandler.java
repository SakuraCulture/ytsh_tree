package cn.iocoder.yudao.module.business.dal.type;

import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedJdbcTypes;
import org.apache.ibatis.type.MappedTypes;
import org.apache.ibatis.type.TypeHandler;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@MappedJdbcTypes(JdbcType.VARCHAR)
@MappedTypes(String.class)
public class JsonStringTypeHandler implements TypeHandler<String> {

    private static final String H2_DATABASE_PRODUCT_NAME = "H2";

    @Override
    public void setParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType) throws SQLException {
        if (parameter == null) {
            ps.setString(i, null);
            return;
        }
        ps.setString(i, JsonUtils.toJsonString(parameter));
    }

    @Override
    public String getResult(ResultSet rs, String columnName) throws SQLException {
        return parseJsonString(rs.getString(columnName), isH2Database(rs.getStatement()));
    }

    @Override
    public String getResult(ResultSet rs, int columnIndex) throws SQLException {
        return parseJsonString(rs.getString(columnIndex), isH2Database(rs.getStatement()));
    }

    @Override
    public String getResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parseJsonString(cs.getString(columnIndex), isH2Database(cs.getConnection()));
    }

    private String parseJsonString(String value, boolean h2Database) {
        if (value == null) {
            return null;
        }
        if (StrUtil.isBlank(value)) {
            return value;
        }
        String result = JsonUtils.parseObject(value, String.class);
        if (!h2Database || !looksLikeJsonStringLiteral(result)) {
            return result;
        }
        return JsonUtils.parseObject(result, String.class);
    }

    private boolean isH2Database(Statement statement) throws SQLException {
        return statement != null && isH2Database(statement.getConnection());
    }

    private boolean isH2Database(Connection connection) throws SQLException {
        if (connection == null) {
            return false;
        }
        DatabaseMetaData metaData = connection.getMetaData();
        return metaData != null && H2_DATABASE_PRODUCT_NAME.equalsIgnoreCase(metaData.getDatabaseProductName());
    }

    private boolean looksLikeJsonStringLiteral(String value) {
        return value != null && value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"");
    }
}
