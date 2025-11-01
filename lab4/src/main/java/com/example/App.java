package com.example;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.example.modeling.components.CompDevice;
import com.example.modeling.components.Component;
import com.example.modeling.components.Connection;
import com.example.modeling.components.Model;
import com.example.modeling.components.Queue;
import com.example.modeling.components.device.Device.DeviceRand;

public class App 
{
    public static void main( String[] args ) throws InterruptedException
    {
        final int nSamples = 10;

        var simtest = new Simulator();
        for (int i = 1000; i <= 10_000; i+=1000) {
            simtest.simulate(createModel1(i, Model.getExponential(25)), i, nSamples);
            System.out.println(i);
        }

        /////
        var sim1 = new Simulator();
        for (int i = 1000; i <= 10_000; i+=1000) {
            sim1.simulate(createModel1(i, Model.getExponential(25)), i, nSamples);
            System.out.println(i);
        }
        sim1.saveResults("file1.xlsx");

        var sim2 = new Simulator();
        for (int i = 1000; i <= 10_000; i+=1000) {
            
            sim2.simulate(createModel2(i, 10, Model.getExponential(25)), i, nSamples);
            System.out.println(i);
        }
        sim2.saveResults("file2.xlsx");
    }

    public static Component[] createModel1(int nSystem, DeviceRand device) {
        var elems = new Component[nSystem * 2];

        for (int i = 0; i < elems.length; i+=2) {
            elems[i] = new Queue("Q" + i);
            elems[i+1] = new CompDevice(device, "Device"+ i);
        }

        for (int i = 0; i < elems.length - 1; i++) {
            elems[i].setNext(elems[i + 1]);
        }

        return elems;
    }

    public static Component[] createModel2(int nSystem, int nDevicePerSystem, DeviceRand device) {
        int systemElemN = 1 + 1 + nDevicePerSystem;
        var elems = new Component[nSystem * systemElemN];

        CompDevice[] lastDevices = null;

        for (int i = 0; i < elems.length; i+=systemElemN) {

            var q = new Queue("Q" + i / systemElemN);
            var con = new Connection(new Model.Priority(), "Con" + i);

            elems[i] = q;
            elems[i+1] = con;
     
            if (lastDevices != null) {
                for (var dev : lastDevices) {
                    dev.setNext(q);
                }
            }
            q.setNext(con);

            var devices = new CompDevice[nDevicePerSystem];
            for (int j = 0; j < nDevicePerSystem; j++) {
                devices[j] = new CompDevice(device, "Dev" + i + "-" + j);
                elems[i + 2 + j] = devices[j];

                con.addNext(devices[j], 1);
            }

            lastDevices = devices;
        }

        for (var dev : elems) {
            if (dev == null) {
                throw new RuntimeException("BAD CODE!");
            }
        }

        return elems;
    }

    public static class Simulator {
        ArrayList<Record> records = new ArrayList<>();

        public void simulate(Component[] model, int nSystem, int nSamples) {
            long time = System.currentTimeMillis();

            for (int i = 0; i < nSamples; i++) {
                model[0].process();
                
                var t = this.getWorkTime(model);
                while (t.isPresent()) {
                    this.runnAllElems(model, t.get());
                    t = getWorkTime(model);
                }
            }

            double result = (double)(System.currentTimeMillis() - time) /(double)nSamples; 
            records.add(new Record(nSystem, result / 1000.0));
        }

        private void runnAllElems(Component[] model, double time) {
            List.of(model).reversed().forEach(elem -> {
                elem.run(time);
            });
        }

        private Optional<Double> getWorkTime(Component[] model) {
            double time = Double.MAX_VALUE;
            for (Component elem : model) {
                Optional<Double> elemTime = elem.getWorkTime();

                if (elemTime.isPresent()) {
                    time = Math.min(time, elemTime.get());
                }
            }

            return time != Double.MAX_VALUE 
                ? Optional.of(time) 
                : Optional.empty();
        }

        public void saveResults(String filename) {
            File file = new File(filename);

            if (file.exists()) {
                if (!file.delete()) {
                    System.err.println("Delete file error: " + filename);
                    return;
                }
            }

            try (
                Workbook workbook = new XSSFWorkbook(); 
                FileOutputStream fileOut = new FileOutputStream(filename)
            ) {
                Sheet sheet = workbook.createSheet("Data");

                Row headerRow = sheet.createRow(0);

                headerRow.createCell(0).setCellValue("nSystem");
                headerRow.createCell(1).setCellValue("time");

                int rowNum = 1;
                for (Record record : records) {
                    Row row = sheet.createRow(rowNum++);

                    row.createCell(0).setCellValue(record.nSystem);
                    row.createCell(1).setCellValue(record.time);
                }

                workbook.write(fileOut);
                System.out.println("File created: " + filename);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static class Record {
        public int nSystem;
        public double time;

        public Record(int nSystem, double time) {
            this.nSystem = nSystem;
            this.time = time;
        }
    }
}
