package com.haleywang.putty.util;


import line.someonecode.AES;

public class AESUtil {
	
	public static String encrypt(String content, String key) throws Exception {
		return AES.encrypt(content, key);
	}
	

	public static String decrypt(String encrypted, String key) throws Exception {
		
		return AES.decrypt(encrypted, key);
	}

	public static String generateKey() {
		return AES.generateKey();
	}

	public static String generateKey(String in) {
		if(in == null || in.length() <= 0) {
			throw new RuntimeException("input should not be null");
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

	public static void main(String[] args) {
		String s = AESUtil.generateKey();
		System.out.println(s.length());

		System.out.println((s+"123").substring(0 ,16).length());
	}

}