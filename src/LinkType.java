public enum LinkType {
    LOW("low", 0), HIGH("high", 1);

    private String type;
    private Integer num;

    LinkType(String type, Integer num) {
        this.type = type;
        this.num = num;
    }

    public String type() {
        return type;
    }

    public static String byNum(Integer num) {
        for (LinkType l : LinkType.values()) {
            if (l.num == num) {
                return l.type;
            }
        }
        System.out.println("num" + num);
        throw new IllegalArgumentException("LinkType num not found.");
    }
}