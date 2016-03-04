package chatapp;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;

public class JEncrypDES {

    //desKeygen() returns a Secret Key Object, need to be used to encrypt and decrypt
    // encryptMessage takes in string and returns a Byte array
    // decryptMessage takes byte array and returns Original string
    // Nounce values must be equal for encrypt and decrypt.. OR ELSE computer will explode
    
    // EXAMPLE USE 
       // byte nounce = 1;
       // SecretKey newKey = desKeyGen();
       // byte[]encryptedMessage =  encryptDES(message , nounce, newKey);
      //  String output = decryptDES(encryptedMessage , nounce, newKey);

    
    public SecretKey desKeyGen() throws NoSuchAlgorithmException {

     //init keygenerator, Object contains functionality to create keys
     KeyGenerator keygen = KeyGenerator.getInstance("DES");
     // generate key
     SecretKey DESkey = keygen.generateKey();

     return DESkey;
    }
    //Pass in a message, nounce and key(Key must be generated by deskeyGen )
    // Returns a BYTE array, the byte array is concatenated with a Nounce 
    public byte[] encryptDES(String message, byte nounce, SecretKey key) {

     try {


      //Cipher object contains Cryptographic cipher functionality
      Cipher desObject;
      // Specify Cipher params
      // DES, ECB Electronic Codebook mode, PKCS5Padding means With Padding, 
      desObject = Cipher.getInstance("DES/ECB/PKCS5Padding");

      //convert message to byte array, Cipher Methods take Byte Arrays as parameters

      byte[] messageBytes = message.getBytes();
      messageBytes[messageBytes.length] = nounce;
      //init cipher encrpyt more with Generated key
      desObject.init(Cipher.ENCRYPT_MODE, key);
      // doFinal runs multi part Encryption 
      byte[] bytesEncrypted = desObject.doFinal(messageBytes);
      return bytesEncrypted;


     } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {

      System.out.println(e);
      return null;
     }


    }

    // Nounce value must be equal to that of the message 
    // this method returns a String
    public String decryptDES(byte[] encryptedMessage, int currentNounce, SecretKey key) {

     try {
      //Cipher object contains Cryptographic cipher functionality
      Cipher desObject;
      // Specify Cipher params
      // DES, ECB Electronic Codebook mode, PKCS5Padding means With Padding, 
      desObject = Cipher.getInstance("DES/ECB/PKCS5Padding");

      // init and execute Decypher mode, using same key
      desObject.init(Cipher.DECRYPT_MODE, key);
      byte[] bytesDecrypted = desObject.doFinal(encryptedMessage);

      if (bytesDecrypted[bytesDecrypted.length - 1] != currentNounce) {
       System.out.println("BAD NOUNCE");
       return null;
      } else {
       Arrays.copyOf(bytesDecrypted, bytesDecrypted.length - 1);
       // Converts Bytes to output string
       String outputText = new String(bytesDecrypted, "UTF-8");
       return outputText;
      }

     } catch (NoSuchAlgorithmException | UnsupportedEncodingException | NoSuchPaddingException | InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {

      System.out.println(e);
      return null;
     }
    }
}
