package com.example.test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.CompDeviceWithCooldown;
import com.example.modeling.Model.ModelStats;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device;
import com.example.modeling.utils.Pair;

public class StatsSaver {
    private final ArrayList<Pair<ModelStats, Integer>> stats = new ArrayList<>();
    
    public void  addStats(ModelStats stat, int testMask) {
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

            Pair<ModelStats, Integer> pair = stats.get(0);

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

    void addRow(Sheet sheet, ModelStats data, int testMask, int index) {
        Row row = sheet.createRow(index);
        var fields = data.get();

        if (index == 0) {
            row.createCell(0).setCellValue("test_mask");
            row.createCell(1).setCellValue("total_time");
        } else {
            row.createCell(0).setCellValue(testMask);
            row.createCell(1).setCellValue(data.getTotalTime());
        }

        int i = 2;
        for (var obj : fields) {

            if (obj instanceof Device.Stats) {
                var st = (Device.Stats)obj;
                var name = st.getName();

                if (index == 0) {
                    row.createCell(i).setCellValue(name + "_utilization"); i++;
                    row.createCell(i).setCellValue(name + "_served");
                } else {
                    row.createCell(i).setCellValue(st.getUtilization()); i++;
                    row.createCell(i).setCellValue(st.getServed());
                }

            } else if (obj instanceof CompDeviceWithCooldown.Stats) {
                var st = (CompDeviceWithCooldown.Stats)obj;
                var name = st.getName();

                if (index == 0) {
                    row.createCell(i).setCellValue(name + "_utilization"); i++;
                    row.createCell(i).setCellValue(name + "_busyTime"); i++;
                    row.createCell(i).setCellValue(name + "_cooldownTime"); i++;
                    row.createCell(i).setCellValue(name + "_served");
                } else {
                    row.createCell(i).setCellValue(st.getUtilization()); i++;
                    row.createCell(i).setCellValue(st.getBusyTime()); i++;
                    row.createCell(i).setCellValue(st.getCooldownTime()); i++;
                    row.createCell(i).setCellValue(st.getServed());
                }

            } else if (obj instanceof Connection.Stats) {
                var st = (Connection.Stats)obj;
                var name = st.getName();

                if (index == 0) {
                    row.createCell(i).setCellValue(name + "_throughput"); i++;
                    row.createCell(i).setCellValue(name + "_availability");
                } else {
                    row.createCell(i).setCellValue(st.getThroughput()); i++;
                    row.createCell(i).setCellValue(st.getAvailability());
                }

            } else if (obj instanceof Queue.Stats) {
                var st = (Queue.Stats)obj;
                var name = st.getName();

                if (index == 0) {
                    row.createCell(i).setCellValue(name + "_avg_sz"); i++;
                    row.createCell(i).setCellValue(name + "_avg_wt"); i++;
                    row.createCell(i).setCellValue(name + "_pair_avg_sz"); i++;
                    row.createCell(i).setCellValue(name + "_pair_avg_wt"); i++;
                    row.createCell(i).setCellValue(name + "_served"); i++;
                    row.createCell(i).setCellValue(name + "_total_wait_time");
                } else {
                    row.createCell(i).setCellValue(st.getAverageQueueSize()); i++;
                    row.createCell(i).setCellValue(st.getAvgWaitTime()); i++;
                    row.createCell(i).setCellValue(st.getAverageBatchQueueSize(2)); i++;
                    row.createCell(i).setCellValue(st.getAvgBatchWaitTime(2)); i++;
                    row.createCell(i).setCellValue(st.getServed()); i++;
                    row.createCell(i).setCellValue(st.getTotalWaitTime());
                }
            } else {
                throw new IllegalArgumentException("Unsupported stats type: " + obj.getClass().getName());
            }

            i++;
        }
    }
}
