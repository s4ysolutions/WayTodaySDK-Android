package solutions.s4y.waytoday.sdk.tracker;

/**
 * Created by lezh1k on 2/13/18.
 */

class DataItemAcc extends DataItem {
    public final double absNorthAcc;
    public final double absEastAcc;
    final double absUpAcc;

    public DataItemAcc(double absNorthAcc, double absEastAcc, double absUpAcc) {
        super();
        this.absUpAcc = absUpAcc;
        this.absNorthAcc = absNorthAcc;
        this.absEastAcc = absEastAcc;
    }

    public double getAbsNorthAcc(double declination) {
        return absNorthAcc * Math.cos(declination) + absEastAcc * Math.sin(declination);
    }

    public double getAbsEastAcc(double declination) {
        return absEastAcc * Math.cos(declination) - absNorthAcc * Math.sin(declination);
    }

    public double getAbsUpAcc() {
        return absUpAcc;
    }
}
