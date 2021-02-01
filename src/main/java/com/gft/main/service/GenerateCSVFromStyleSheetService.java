package com.gft.main.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.enterprise.context.ApplicationScoped;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

@ApplicationScoped
public class GenerateCSVFromStyleSheetService {

	static Pattern rxquote = Pattern.compile("\"");

	static String encodeValue(String value) {
		boolean needQuotes = false;
		if (value.indexOf(';') != -1 || value.indexOf('"') != -1 || value.indexOf('\n') != -1
				|| value.indexOf('\r') != -1)
			needQuotes = true;
		Matcher m = rxquote.matcher(value);
		if (m.find())
			needQuotes = true;
		value = m.replaceAll("\"\"");
		if (needQuotes)
			return "\"" + value + "\"";
		else
			return value;

	}

	public List<File> convertExcelToCSV(XSSFWorkbook workbook, int[] sheets) throws Exception {
		FormulaEvaluator fe = workbook.getCreationHelper().createFormulaEvaluator();
		DataFormatter formatter = new DataFormatter();
		List<File> csvs = new ArrayList<File>();
		//
		for (int idx : sheets) {
			XSSFSheet sheet = workbook.getSheetAt(idx);

			File csvFile = new File(sheet.getSheetName().toLowerCase() + ".csv");

			PrintStream out = new PrintStream(new FileOutputStream(csvFile), true, "UTF-8");
			byte[] bom = { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
			out.write(bom);

			boolean firstRow = true;
			long headers = 0;

			Iterator<Row> rowIterator = sheet.iterator();
			while (rowIterator.hasNext()) {
				StringBuilder sb = new StringBuilder();
				Row row = rowIterator.next();

				Iterator<Cell> cellIterator = row.cellIterator();
				boolean firstCell = true;
				while (cellIterator.hasNext()) {
					Cell cell = cellIterator.next();

					if (firstRow)
						headers++;

					if (!firstCell) {
						sb.append(";");
					}

					if (cell != null && cell.getCellType() != CellType.BLANK) {
						cell = fe.evaluateInCell(cell);
						String value = formatter.formatCellValue(cell);

						if (cell.getCellType() == CellType.FORMULA) {
							value = formatter.formatCellValue(cell, fe);
						}

						sb.append(encodeValue(value));

					}

					firstCell = false;
				}

				if (!sb.toString().endsWith(";"))
					sb.append(";");

				if (!firstRow) {

					long count = sb.toString().chars().filter(ch -> ch == ';').count();
					++count;

					if (count < headers) {
						long diff = headers - count;
						for (long i = 0; i < diff; i++)
							sb.append(";");
					}
				}

				out.println(sb.toString());

				firstRow = false;
			}

			out.close();
			csvs.add(csvFile);
		}
		//
		return csvs;
	}
}
