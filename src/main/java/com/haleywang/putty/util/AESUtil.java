package com.haleywang.putty.util;


import com.haleywang.putty.common.AESException;
import com.haleywang.putty.common.Preconditions;
import org.someonecode.AES;

public class AESUtil {
	private AESUtil(){}
	
	public static String encrypt(String content, String key) {
		Preconditions.checkArgument(content != null, "Content cannot be null");
		Preconditions.checkArgument(key != null, "Key cannot be null");
		try {
			return AES.encrypt(content, key);
		} catch (Exception e) {
			throw new AESException(e);
		}
	}
	

	public static String decrypt(String encryptedText, String key) {
		Preconditions.checkArgument(encryptedText != null, "EncryptedText cannot be null");
		Preconditions.checkArgument(key != null, "Key cannot be null");

		try {
			return AES.decrypt(encryptedText, key);
		} catch (Exception e) {
			throw new AESException(e);
		}
	}

	public static String generateKey() {
		return AES.generateKey();
	}

	public static String generateKey(String in) {
		if(in == null || in.length() <= 0) {
			throw new AESException("input should not be empty");
		}

		String input = in.toLowerCase();
		if(input.length() > 16) {
			return input.substring(0, 16);
		}
		while (input.length() < 16) {
			input = appendA(input);
		}
		return input;

	}

	private static String appendA(String in) {
		return in + "a";
	}
}