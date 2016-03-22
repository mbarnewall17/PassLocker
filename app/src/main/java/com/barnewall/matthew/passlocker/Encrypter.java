package com.barnewall.matthew.passlocker;

import java.io.UnsupportedEncodingException;
import java.security.AlgorithmParameters;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.InvalidParameterSpecException;
import java.security.spec.KeySpec;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * Provided the same password, salt, and iv can encrypt a string and decrypt a byte array
 *
 * Created by Matthew on 4/2/2015.
 */
public class Encrypter{
    private byte[] salt;
    private byte[] iv;
    private Cipher eCipher;
    private Cipher dCipher;
    private SecretKey key;

    public Encrypter(String password){
        setUpSalt();
        setUpKey(salt, password.toCharArray());
        setUpECipher();
        setUpDCipher();
    }

    public Encrypter(String password, byte[] salt, byte[] iv){
        this.salt = salt;
        this.iv = iv;
        setUpKey(salt, password.toCharArray());
        setUpECipher(iv);
        setUpDCipher();
    }

    /*
     * Creates a salt for the encrypter
     */
    private void setUpSalt(){
        salt = new byte [8];
        SecureRandom rnd = new SecureRandom ();
        rnd.nextBytes(salt);
    }

    /*
     * Creates a secret key based on the salt and password
     *
     * @param   salt        The salt used to create the secret key
     * @param   password    The password used to create the secret key
     */
    private void setUpKey(byte[] salt, char[] password){
        try{
            SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
            KeySpec spec = new PBEKeySpec(password, salt, 65536, 128);
            SecretKey tmp = factory.generateSecret(spec);
            key = new SecretKeySpec(tmp.getEncoded(), "AES");
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }

    /*
     * Creates an encryption cipher
     */
    private void setUpECipher(){
        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key);
            AlgorithmParameters params = cipher.getParameters();
            iv = params.getParameterSpec(IvParameterSpec.class).getIV();
            eCipher = cipher;
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }

    /*
     * Creates an encryption cipher
     *
     * @param   iv  The initialization vector for the encrypter
     */
    private void setUpECipher(byte[] iv){
        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
            eCipher = cipher;
        } catch(Exception e){
            System.out.println(e.toString());
        }
    }

    /*
     * Creates a decryption cipher
     */
    private void setUpDCipher(){
        try{
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
            dCipher = cipher;
        } catch(Exception e){
            System.out.println(e.toString());
        }

    }

    /*
     * Attempts to encrypt the plaintext
     *
     * @param   plaintext   The String to be encrypted
     * @return              The encrypted byte array
     */
    public byte[] encrypt(String plaintext){
        try{
            return eCipher.doFinal(plaintext.getBytes("UTF-8"));
        } catch(Exception e){
            System.out.println(e.toString());
        }
        return null;
    }

    /*
     * Attempts to decrypt the ciphertext
     *
     * @param   ciphertext    The array to be decrypted
     * @return                The decrypted string
     */
    public String decrypt(byte[] ciphertext){
        try{
            return new String(dCipher.doFinal(ciphertext), "UTF-8");
        } catch(Exception e){
            e.printStackTrace();
        }
        return null;
    }

    /*
     * Return the salts
     *
     * @return salt   The encrypter objects salt
     */
    public byte[] getSalt(){
        return salt;
    }

    /*
     * Return the IV
     *
     * @return iv   The encrypter objects iv
     */
    public byte[] getIV(){
        return iv;
    }
}
