package com.ConsoleDownloader;

import static org.junit.Assert.*;

import java.io.*;
import java.util.*;

import org.junit.*;

public class TestAnalyzeParameters {
	public static Map<String, List<String>> testLinks;
	
	/**
	 * Подготавливаем файл ссылками и именами
	 */
	@Before
	  public void setUpParameters() {
	    testLinks = new HashMap<String, List<String>> ();
	    List<String> files = new ArrayList<String>();
	    files.add("path1.zip");
	    files.add("path1_copy.zip");
	    testLinks.put("http://path1.zip", files);
	    files = new ArrayList<String>();
	    files.add("path2.zip");
	    testLinks.put("http://path2.zip", files);
	    FileWriter fw = null;
	    try {
		    fw = new FileWriter("links.txt");
		    for(Map.Entry<String, List<String>> entry: testLinks.entrySet())
		    	for(String str: entry.getValue())
		    		fw.write(entry.getKey() + " " + str + "\r\n");
	    } catch (IOException e){
	    	e.printStackTrace();
	    }
	    finally  {
	    	if (fw != null)
				try {
					fw.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
	    }
	  }
	
	/**
	 * Тест для проверки правильности ввода всех параметров
	 */
	@Test
	public void testNormParameters() {
		String parameters = "-n 5 -l 2000k -o test -f links.txt";
		AnalyzeParameters analyzeParameters = null;
		try {
			analyzeParameters = new AnalyzeParameters(parameters.split(" "));
		} catch (BadParamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			fail("Not yet implemented");
		}
		assertEquals(analyzeParameters.getCountThreads(), 5);						//Проверка на кол-во потоков
		assertEquals(analyzeParameters.getSpeed(), 2000*1024);						//Проверка на максимальную скорость
		File file = new File("test");
		assertTrue(file.exists());													//Проверка на созданный каталог
		Map<String, List<String>> recvLinks= analyzeParameters.getListFiles();
		assertTrue(recvLinks.equals(testLinks));									//Проверка на правильность ссылок и имен, прочитанных из файла
	}
	
	/**
	 * Тест для проверки ввода колва потоков 
	 */
	@Test
	public void testBadParametersCountThreads() {
		String parameters = "-n op -l 2000k -o test -f links.txt";
		AnalyzeParameters analyzeParameters = null;
		try {
			analyzeParameters = new AnalyzeParameters(parameters.split(" "));
		} catch (BadParamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		fail("Not yet implemented");
	}
	
	/**
	 * Тест для проверки ввода максимальной скорости 
	 */
	@Test
	public void testBadParametersSpeed() {
		String parameters = "-n 5 -l 2000mk -o test -f links.txt";
		AnalyzeParameters analyzeParameters = null;
		try {
			analyzeParameters = new AnalyzeParameters(parameters.split(" "));
		} catch (BadParamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		fail("Not yet implemented");
	}
	
	/**
	 * Тест для проверки ввода максимальной скорости 
	 */
	@Test
	public void testBadParametersCountParam() {
		String parameters = "-n 5 -l 2000mk -o test -f ";
		AnalyzeParameters analyzeParameters = null;
		try {
			analyzeParameters = new AnalyzeParameters(parameters.split(" "));
		} catch (BadParamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		fail("Not yet implemented");
	}
		
	/**
	 * Удаляем файл и каталог
	 */
	@After
	public void tearParameters() {
		File file = new File("links.txt");
		file.delete();
		file = new File("test");
		file.delete();
	}

}
