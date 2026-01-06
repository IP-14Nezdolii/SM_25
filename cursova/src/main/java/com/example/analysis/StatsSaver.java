package com.example.analysis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.modeling.utils.Pair;

public class StatsSaver {
    private final String filename;
    private final ArrayList<Pair<ResultCalculator, Integer>> statsBuffer = new ArrayList<>();

    public StatsSaver(String filename) {
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

    public void addStats(ResultCalculator stat, int testMask) {
        this.statsBuffer.add(Pair.createPair(stat, testMask));
    }

    public void addStats(ArrayList<Pair<ResultCalculator, Integer>> buff) {
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
        row.createCell(0).setCellValue("test_mask");

        row.createCell(1).setCellValue("prod_served");

        row.createCell(2).setCellValue("mean_q_size");
        row.createCell(3).setCellValue("max_q_size");
        row.createCell(4).setCellValue("mean_wait_q");

        row.createCell(5).setCellValue("loader1_served");
        row.createCell(6).setCellValue("loader2_served");
        row.createCell(7).setCellValue("mean_loader_q_size");
        row.createCell(8).setCellValue("mean_loader_wait_q");

        row.createCell(9).setCellValue("mean_truck_q_size");
        row.createCell(10).setCellValue("mean_truck_wait_q");

        row.createCell(11).setCellValue("productivity");
        row.createCell(12).setCellValue("processing_time");
    }

    private void writeBufferToSheet(Sheet sheet, int startRowIndex) {
        int currentRowIndex = startRowIndex;

        for (Pair<ResultCalculator, Integer> pair : statsBuffer) {
            ResultCalculator data = pair.get0();
            int testMask = pair.get1();

            Row row = sheet.createRow(currentRowIndex++);

            row.createCell(0).setCellValue(testMask);

            row.createCell(1).setCellValue(data.prod_served().get());

            row.createCell(2).setCellValue(data.mean_q_size().get());
            row.createCell(3).setCellValue(data.max_q_size().get());
            row.createCell(4).setCellValue(data.mean_wait_q().get());

            row.createCell(5).setCellValue(data.loader1_served().get());
            row.createCell(6).setCellValue(data.loader2_served().get());
            row.createCell(7).setCellValue(data.mean_loader_q_size().get());
            row.createCell(8).setCellValue(data.mean_loader_wait_q().get());

            row.createCell(9).setCellValue(data.mean_truck_q_size().get());
            row.createCell(10).setCellValue(data.mean_truck_wait_q().get());

            row.createCell(11).setCellValue(data.productivity().get());
            row.createCell(12).setCellValue(data.processing_time().get());
        }
    }
}
