package com.ConsoleDownloader;

import java.io.*;
import java.util.*;

/**
 * @author DmitrakovDI
 */

/**
 * Исключение. выбрасываемое при неверных парматерах
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
				try	{
					countThreads = Integer.parseInt(args[i + 1]);
					if (countThreads < 1)			//Проверка на положительность
						throw new BadParamException(args[i], args[i + 1]);
				} catch (NumberFormatException e) {
					throw new BadParamException(args[i], args[i + 1]);
				}
			else if (args[i].equals("-l"))
				try {
					String strSpeed = args[i + 1];
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
						throw new BadParamException(args[i], args[i + 1]);
					
					speed = k * speed;
				} catch (NumberFormatException e) {
					throw new BadParamException(args[i], args[i + 1]);
				}
			else if (args[i].equals("-o"))
			{
				File file = new File(args[i + 1]);
				if (!file.exists())
					file.mkdirs();
				outFolder = args[i + 1];
			}
			//Список файлов для закачки
			else if (args[i].equals("-f")) {
				String line;
				try (BufferedReader br = new BufferedReader(new FileReader(args[i + 1]))){
					while ((line = br.readLine()) != null) {
						String files[] = line.split(" ");
						if (listFiles.containsKey(files[0]))
							listFiles.get(files[0]).add(files[1]);
						else
							listFiles.put(files[0], new ArrayList<String>(Arrays.asList(files[1])));
					}
				} catch (IOException exec) {
					throw new BadParamException("-f", exec.toString());	
				}
			}			
			else {
				throw new BadParamException(args[i], "unknown parameter");
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
