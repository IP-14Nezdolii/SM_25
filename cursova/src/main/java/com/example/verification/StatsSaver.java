package com.example.verification;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.modeling.utils.Pair;

public class StatsSaver {
    private final ArrayList<Pair<ResultCalculator, Integer>> stats = new ArrayList<>();

    public void  addStats(ResultCalculator stat, int testMask) {
        this.stats.add(Pair.createPair(stat, testMask));
    }

    public void save(String filename) {
        File file = new File(filename);

        if (file.exists()) {
            if (!file.delete()) {
                System.err.println("Delete file error: " + filename);
                return;
            }
        }

        try (Workbook workbook = new XSSFWorkbook(); 
            FileOutputStream fileOut = new FileOutputStream(filename)) {
            Sheet sheet = workbook.createSheet("Data");

            if (stats.isEmpty()) {
                throw new IllegalStateException("Cannot save stats: no data collected.");
            }

            Pair<ResultCalculator, Integer> pair = stats.get(0);

            this.addRow(sheet, pair.get0(),pair.get1(), 0);

            for (int i = 0; i < stats.size(); i++) {
                pair = stats.get(i);
                this.addRow(sheet, pair.get0(), pair.get1(), i + 1);
            }

            workbook.write(fileOut);
            System.out.println("File created: " + filename);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void addRow(Sheet sheet, ResultCalculator data, int testMask, int index) {
        Row row = sheet.createRow(index);

        if (index == 0) {
            row.createCell(0).setCellValue("test_mask");
            row.createCell(1).setCellValue("prod_served");
            row.createCell(2).setCellValue("mean_q_size");
            row.createCell(3).setCellValue("q_served");
            row.createCell(4).setCellValue("m_loader_util");
            row.createCell(5).setCellValue("m_truck_util");
            row.createCell(6).setCellValue("productivity");
            row.createCell(7).setCellValue("processing_time");
        } else {
            row.createCell(0).setCellValue(testMask);
            row.createCell(1).setCellValue(data.prod_served().get());
            row.createCell(2).setCellValue(data.mean_q_size().get());
            row.createCell(3).setCellValue(data.q_served().get());
            row.createCell(4).setCellValue(data.m_loader_util().get());
            row.createCell(5).setCellValue(data.m_truck_util().get());
            row.createCell(6).setCellValue(data.productivity().get());
            row.createCell(7).setCellValue(data.processing_time().get());
        }
    }
}
