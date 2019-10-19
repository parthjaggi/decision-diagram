public enum NodeType {
    INTERNAL(0), EXTERNAL(1);

    private Integer type;

    NodeType(Integer type) {
        this.type = type;
    }

    public Integer type() {
        return type;
    }
}