package com.h8000572003.values.sql;

import java.util.List;

/**
 * Result of {@link SqlParameterizer#rewrite}.
 *
 * @param expression Java expression of the parameterized SQL
 * @param parameters values to bind, in placeholder order
 */
public record SqlRewrite(String expression, List<BindParameter> parameters) {
}
