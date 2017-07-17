package pe.joyyir.Heungbubak.Const;

public enum PriceType {
    BUY("bid"), // 사람들이 사려고 올린 가격 (= 내가 파는 가격)
    SELL("ask"); // 사람들이 팔려고 올린 가격 (= 내가 사는 가격)

    private String type;
    PriceType(String type) { this.type = type; }

    @Override
    public String toString() {
        return type;
    }
}