package com.example.modeling.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.function.Supplier;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.CompDeviceWithCooldown;
import com.example.modeling.Model;
import com.example.modeling.Model.ModelStats;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Predicate;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device;

public class Tester {
    private final Supplier<Model> initializer;

    public Tester(Supplier<Model> initializer) {
        this.initializer = initializer;
    }

    public ModelStats test(double time) {
        var proc = this.initializer.get();
        proc.run(time);

        return proc.getStats();
    }

    public static class StatsSaver {
        private final ArrayList<ModelStats> stats = new ArrayList<>();
        
        public void  addStats(ModelStats stat) {
            this.stats.add(stat);
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

                this.addRow(sheet, stats.get(0), 0);
                for (int i = 0; i < stats.size(); i++) {
                    this.addRow(sheet, stats.get(i), i + 1);
                }

                workbook.write(fileOut);
                System.out.println("File created: " + filename);
                
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        void addRow(Sheet sheet, ModelStats data, int index) {
            Row row = sheet.createRow(index);
            var fields = data.get();

            if (index == 0) {
                row.createCell(0).setCellValue("total_time");
            } else {
                row.createCell(0).setCellValue(data.getTotalTime());
            }

            int i = 1;
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
                        row.createCell(i).setCellValue(name + "_throughput");
                    } else {
                        row.createCell(i).setCellValue(st.getThroughput());
                    }

                } else  if (obj instanceof Predicate.Stats) {
                    var st = (Predicate.Stats)obj;
                    var name = st.getName();

                    if (index == 0) {
                        row.createCell(i).setCellValue(name + "_availability"); i++;
                        row.createCell(i).setCellValue(name + "_requests");
                    } else {
                        row.createCell(i).setCellValue(st.getAvailability()); i++;
                        row.createCell(i).setCellValue(st.getRequestsNumber());
                    }

                } else  if (obj instanceof Queue.Stats) {
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
}
