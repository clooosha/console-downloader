package com.ConsoleDownloader;

import java.util.*;

public class ConsoleDownloader {

	public static void main(String[] args) {
		long timeStart = System.currentTimeMillis();	//Время старта программы
		long bytes = 0;									//Кол-во байт
	
		Map<String, List<String>> listFiles;			//Для каждого url список имен, для сохранения
		int countThreads;								//Кол-во потоков
		long speed;										//Скорость закачки в байтах.
		String folder;									//Каталог для сохранения		
		
		//Анализ принятых параметров
		try {
			AnalyzeParameters param= new AnalyzeParameters(args);
			listFiles = param.getListFiles();
			if (listFiles == null) {
				return;
			}
			countThreads = param.getCountThreads();
			speed = param.getSpeed();
			folder = param.getFolder();
		} catch (BadParamException e)
		{
			System.out.println(e);
			System.out.println("Example usage: ConsoleDownloader [-n 5 -l 2000k -o output_folder -f links.txt]");
			return;
		}
		System.out.println("Max speed=" + speed + ", countThreads=" + countThreads + ", folder=" + folder);
		
		DownLoaderFiles downloaderFiles = new DownLoaderFiles(listFiles, folder, countThreads, speed);
		try {
			downloaderFiles.thrd.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		bytes = downloaderFiles.totalDownloaded;	//Всего загружено байт
		
		long timeEnd = System.currentTimeMillis();	//
		System.out.println("Finished after " + ((float)(timeEnd - timeStart)/1000) + " seconds. Download " + bytes + " bytes.");
	}
}
