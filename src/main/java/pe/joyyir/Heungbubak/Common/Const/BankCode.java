package pe.joyyir.Heungbubak.Common.Const;

public enum BankCode {
    SHINHAN("088");

    String bankCode;
    BankCode(String bankCode) {
        this.bankCode = bankCode;
    }

    @Override
    public String toString() {
        return bankCode;
    }
}