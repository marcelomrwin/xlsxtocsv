package com.gft.main;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;

import javax.ws.rs.FormParam;

public class MultipartBody implements Serializable {

	private static final long serialVersionUID = 732665616573498660L;

	@FormParam("file")
	private File file;

	@FormParam("fileName")
	private String fileName;

	public long getSize() {
		return file.length();
	}

	public void setFile(File file) {
		this.file = file;
	}

	public InputStream getFile() {
		try {
			return new FileInputStream(file);
		} catch (Exception e) {
			throw new IllegalStateException("Arquivo não encontrado", e);
		}
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("MultipartBody [");
		if (file != null)
			builder.append("file=").append(file).append(", ");
		if (fileName != null)
			builder.append("fileName=").append(fileName);
		builder.append("]");
		return builder.toString();
	}

}
