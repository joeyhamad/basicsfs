package com.csproj.Client;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class clientutils {

	public byte[] initVectorHash;
	public byte[] sharedKey;

	/**
	 * Takes a file and reads it into a byte array for processing later
	 *
	 * @param file File to be split
	 * @return Byte array of the file
	 * @throws IOException
	 */

	public byte[] readFileToByteArray(RandomAccessFile file) throws IOException {
		byte[] b_array = new byte[(int)file.length()];
		file.seek(0);
		file.readFully(b_array);
		file.close();
		return b_array;
	}

	public File readByteArrayToFile(byte[] arrayToWrite, String outputFilePath) throws IOException {
		try {
			File file = new File(outputFilePath);
			file.createNewFile();
			System.out.println("File: " + file);
		} catch(Exception e) {
			e.printStackTrace();
		}

		try (FileOutputStream fos = new FileOutputStream(outputFilePath)) {
			fos.write(arrayToWrite);
			fos.close();
		} catch(Exception e){
			FileOutputStream fos = new FileOutputStream(outputFilePath);
			fos.write(0x01);
		}

		return new File(outputFilePath);
	}

	public ArrayList splitByteArrayToArrayList (byte[] byteArray){
		ArrayList<byte[]> splitByteArrayList = new ArrayList<>();
		int byteArrayLength = byteArray.length;
		int traversals = byteArray.length / 32;
		if(byteArrayLength % 32 > 0){
			traversals += 1;
		}


		for (int i = 0; i <traversals ; i++) {
			byte[] splitArray = new byte[32];
			for (int j = 0; j < 32; j++) {
				splitArray[j] = byteArray[j + (i*32)] ;
			}
			splitByteArrayList.add(splitArray);
		}
		return splitByteArrayList;
	}

	public byte[] joinArrayListToByteArray(ArrayList<byte[]> byteArrayList){
		int arrayListSize = byteArrayList.size();
		int arraySize = byteArrayList.get(0).length;
		byte[] returnedArray = new byte[arraySize * arrayListSize];

		for(int i = 0; i < arrayListSize; i++){
			byte[] current = byteArrayList.get(i);
			int arrayIndex;
			for(int j = 0; j < 32; j++){
				returnedArray[j + (i*32)] = current[j];
			}
		}

		return returnedArray;
	}

	public String readFileContents(String filePath) throws IOException {
		File file = new File(filePath);
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);
		String line = "";
		String fileContents = "";

		while ((line = br.readLine()) != null) {
			fileContents += line + "\n";
		}

		br.close();
		fr.close();

		return fileContents;
	}

	public void writeToFile(String filepath, String contentToWrite) throws IOException {
		String strToWrite = contentToWrite + "\n";
		FileOutputStream os = new FileOutputStream(filepath, true);
		byte[] strToBytes = strToWrite.getBytes();
		os.write(strToBytes);
		os.close();
	}

	/**
	 *
	 * @param byteArray byteArray to be encrypted
	 * @return
	 */
	// ORIGINAL FUNCTION HEADER FOR LATER: public ArrayList<byte[]> encryptByteArray(byte[] byteArray, int chunkSize, String initialKey) throws NoSuchAlgorithmException {
	public byte[] encryptByteArray(byte[] byteArray) throws NoSuchAlgorithmException {

		ArrayList<byte[]> splitByteArrayList = splitByteArrayToArrayList(byteArray);
		// Split file into individual 32 byte arrays to perform operations on

		// Generate initialization vector
		Random rd = new Random();
		byte[] arr = new byte[32];
		rd.nextBytes(arr);

		byte[] sharedKey = this.sharedKey;

		// Copy to key + initialization vector to new 48 byte array
		byte[] initVector = new byte[48];
		System.arraycopy(sharedKey, 0, initVector, 0, 16);
		System.arraycopy(arr, 0, initVector, 16, 32);

		// Calculate SHA 256 of initialization vector
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] keyedhash = digest.digest(initVector);
		this.initVectorHash = keyedhash.clone();

		// XOR calculated hash with first 32 bytes of data
		byte[] firstEncodedBlock = new byte[32];
		byte[] firstDataBlock = splitByteArrayList.get(0);
		for(int i = 0; i < 32; i++){
			firstEncodedBlock[i] = (byte)(keyedhash[i] ^ firstDataBlock[i]);
		}

		ArrayList<byte[]> cipheredByteArrayList = new ArrayList<>();
		cipheredByteArrayList.add(firstEncodedBlock);

		// Take previous ciphertext, append key to it, then calculate SHA256 value.
		// XOR this SHA256 value with plaintext value

		for(int i = 1; i < splitByteArrayList.size(); i++){
			byte[] vector = new byte[48];
			byte[] previousCipherBlock = cipheredByteArrayList.get(i-1);

			System.arraycopy(previousCipherBlock, 0, vector, 0, 32);
			System.arraycopy(sharedKey, 0, vector, 32, 16);

			keyedhash = digest.digest(vector);

			byte[] currentPlaintext = splitByteArrayList.get(i);

			byte[] calculatedCipherBlock = new byte[32];

			for(int j = 0; j < 32; j++){
				calculatedCipherBlock[j] = (byte)(currentPlaintext[j] ^ keyedhash[j]);
			}

			cipheredByteArrayList.add(calculatedCipherBlock);

		}

		return joinArrayListToByteArray(cipheredByteArrayList);
	}


	// We can change this so that the client has to perform the SHA-256 calculation of the initialization vector + shared key
	public byte[] decryptByteArray(byte[] byteArray, byte[] sharedKey, byte[] initializationVectorHash) throws NoSuchAlgorithmException{

		ArrayList<byte[]> cipheredByteArrayList = splitByteArrayToArrayList(byteArray);
		ArrayList<byte[]> decryptedByteArrayList = new ArrayList<>();

		byte[] firstEncodedBlock = cipheredByteArrayList.get(0);
		byte[] firstDataBlock = new byte[32];

		for(int i = 0; i < 32; i++){
			firstDataBlock[i] = (byte)(initializationVectorHash[i] ^ firstEncodedBlock[i]);
		}

		decryptedByteArrayList.add(firstDataBlock);

		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] keyedhash;

		for(int i = 1; i < cipheredByteArrayList.size(); i++){
			byte[] vector = new byte[48];
			byte[] previousCipherBlock = cipheredByteArrayList.get(i-1);

			System.arraycopy(previousCipherBlock, 0, vector, 0, 32);
			System.arraycopy(sharedKey, 0, vector, 32, 16);

			keyedhash = digest.digest(vector);

			byte[] currentCiphertext = cipheredByteArrayList.get(i);

			byte[] calculatedPlaintextBlock = new byte[32];

			for(int j = 0; j < 32; j++){
				calculatedPlaintextBlock[j] = (byte)(currentCiphertext[j] ^ keyedhash[j]);
			}

			decryptedByteArrayList.add(calculatedPlaintextBlock);

		}
		return joinArrayListToByteArray(decryptedByteArrayList);

	}
}
