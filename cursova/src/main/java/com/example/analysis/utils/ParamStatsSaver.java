package com.example.analysis.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.analysis.modelInit.ParamResultCalculator;
import com.example.modeling.utils.Pair;

public class ParamStatsSaver {
    private final String filename;
    private final ArrayList<Pair<ParamResultCalculator, Integer>> statsBuffer = new ArrayList<>();

    public ParamStatsSaver(String filename) {
        this.filename = filename;

        this.deleteFile();

        File file = new File(this.filename);
        if (!file.exists()) {
            try (Workbook workbook = new XSSFWorkbook(); 
                FileOutputStream fileOut = new FileOutputStream(this.filename)) {

                Sheet sheet = workbook.createSheet("Data");
                this.createHeaderRow(sheet);

                workbook.write(fileOut);
                System.out.println("File created: " + this.filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void deleteFile() {
        File file = new File(this.filename);

        if (file.exists()) {
            if (file.delete()) {
                System.out.println("File deleted successfully: " + this.filename);
            } else {
                System.err.println("Error: Cannot delete file (it might be open in Excel): " + this.filename);
            }
        } else {
            System.out.println("File not found (nothing to delete): " + filename);
        }
    }

    public void addStats(ParamResultCalculator stat, int mask) {
        this.statsBuffer.add(new Pair<ParamResultCalculator,Integer>(stat, mask));
    }

    public void addStats(ArrayList<Pair<ParamResultCalculator, Integer>> buff) {
        this.statsBuffer.addAll(buff);
    }

    public void appendToFile() {
        File file = new File(this.filename);

        if (statsBuffer.isEmpty()) {
            System.out.println("Buffer is empty, nothing to append.");
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis);
             FileOutputStream fileOut = new FileOutputStream(file)) {

            Sheet sheet = workbook.getSheet("Data");
            
            if (sheet == null) {
                sheet = workbook.createSheet("Data");
                createHeaderRow(sheet);
            }
            fis.close();

            int lastRowNum = sheet.getLastRowNum();
            this.writeBufferToSheet(sheet, lastRowNum + 1);

            workbook.write(fileOut);
            statsBuffer.clear();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createHeaderRow(Sheet sheet) {
        Row row = sheet.createRow(0);

        row.createCell(0).setCellValue("mask");

        row.createCell(1).setCellValue("buldozerNum");
        row.createCell(2).setCellValue("loader1Num");
        row.createCell(3).setCellValue("loader2Num");
        row.createCell(4).setCellValue("truckNum");

        row.createCell(5).setCellValue("prod_served");

        row.createCell(6).setCellValue("mean_q_size");
        row.createCell(7).setCellValue("max_q_size");
        row.createCell(8).setCellValue("mean_wait_q");

        row.createCell(9).setCellValue("mean_loader_q_size");
        row.createCell(10).setCellValue("mean_loader_wait_q");

        row.createCell(11).setCellValue("mean_truck_q_size");
        row.createCell(12).setCellValue("mean_truck_wait_q");

        row.createCell(13).setCellValue("productivity");
        row.createCell(14).setCellValue("processing_time");
    }

    private void writeBufferToSheet(Sheet sheet, int startRowIndex) {
        int currentRowIndex = startRowIndex;

        for (var st : statsBuffer) {
            int mask = st.get1();
            var data = st.get0();

            Row row = sheet.createRow(currentRowIndex++);

            row.createCell(0).setCellValue(mask);

            row.createCell(1).setCellValue(data.buldozerNum());
            row.createCell(2).setCellValue(data.loader1Num());
            row.createCell(3).setCellValue(data.loader2Num());
            row.createCell(4).setCellValue(data.truckNum());

            row.createCell(5).setCellValue(data.prod_served().get());

            row.createCell(6).setCellValue(data.mean_q_size().get());
            row.createCell(7).setCellValue(data.max_q_size().get());
            row.createCell(8).setCellValue(data.mean_wait_q().get());

            row.createCell(9).setCellValue(data.mean_loader_q_size().get());
            row.createCell(10).setCellValue(data.mean_loader_wait_q().get());

            row.createCell(11).setCellValue(data.mean_truck_q_size().get());
            row.createCell(12).setCellValue(data.mean_truck_wait_q().get());

            row.createCell(13).setCellValue(data.productivity().get());
            row.createCell(14).setCellValue(data.processing_time().get());
        }
    }
}
