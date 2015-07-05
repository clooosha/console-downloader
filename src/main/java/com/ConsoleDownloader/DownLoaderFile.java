package com.ConsoleDownloader;

import java.util.*;
import java.nio.channels.FileChannel;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Менеджер загрузки файлов. Проходит по Map <url, filenames>
 * и загружает файл по url, используя нужное кол-во потоков
 * (если возможно)
 */
class DownLoaderFiles implements Runnable {
	private static final int BLOCK_SIZE = 1024;		//Размер блока для единичного скачивания
	private Map<String, List<String>> mapFiles;		//Карта url и имен для сохранения
	private String outFolder;						//Папка для сохранения
	private int countThreads;						//Кол-во потоков
	private long maxSpeed;							//Макс. скорость загрузки в байтах.
	public long totalDownloaded;					//Всего загруженных байт
	private URL url;								//Текущий url
	public Thread thrd;								//Поток
	private boolean multiThread;					//Поддержка многопоточной загрузки для текущего url
	
	public DownLoaderFiles(Map<String, List<String>> mapFiles, String outFolder, int countThreads, long maxSpeed) {
		this.mapFiles = mapFiles;
		this.outFolder = outFolder;
		this.countThreads = countThreads;
		this.maxSpeed = maxSpeed;
		totalDownloaded = 0;
		multiThread = false;
		thrd = new Thread(this);
		thrd.start();
	}
	
	/**
	 *  Проверяет текущий url на доступность и поддержку многопоточности
	 * @param link текущий url
	 * @return длина файла в байтах
	 */
	private long checkUrl(String link) {
		try {
			url = new URL(link);
		} catch (MalformedURLException e) {
			System.out.println("Error " + link + " timeout.");
		}
		HttpURLConnection conn = null;
		try {
			conn = (HttpURLConnection)url.openConnection();			//Открываем соединение
			conn.setConnectTimeout(10000);
			conn.setRequestProperty("Range", "bytes=0-");			//Проставляем значение поля, чтоб определить поддержку многопоточности
			conn.connect();
			
			if (conn.getResponseCode() / 100 != 2) {				//Положительный ответ сервера
				System.out.println("Error " + link + " timeout.");
				return -1;
			}
			if (conn.getResponseCode() == 206)						//Поддерживает многопоточность
				multiThread = true;
			else  {
				multiThread = false;
				System.out.println(link + " multithread not supported.");
			}
		} catch (IOException e){
			System.out.println("Error " + link + " " + e);			
		}
		
		long contentLength = conn.getContentLength();				
		if (contentLength < 1) {									//Проверка на длину файла
			System.out.println("Error " + link + " file_size=" + contentLength);
			return -1;
		}
		System.out.println(link + " file_size=" + contentLength);
		return contentLength;
	}
	
	/**
	 * Возвращает кол-во потоков небходимое для загрузки файла,
	 * учитывая его размер и ограничение на потоки
	 * @param contentLength размер файла
	 * @return кол-во потоков
	 */
	private int calculateCountThreads(long contentLength) {
		int count = (int) Math.ceil((float)contentLength / BLOCK_SIZE);
		if (count > countThreads) 
			count = countThreads;
		if (!multiThread)
			count =1;
		return count;
	}
	
	private long calculateSizeOfPart(long contentLength, int curCountThreads) {
		if (multiThread)
			return (long)Math.ceil((float)contentLength / curCountThreads);
		else
			return contentLength;
	}
	
	/**
	 * Обработка Url из mapFiles и создание потоков
	 */
	public void run() {
		for(Map.Entry<String, List<String>> entry: mapFiles.entrySet()) {
			System.out.println("Link " + entry.getKey());
			createThreads(entry.getKey(), entry.getValue());
			saveOtherNames(entry.getValue());
		}
	}
	
