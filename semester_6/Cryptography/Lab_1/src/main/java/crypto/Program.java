package crypto;

import java.math.BigInteger;
import java.util.Random;

public class Program {

    private static class EuclidResult{
        BigInteger x;
        BigInteger y;
        BigInteger gcd;

        public EuclidResult(BigInteger x, BigInteger y, BigInteger g) {
            this.x = x;
            this.y = y;
            this.gcd = g;
        }
    }

    public static BigInteger randomBigInteger(int bitSize){
        Random random = new Random();
        BigInteger randomNumber;
        randomNumber = new BigInteger(bitSize, random);
        return randomNumber;
    }

    public static BigInteger randomBigInteger(BigInteger upperLimit){
        Random random = new Random();
        BigInteger randomNumber;
        do{
            randomNumber = new BigInteger(upperLimit.bitLength(), random);
        }while(randomNumber.compareTo(upperLimit) >= 0);
        return randomNumber;
    }

    public static BigInteger powMod(BigInteger a, BigInteger n, BigInteger mod){
        if(a.equals(BigInteger.ZERO)){
            return a;
        }
        if(n.equals(BigInteger.ZERO)){
            return BigInteger.ONE;
        }
        if(n.equals(BigInteger.ONE)){
            return a;
        }

        var half = powMod(a, n.divide(BigInteger.TWO), mod);
        var result = half.multiply(half).mod(mod);
        if(!n.mod(BigInteger.TWO).equals(BigInteger.ZERO)){
            result = result.multiply(a).mod(mod);
        }
        return result;
    }

    public static boolean checkFermat(BigInteger x, long iterations){
        if(x.compareTo(BigInteger.valueOf(4)) < 0){
            return x.compareTo(BigInteger.TWO) * x.compareTo(BigInteger.valueOf(3)) == 0;
        }
        for(long i = 0; i < iterations; i++){
            BigInteger a = BigInteger.TWO.add(randomBigInteger(x.subtract(BigInteger.valueOf(4))));
            if(!powMod(a, x.subtract(BigInteger.ONE), x).equals(BigInteger.ONE)){
                return false;
            }
        }
        return true;
    }

    public static BigInteger Karatsuba(BigInteger a, BigInteger b){
        int minimumSize = 10;
        int maxLength = Math.max(a.toString().length(), b.toString().length());
        if(maxLength < minimumSize) {
            return a.multiply(b);
        }

        int half = maxLength / 2;
        BigInteger tenPowerHalf = BigInteger.valueOf(10).pow(half);

        BigInteger aLeft = a.divide(tenPowerHalf);
        BigInteger aRight = a.mod(tenPowerHalf);

        BigInteger bLeft = b.divide(tenPowerHalf);
        BigInteger bRight = b.mod(tenPowerHalf);

        BigInteger leftLeft = Karatsuba(aLeft, bLeft);
        BigInteger rightRight = Karatsuba(aRight, bRight);
        BigInteger leftRight = Karatsuba(aLeft.add(aRight), bLeft.add(bRight));

        return leftLeft.multiply(BigInteger.valueOf(10).pow(half * 2))
                .add(tenPowerHalf.multiply(
                        (leftRight.subtract(leftLeft).subtract(rightRight)))
                    )
                .add(rightRight);
    }

    public static boolean millerRabin(BigInteger a, int iterations){
        if(a.compareTo(BigInteger.TWO) < 0)
            return false;
        if(a.compareTo(BigInteger.valueOf(4)) < 0)
            return true;

        int s = 0;
        BigInteger t = a.subtract(BigInteger.ONE);

        while(t.mod(BigInteger.TWO).equals(BigInteger.ZERO)){
            t = t.divide( BigInteger.TWO);
            s += 1;
        }

        for(int i = 0; i < iterations; i++){
            var b = BigInteger.TWO.add(randomBigInteger(a.subtract(BigInteger.valueOf(4))));
            var x = powMod(b,t,a);
            if(x.equals(BigInteger.ONE) || x.equals(a.subtract(BigInteger.ONE))){
                continue;
            }
            for(int j = 1; j < s; j++){
                x = x.multiply(x).mod(a);
                if (x.equals(BigInteger.ONE)) {
                    return false;
                }
                if (x.equals(a.subtract(BigInteger.ONE))){
                    break;
                }
            }
            if (!x.equals(a.subtract(BigInteger.ONE))){
                return false;
            }
        }
        return true;
    }

