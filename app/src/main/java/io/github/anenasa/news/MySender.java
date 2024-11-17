package io.github.anenasa.news;


import android.content.Context;

import androidx.annotation.NonNull;

import org.acra.data.CrashReportData;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

public class MySender implements ReportSender {

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {
        try {
            FileWriter fileWriter = new FileWriter(new File(context.getExternalFilesDir(null), "log.txt"), true);
            BufferedWriter bufferedWriter = new BufferedWriter(fileWriter);
            Map<String, Object> map = report.toMap();
            for(Map.Entry<String, Object> entry: map.entrySet()){
                bufferedWriter.write(entry.getKey());
                bufferedWriter.write(' ');
                bufferedWriter.write(entry.getValue().toString());
                bufferedWriter.write('\n');
            }
            bufferedWriter.close();
        } catch (IOException e) {
            throw new ReportSenderException("ReportSenderException", e);
        }
    }
}
