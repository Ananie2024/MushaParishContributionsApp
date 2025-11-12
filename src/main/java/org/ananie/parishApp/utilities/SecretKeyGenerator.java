package org.ananie.parishApp.utilities;

import java.security.SecureRandom;
import java.util.Base64;


public class SecretKeyGenerator {
	public static void main(String [] ss) {
		// Generate a cryptographically strong 32-byte (256-bit) key
		SecureRandom random = new SecureRandom();
		byte[] bytes = new byte[32];
		random.nextBytes(bytes);
		
		 // Encode the key bytes as a Base64 string for use in properties
		String secretKey = Base64.getEncoder().encodeToString(bytes);
		System.out.println("My secret key : " + secretKey);
	
 }
}