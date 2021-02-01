package com.gft.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.io.FileUtils;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.jboss.resteasy.annotations.providers.multipart.MultipartForm;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.gft.main.service.GenerateCSVFromStyleSheetService;

@Path("/xlsx")
@RequestScoped
public class UploadXlsxResource {
	protected Logger logger = LoggerFactory.getLogger(getClass());

	@Inject
	protected GenerateCSVFromStyleSheetService gen;

	@POST
	@Path("/upload")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response upload(@MultipartForm MultipartBody file) {
		logger.warn(file.toString());

		try {

			String xlsxName = file.getFileName();

			logger.info("Open file %s \n", xlsxName);
			XSSFWorkbook workbook = new XSSFWorkbook(file.getFile());

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

			if (csvs.size() > 1) {
				File zip = new File("csvs.zip");
				try (FileOutputStream fos = new FileOutputStream(zip)) {
					try (ZipOutputStream zipOut = new ZipOutputStream(fos)) {
						for (File csv : csvs) {
							FileInputStream fis = new FileInputStream(csv);
							ZipEntry zipEntry = new ZipEntry(csv.getName());
							zipOut.putNextEntry(zipEntry);

							byte[] bytes = new byte[1024];
							int length;
							while ((length = fis.read(bytes)) >= 0) {
								zipOut.write(bytes, 0, length);
							}
						}
					}
				}
				return responseFileAttachment(zip, "application/zip");
			} else {
				File csv = csvs.get(0);
				return responseFileAttachment(csv, "text/csv");
			}
		} catch (Exception e) {
			return null;
		}

	}

	private Response responseFileAttachment(File attach, String mime) throws IOException {
		byte[] array = FileUtils.readFileToByteArray(attach);
		StreamingOutput stream = readFromByteArray(array);
		ResponseBuilder response = Response.ok(stream, mime);
		response.header("Content-Disposition", "attachment; filename=\"" + attach.getName() + "\"");
		return response.build();
	}

	private StreamingOutput readFromByteArray(byte[] array) {
		return new StreamingOutput() {
			@Override
			public void write(java.io.OutputStream output) throws IOException {
				output.write(array);
				output.flush();
			}
		};
	}

}
