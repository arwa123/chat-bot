package com.example.chat.model;

/**
 * Enum representing PostgreSQL vector operations
 */
public enum VectorOperation {
    /**
     * Cosine distance operator
     */
    COSINE("<=>"),
    
    /**
     * Euclidean distance operator
     */
    EUCLIDEAN("<->"),
    
    /**
     * Inner product distance operator
     */
    INNER_PRODUCT("<#>"),
    
    /**
     * Negative inner product distance operator (for similarity instead of distance)
     */
    NEGATIVE_INNER_PRODUCT("<=>");
    
    private final String operator;
    
    VectorOperation(String operator) {
        this.operator = operator;
    }
    
    public String getOperator() {
        return operator;
    }
    
    @Override
    public String toString() {
        return operator;
    }
}