	private void createThreads(String strUrl, List<String> listOfNames) {
		long contentLength = checkUrl(strUrl);
		if (contentLength  < 0)
			return;
			
		int curCountThreads = calculateCountThreads(contentLength);
		long partSize = calculateSizeOfPart(contentLength, curCountThreads);	
		System.out.println(strUrl + " countThreads=" + curCountThreads + " part_size=" + partSize);		
		
		//Устанавливаем настройки для потоков, качающих файл по текущем url
		DownloadThread.url = url;
		DownloadThread.fileName = listOfNames.get(0);
		DownloadThread.outFolder = outFolder;
		DownloadThread.block_size = BLOCK_SIZE;
		DownloadThread.maxSpeed = maxSpeed;
		DownloadThread.countThreads = curCountThreads;
		DownloadThread.speed = maxSpeed / countThreads;
		
		//Создаем потоки, указывая им необходимый диапазон загрузки файла
		List<DownloadThread> listThreads = new ArrayList<DownloadThread>();
		for(int i=0; i < curCountThreads; i++) {
			long startByte = i*partSize;
			long endByte = (i+1)*partSize - 1;
			//System.out.println("Start=" + startByte + " endByte=" + endByte);
			listThreads.add(new DownloadThread(startByte, endByte));
		}
		
		//Ждем завершения каждого потока
		for(DownloadThread dThread: listThreads) {					
			try {
				dThread.thrd.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			totalDownloaded += dThread.downloaded;		//Запоминаем сколько скачал байт
		}
		System.out.println(strUrl + " download finished.");
	}

	private void saveOtherNames(List<String> listOfNames) {
		for(int i=1; i <listOfNames.size(); i++)
			try {
		        FileChannel srcChannel = new FileInputStream(outFolder + "/" + listOfNames.get(0)).getChannel();
		        FileChannel dstChannel = new FileOutputStream(outFolder + "/" + listOfNames.get(i)).getChannel();
		        dstChannel.transferFrom(srcChannel, 0, srcChannel.size());
		        srcChannel.close();
		        dstChannel.close();
		        System.out.println(listOfNames.get(i) + " is saved.");
		    } catch (IOException e) {
		    	System.out.println("Error copy " + outFolder + "/" + listOfNames.get(i) + ". " + e);
		    }	
	}	
}

	/**
	* Поток, реализующий загрузку части файла по http и сохранение на диск
	*/
	class DownloadThread implements Runnable {
		public static URL url;									//url
		public static String fileName;							//Имя для сохранения файла
		public static String outFolder;							//Каталог для сохранения
		public static int block_size;							//Размер блока данных, для единичной загрузки
		public static long maxSpeed;							//Максимальная скорость
		public static volatile long speed;						//Скорость каждого потока, меняется от кол-ва потоков
		public static volatile long countThreads;				//Кол-во потоков
		
		public long downloaded;									//Кол-во байт загруженное потоком
		private long startByte;									//Номер байта, с которого начинается закачка
		private long endByte;									//Номер байт, на котором заканчивается закачка
		public Thread thrd;
		DownloadThread(long startByte, long endByte) {
			this.startByte = startByte;
			this.endByte = endByte;
			downloaded = 0;
			thrd = new Thread(this);
			thrd.start();
		}

		public void run() {
			BufferedInputStream in = null;
			RandomAccessFile raf= null;

			try {
				HttpURLConnection conn = (HttpURLConnection)url.openConnection(); 			//Открываем соединение
				String byteRange = startByte + "-" + endByte;								//Устанавливаем диапазон закачки
				conn.setRequestProperty("Range", "bytes=" + byteRange);
				conn.connect();
				if (conn.getResponseCode() / 100 != 2) {
					System.out.println("Error " + url.getPath() + " " + conn.getResponseCode());
					return;
				}
				
				in = new BufferedInputStream(conn.getInputStream());
				raf = new RandomAccessFile(outFolder + "/" + fileName, "rw");
				raf.seek(startByte);
					
				byte data[] = new byte[block_size];
				int numRead = 0;
				long timeStart= System.currentTimeMillis();
				long timeEnd;
				long leftBytes = speed;									//Оставшееся кол-во байт за данную секунду
				while((numRead = in.read(data, 0, block_size)) != -1 )
				{
					raf.write(data,0,numRead);
					downloaded += numRead;								//Обновляем кол-во загруженных байт
					leftBytes -= numRead;								
			        if (leftBytes < 1) {								//В данную секунду, загрузили положенное кол-во байт
			        	timeEnd = System.currentTimeMillis();
			        	if (timeEnd - timeStart < 1000)					//Секунда не закончена
				        	try {
								thrd.sleep(1000 - (timeEnd - timeStart));//Засыпаем до окончания данной секунды
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
			        	timeStart = timeEnd;
			        	leftBytes = speed;								//Обновляем кол-во байт для закачки в след секунду
			        }
						
				}
			} catch (IOException e) {
				System.out.println("Error " + e);
			} finally {
				try {
					raf.close();
				} catch (IOException e) {
					System.out.println("Error close " + raf.toString());
				}
				if (in != null) {
					try {
					in.close();
					} catch (IOException e) {}
			}
			countThreads--;												//Кол-во качающих потоков уменшаем
			if (countThreads > 0)
				speed = maxSpeed / countThreads;						//Пересчитываем скорость для потока
		}
	}
}