    public static EuclidResult expandedEuclid(BigInteger a, BigInteger b) {
        if(a.equals(BigInteger.ZERO)) {
            return new EuclidResult(BigInteger.ZERO, BigInteger.ONE, b);
        }
        var euclidResult = expandedEuclid(b.mod(a),a);
        var x = euclidResult.y.subtract(b.divide(a).multiply(euclidResult.x));
        var y = euclidResult.x;
        return new EuclidResult(x,y,euclidResult.gcd);
    }

    private static BigInteger MontgomeryRepresentation(BigInteger number, BigInteger N, BigInteger R, int power) {
        EuclidResult euclid = expandedEuclid(N, R);

        //m = number * N' mod R
        BigInteger m = (number.multiply(euclid.x.negate())).mod(R);
        //result = (number + m * N) >> power
        BigInteger result = (number.add(m.multiply(N))).shiftRight(power);

        while (N.compareTo(result) < 0) {
            result = result.subtract(N);
        }
        return result;
    }

    private static int createR(BigInteger N, BigInteger R){
        int power = 10;
        while(!expandedEuclid(N, R).gcd.equals(BigInteger.ONE)){
            R = R.shiftLeft(1);
            power++;
        }
        return power;
    }

    public static BigInteger multiplyMontgomery(BigInteger a, BigInteger b, BigInteger N) {
        if(!expandedEuclid(N, BigInteger.TWO).gcd.equals(BigInteger.ONE)){
            return Karatsuba(a,b).mod(N);
        }
        BigInteger R = BigInteger.valueOf(1024);
        int power = createR(N, R);
        BigInteger a1 = a.shiftLeft(power).mod(N);  //a1 = aR mod N
        BigInteger b1 = b.shiftLeft(power).mod(N);  //b1 = bR mod N
        BigInteger c1 = a1.multiply(b1).mod(N);

        BigInteger c2 = MontgomeryRepresentation(a1.multiply(b1), N, R, power);
        return MontgomeryRepresentation(c1, N, R, power);
    }

    public static BigInteger powerMontgomery(BigInteger a, BigInteger e, BigInteger N) {
        if(!expandedEuclid(N, BigInteger.TWO).gcd.equals(BigInteger.ONE)){
            return powMod(a,e,N);
        }
        BigInteger R = BigInteger.valueOf(1024);
        int power = createR(N, R);
        BigInteger a1 = a.shiftLeft(power).mod(N);
        BigInteger x1 = BigInteger.ONE;
        while (e.compareTo(BigInteger.ZERO) > 0) {
            x1 = MontgomeryRepresentation(x1.multiply(a1), N, R, power);
            e = e.subtract(BigInteger.ONE);
        }
        return x1;
    }

    public static void main(String[] args){
        System.out.println(checkFermat(BigInteger.valueOf(3253), 5));
        System.out.println(checkFermat(BigInteger.valueOf(13), 5));
        System.out.println(checkFermat(BigInteger.valueOf(17), 5));
        System.out.println(checkFermat(BigInteger.valueOf(18), 5));
        System.out.println(checkFermat(BigInteger.valueOf(24), 5));

        System.out.println(millerRabin(BigInteger.valueOf(3253), 5));
        System.out.println(millerRabin(BigInteger.valueOf(13), 5));
        System.out.println(millerRabin(BigInteger.valueOf(17), 5));
        System.out.println(millerRabin(BigInteger.valueOf(18), 5));
        System.out.println(millerRabin(BigInteger.valueOf(24), 5));

        BigInteger c1 = Karatsuba(BigInteger.valueOf(12345678), BigInteger.valueOf(12345679)).mod(BigInteger.valueOf(100001));
        BigInteger c2 = multiplyMontgomery(BigInteger.valueOf(12345678),
                                            BigInteger.valueOf(12345679),
                                            BigInteger.valueOf(100001));
        System.out.println(c1);
        System.out.println(c2);
    }
}


