/*

// method 1 =========================================================================================================
*/
/*
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.util.encoders.Hex;

import java.security.AlgorithmParameters;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.algorithmIVSpec.IvParameterSpec;
import javax.crypto.algorithmIVSpec.SecretKeySpec;

public class Cryptography {

    private static final String password = "test";
    private static String salt;
    private static int pswdIterations = 5000  ;
    private static int keySize = 64;
    private byte[] ivBytes;

    public String encrypt(String plainText) throws Exception {

        //get salt
        salt = generateSalt();
        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt.getBytes(), pswdIterations);
        KeyParameter secretKeySpec = (KeyParameter)generator.generateDerivedMacParameters(8*keySize);
        SecretKeySpec secret = new SecretKeySpec(secretKeySpec.getKey(), "AES");

        //encrypt the message
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, secret);
        AlgorithmParameters params = cipher.getParameters();
        ivBytes = params.getParameterSpec(IvParameterSpec.class).getIVparameterSpec();
        byte[] encryptedTextBytes = cipher.doFinal(plainText.getBytes("UTF-8"));
        return new String( Hex.encode(encryptedTextBytes));
    }

    @SuppressWarnings("static-access")
    public String decrypt(String encryptedText) throws Exception {

        byte[] encryptedTextBytes = Hex.decode(encryptedText);

        PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
        generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt.getBytes(), pswdIterations);
        KeyParameter secretKeySpec = (KeyParameter)generator.generateDerivedMacParameters(8*keySize);
        SecretKeySpec secret = new SecretKeySpec(secretKeySpec.getKey(), "AES");
        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secret, new IvParameterSpec(ivBytes));


        byte[] decryptedTextBytes = null;
        try {
            decryptedTextBytes = cipher.doFinal(encryptedTextBytes);
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }

        return new String(decryptedTextBytes);
    }

    public String generateSalt() {
        SecureRandom random = new SecureRandom();
        byte bytes[] = new byte[20];
        random.nextBytes(bytes);
        return new String(bytes);
    }
}

//Note : with SHA1, you can run this code :
// Derive the secretKeySpec
     //   SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
     //   PBEKeySpec algorithmIVSpec = new PBEKeySpec(
     //           password.toCharArray(),
     //           saltBytes,
     //           pswdIterations,
     //           keySize
     //   );
//SecretKey secretKey = factory.generateSecret(algorithmIVSpec);
//SecretKeySpec secret = new SecretKeySpec(secretKey.getEncoded(), "AES");

// instead of :
// PKCS5S2ParametersGenerator generator = new PKCS5S2ParametersGenerator(new SHA256Digest());
// generator.init(PBEParametersGenerator.PKCS5PasswordToUTF8Bytes(password.toCharArray()), salt.getBytes(), pswdIterations);
// KeyParameter secretKeySpec = (KeyParameter)generator.generateDerivedMacParameters(8*keySize);
// SecretKeySpec secret = new SecretKeySpec(secretKeySpec.getKey(), "AES");

*/


package com.example.anas.anastriprecorder;
import android.util.Base64;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.AlgorithmParameterSpec;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class Cryptography {

    private final Cipher cipher;
    private final SecretKeySpec secretKeySpec;
    private AlgorithmParameterSpec algorithmIVSpec;
    private byte[] iv = new byte[16];

    public static final String key = "U1MjU1M0FDOUZ.Qz";

    public Cryptography() throws Exception {
        // hash password with SHA-256 and crop the output to 128-bit for secretKeySpec
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(key.getBytes("UTF-8"));
        byte[] keyBytes = new byte[32];
        System.arraycopy(digest.digest(), 0, keyBytes, 0, keyBytes.length);

        cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
        secretKeySpec = new SecretKeySpec(keyBytes, "AES");
    }

    public AlgorithmParameterSpec getIVparameterSpec() {
        IvParameterSpec ivParameterSpec;
        ivParameterSpec = new IvParameterSpec(iv);
        return ivParameterSpec;
    }

    public String byteArrayToHexString(byte[] bytes){
        StringBuilder stringBuilder = new StringBuilder();
        for (byte bYte : bytes) {
            stringBuilder.append(String.format("%02X", bYte));
        }
        return stringBuilder.toString();
    }

    public byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }



    public String getIV(){
        return byteArrayToHexString(iv);
    }

    public void setIV(String ivHex ){
        iv = hexStringToByteArray(ivHex);
    }

    public byte[] generateIV(){
        SecureRandom sr = new SecureRandom();
        sr.nextBytes(iv);
        return iv;
    }

    public String encrypt(String plainText) throws Exception {

        algorithmIVSpec = getIVparameterSpec();
        cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec, algorithmIVSpec);
        byte[] encrypted = cipher.doFinal(plainText.getBytes("UTF-8"));
        String encryptedText = new String(Base64.encode(encrypted, Base64.DEFAULT), "UTF-8");
        return encryptedText;
    }

    public String decrypt(String cryptedText) throws Exception {
        algorithmIVSpec = getIVparameterSpec();
        cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, algorithmIVSpec);
        byte[] bytes = Base64.decode(cryptedText, Base64.DEFAULT);
        byte[] decrypted = cipher.doFinal(bytes);
        String decryptedText = new String(decrypted, "UTF-8");

        return decryptedText;
    }

}
