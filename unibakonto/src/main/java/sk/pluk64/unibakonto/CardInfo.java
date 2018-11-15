package sk.pluk64.unibakonto;

public class CardInfo {
    public final String number;
    private final String released;
    private final String validFrom;
    public final String validUntil;

    CardInfo(String number, String released, String validFrom, String validUntil) {
        this.number = divideBy4Digits(number);
        this.released = released;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    private String divideBy4Digits(String number) {
        StringBuilder resultBuilder = new StringBuilder();

        int BLOCK_LENGTH = 4;
        int length = number.length();
        int firstBlockLength = length % BLOCK_LENGTH;
        resultBuilder.append(number, 0, firstBlockLength);

        for (int i = firstBlockLength; i < length; i += BLOCK_LENGTH) {
            if (i > 0) {
                resultBuilder.append(" ");
            }
            resultBuilder.append(number, i, i + BLOCK_LENGTH);
        }

        return resultBuilder.toString();
    }
}
