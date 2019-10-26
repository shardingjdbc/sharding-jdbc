package org.apache.shardingsphere.shardingproxy.transport.mysql.packet.command.query;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;

/**
 * MySQL Column Field Detail Flag.
 * 
 * @see <a href="https://mariadb.com/kb/en/library/resultset/#column-definition-packet">Column count packet</a>
 * 
 * @author dongzonglei
 */
@RequiredArgsConstructor
@Getter
public enum MySQLColumnFieldDetailFlag {

    NOT_NULL(1),

    PRIMARY_KEY(2),

    UNIQUE_KEY(4),

    MULTIPLE_KEY(8),

    BLOB(16),

    UNSIGNED(32),

    ZEROFILL_FLAG(64),

    BINARY_COLLATION(128),

    ENUM(256),

    AUTO_INCREMENT(512),

    TIMESTAMP(1024),

    SET(2048),

    NO_DEFAULT_VALUE_FLAG(4096),

    ON_UPDATE_NOW_FLAG(8192),

    NUM_FLAG(32768);

    private static final Map<Integer, MySQLColumnFieldDetailFlag> MYSQL_COLUMN_FIELD_DETAIL_FLAG_CACHE = new HashMap<Integer, MySQLColumnFieldDetailFlag>() {
        {
            for (MySQLColumnFieldDetailFlag each : MySQLColumnFieldDetailFlag.values()) {
                this.put(each.value, each);
            }
        }
    };

    private final int value;

    /**
     * Value of integer.
     *
     * @param value integer value
     * @return column field detail flag enum
     */
    public static MySQLColumnFieldDetailFlag valueOf(final int value) {
        MySQLColumnFieldDetailFlag result = MYSQL_COLUMN_FIELD_DETAIL_FLAG_CACHE.get(value);
        if (null == result) {
            throw new IllegalArgumentException(String.format("Cannot find '%s' in column field detail flag", value));
        }
        return result;
    }
}
