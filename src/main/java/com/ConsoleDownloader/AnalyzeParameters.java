/**
 * @author DmitrakovDI
 */

package com.ConsoleDownloader;

import java.io.*;
import java.util.*;

/**
 * Исключение. выбрасываемое при неверных параметрах
 */
class BadParamException extends Exception {
	String param;
	String value;
	BadParamException(String param, String value) {
		this.param = param;
		this.value= value;
	}
	public String toString() {
		return "Error: parameter=" + param + ", value=" + value;
	}
}

/**
 * Класс, реализующий проверку параметров
 * Бросает исключение BadParamException
 */
class AnalyzeParameters {
	private Map<String, List<String>> listFiles;	//Карта url и имен для сохранения
	private int countThreads;						//Кол-во потоков
	private long speed;								//Макс. скорость загрузки в байтах
	private String outFolder;						//Папка для сохранения
	
	AnalyzeParameters(String[] args) throws BadParamException
	{	
		
		if (args.length != 8) {						//Неверное кол-во параметров
			throw new BadParamException("count of parameters", Integer.toString(args.length));
		}
		
		listFiles = new HashMap<String, List<String>>();
		
		for (int i =0; i < args.length; i+=2)
			if (args[i].equals("-n"))				
				analyzeCountThreads(args[i], args[i+1]);
			else if (args[i].equals("-l"))
				analyzeSpeed(args[i], args[i+1]);
			else if (args[i].equals("-o"))
				analyzeOutputFolder(args[i], args[i+1]);
			//Список файлов для закачки
			else if (args[i].equals("-f"))
				analyzeFileOfLinks(args[i], args[i+1]);			
			else {
				throw new BadParamException(args[i], "unknown parameter");
			}		
	}
	
	private void analyzeCountThreads(String param, String value) throws  BadParamException {
		try	{
			countThreads = Integer.parseInt(value);
			if (countThreads < 1)			//Проверка на положительность
				throw new BadParamException(param, value);
		} catch (NumberFormatException e) {
			throw new BadParamException(param, value);
		}
	}
	
	private void analyzeSpeed(String param, String value) throws  BadParamException {
		try {
			String strSpeed= value;
			int k = 1;
			
			//Проверка на суффикс 'm'
			if (strSpeed.charAt(strSpeed.length() -1) == 'm') {
				k = 1024*1024;
				strSpeed = strSpeed.substring(0, strSpeed.length() - 1); 
			}
			//Проверка на суффикс 'k'
			else if (strSpeed.charAt(strSpeed.length() -1) == 'k') {
				k = 1024;
				strSpeed = strSpeed.substring(0, strSpeed.length() - 1);
			}
			speed = Integer.parseInt(strSpeed);
			
			if (speed < 1)			//Проверка на положительность
				throw new BadParamException(param, value);
			
			speed = k * speed;
		} catch (NumberFormatException e) {
			throw new BadParamException(param, value);
		}
	}
	
	private void analyzeOutputFolder(String param, String value) throws  BadParamException {
		File file = new File(value);
		if (!file.exists())
			if (!file.mkdirs()) {
				System.out.println("Directory wasn't create. " + value);
				throw new BadParamException(param, value);
			};
		outFolder = value;
	}
	
	private void analyzeFileOfLinks(String param, String value) throws  BadParamException{
		String line;
		try (BufferedReader br = new BufferedReader(new FileReader(value))){
			while ((line = br.readLine()) != null) {
				String files[] = line.split(" ");
				if (listFiles.containsKey(files[0]))
					listFiles.get(files[0]).add(files[1]);
				else
					listFiles.put(files[0], new ArrayList<String>(Arrays.asList(files[1])));
			}
		} catch (IOException exec) {
			throw new BadParamException(param, exec.toString());	
		}
	}
	
	/**
	 * @return Map <url, filenames>
	 */
	public Map<String, List<String>> getListFiles() {
		return listFiles;
	}
	
	/**
	 * @return Кол-во потоков
	 */
	public int getCountThreads() {
		return countThreads;
	}
	
	/**
	 * @return Скорость скачивания в байтах
	 */
	public long getSpeed() {
		return speed;
	}
	
	/**
	 * @return Каталог сохранения
	 */
	public String getFolder() {
		return outFolder;
	}
}
