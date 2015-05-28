package com.ConsoleDownloader;

import java.util.*;
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
		thrd = new Thread(this, "DownLoaderFiles");
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
		return count;
	}
	
	/**
	 * Обработка Url из mapFiles и создание потоков
	 */
	public void run() {
		for(Map.Entry<String, List<String>> entry: mapFiles.entrySet()) {
			System.out.println("Link " + entry.getKey());
			List<DownloadThread> listThreads = new ArrayList<DownloadThread>();
			long contentLength = checkUrl(entry.getKey());
			if (contentLength > 0) {											//файл доступен
				long curCountThreads;
				long partSize;
				if (multiThread) {												//Многопоточная загрузка						
					curCountThreads = calculateCountThreads(contentLength);
					partSize = (long)Math.ceil((float)contentLength / curCountThreads);				
					
				} else {
					curCountThreads = 1;
					partSize = contentLength;
				}
				System.out.println(entry.getKey() + " countThreads=" + curCountThreads + " part_size=" + partSize);
				
				//Устанавливаем настройки для потоков, качающих файл по текущем url
				DownloadThread.url = url;
				DownloadThread.fileName = entry.getValue();
				DownloadThread.outFolder = outFolder;
				DownloadThread.block_size = BLOCK_SIZE;
				DownloadThread.maxSpeed = maxSpeed;
				DownloadThread.countThreads = countThreads;
				DownloadThread.speed = maxSpeed / countThreads;
				
				//Создаем потоки, указывая им необходимый диапазон загрузки файла
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
				System.out.println(entry.getKey() + " download finished.");
			}
		}
	}
}

	/**
	* Поток, реализующий загрузку части файла по http и сохранение на диск
	*/
	class DownloadThread implements Runnable {
		public static URL url;									//url
		public static List<String> fileName;					//Список имен, для сохранения файла
		public static String outFolder;							//Каталог для сохранения
		public static int block_size;							//Размер блока данных, для единичной загрузки
		public static long maxSpeed;							//Максимальная скорость
		public static long speed;								//Скорость каждого потока, меняется от кол-ва потоков
		public static long countThreads;						//Кол-во потоков
		
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
			List<RandomAccessFile> listRAF = new ArrayList<RandomAccessFile>();

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
				//Для каждого имени в списке, открываем файл на запись и выставлем указатель на нужное место в файле
				for(String file: fileName) {
					RandomAccessFile raf = new RandomAccessFile(outFolder + "/" + file, "rw");
					listRAF.add(raf);
					raf.seek(startByte);
				}
				byte data[] = new byte[block_size];
				int numRead = 0;
				long timeStart= System.currentTimeMillis();
				long timeEnd;
				long leftBytes = speed;									//Оставшееся кол-во байт за данную секунду
				while((numRead = in.read(data, 0, block_size)) != -1 )
				{
					for(RandomAccessFile raf: listRAF)					//Записываем в файлы
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
				for (RandomAccessFile raf: listRAF)						//Закрываем все файлы
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
			//System.out.println(thrd.toString() + " downloaded " + downloaded);
		}
	}
}