package com.gft.main;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gft.main.service.GenerateCSVFromStyleSheetService;

import io.quarkus.runtime.StartupEvent;

@ApplicationScoped
public class AppLifecycleBean {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	protected GenerateCSVFromStyleSheetService gen;

	void onStart(@Observes StartupEvent event) {
		logger.info("Quarkus App is starting");
		logger.info("Looking for xlsx files in /deployments/convert folder");
		processFilesInFolder();
	}

	private void processFilesInFolder() {
		try {
			File convert = new File("/deployments/convert");
			if (convert.exists()) {
				File[] workbooks = convert.listFiles(new FileFilter() {
					@Override
					public boolean accept(File file) {
						return file.isFile() && file.getName().endsWith(".xlsx");
					}
				});
				logger.info("find "+workbooks.length+" xlsx files in folder");

				for (File xlsx : workbooks) {

					XSSFWorkbook workbook = new XSSFWorkbook(xlsx);

					POIXMLProperties properties = workbook.getProperties();
					int[] idxSheets = null;
					CTProperty sheetProp = properties.getCustomProperties().getProperty("PathSheets");
					if (sheetProp != null) {
						String sheets = sheetProp.getLpwstr();
						if (sheets != null && !sheets.isEmpty()) {
							idxSheets = Arrays.stream(sheets.split(",")).mapToInt(Integer::valueOf).toArray();
						}
					}

					if (idxSheets == null) {
						int numSheets = workbook.getNumberOfSheets();
						idxSheets = IntStream.rangeClosed(0, numSheets - 1).toArray();
					}

					List<File> csvs = gen.convertExcelToCSV(workbook, idxSheets);
					for (File file : csvs) {
						FileUtils.moveFileToDirectory(file, convert, false);
					}
				}

			}else {
				logger.info("Folder does not exist");
			}
		} catch (Exception e) {
			logger.error("Error on files process");
			logger.error(e.getMessage(), e);
		}
	}

}
