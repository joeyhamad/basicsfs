package com.csproj.Client;
import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class clientutils {

	public byte[] initVectorHash;
	//change to private
	public byte[] sharedKey;

	public static final int KEY_SIZE_AES = 128;

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
			//fos.close(); There is no more need for this line since you had created the instance of "fos" inside the try. And this will automatically close the OutputStream
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

		// DEBUGGING
		// Generate random key for testing, disable this for final product!
//		Random rd2 = new Random();
//		byte[] sharedKey = new byte[16];
//		rd2.nextBytes(sharedKey);
//		this.sharedKey = sharedKey.clone();
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

	public static void printByteArray(String msg, byte[] byteArray) { //used for debugging
		System.out.println(msg);
		System.out.println("Total: " + byteArray.length + " bytes.");
		StringBuilder result = new StringBuilder();
		for (int i = 0; i < byteArray.length; i++) {
			result.append(String.format("%02x", byteArray[i]));
			if ((i + 1) % 16 == 0)
				result.append("\n");
			else if ((i + 1) % 2 == 0)
				result.append(" ");
		}
		System.out.println(result.toString());	
	}
	
	/**
	 * Forwards the bytes from one stream to another
	 * @param source - the input stream from which we are sending
	 * @param destination - the output stream that we are sending to
	 * @throws IOException
	 */
	public static void sendBytes(InputStream source, OutputStream destination) throws IOException {
		byte[] buffer = new byte[1024];
		while(true) {
			int readAmount = source.read(buffer);
			if (readAmount == -1) break;
			destination.write(buffer,0,readAmount);
		}
	}
	/**
	 * Attempts to forward <code>len</code> bytes from one stream to another
	 * @param source - the input stream from which we are sending
	 * @param destination - the output stream that we are sending to
	 * @param len - the maximum number of bytes to send from source to destination
	 * @throws IOException
	 */
	public static void sendBytes(InputStream source, OutputStream destination, long len) throws IOException {
		byte[] buffer = new byte[1024];
		long remaining = len;
		while(true) {
			if (remaining == 0) break;
			int readAmount = source.read(buffer,0,(int) remaining);
			if (readAmount == -1) break;
			destination.write(buffer,0,readAmount);
			remaining -= readAmount;
		}
	}
	
	
	/**
	 * Consumes just the ASCII encoded header from the stream (but leaves the body untouched). This also includes consuming
	 * the two new line characters "\n\n" that separate the body from the header.
	 * @param in - the input stream (from a socket)
	 * @return ArrayList that contains the header parts separated.
	 * @throws IOException
	 */
	public static ArrayList<String> consumeAndBreakHeader(InputStream in) throws IOException {
		ArrayList<Character> pipeline = new ArrayList<>();
		StringBuilder header = new StringBuilder();
		int c;
		while ((c = in.read()) != -1) {
			pipeline.add((char) c);
			header.append((char) c);
			if (pipeline.size() != 2) // pipeline not full
				continue;
			if (pipeline.get(0) == '\n' && pipeline.get(1) == '\n') {
				header.deleteCharAt(header.length()-1);
				break;
			}
			pipeline.remove(0); // keep track of only the recent 2 bytes
		}
		if (header.length() == 0) return null;
		ArrayList<String> headerParts = new ArrayList<String>();
		Scanner scanner = new Scanner(header.toString());
		scanner.useDelimiter("\n");
		while (scanner.hasNext()) {
			headerParts.add(scanner.next());
		}
		scanner.close();
		return headerParts;
	}
}
