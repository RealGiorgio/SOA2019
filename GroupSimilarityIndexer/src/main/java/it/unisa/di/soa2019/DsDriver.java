package it.unisa.di.soa2019;

import it.unisa.di.soa2019.indexing.IndexingMapper;
import it.unisa.di.soa2019.indexing.IndexingReducer;
import it.unisa.di.soa2019.indexing.StringWritable;
import it.unisa.di.soa2019.partitioning.GroupPartitioningMapper;
import it.unisa.di.soa2019.partitioning.GroupPartitioningReducer;
import it.unisa.di.soa2019.similarity.SimilarityMapper;
import it.unisa.di.soa2019.similarity.SimilarityReducer;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.MapWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.NLineInputFormat;
import org.apache.hadoop.mapreduce.lib.input.SequenceFileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.SequenceFileOutputFormat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class DsDriver {
    private static boolean USE_NLINE_SPLITTER = true;

    public static void main(String args[]) throws Exception {
        Configuration conf = new Configuration();
        String input = args[0];

        Job j1 = Job.getInstance(conf, "DocSim - Group partitioning");
        conf.set("mapreduce.input.fileinputformat.split.maxsize", "73400320");
        conf.set("mapreduce.map.memory.mb", "2048");
        conf.set("mapreduce.map.java.opts", "-Xmx1638m");
        j1.setJarByClass(DsDriver.class);
        j1.setMapperClass(GroupPartitioningMapper.class);
        j1.setReducerClass(GroupPartitioningReducer.class);
        if (USE_NLINE_SPLITTER) {
            j1.setInputFormatClass(NLineInputFormat.class);
            NLineInputFormat.addInputPath(j1, new Path(input));
            j1.getConfiguration().setInt("mapreduce.input.lineinputformat.linespermap", 662393); // 127179552(total msgs)/192(cores)

        } else {
            FileInputFormat.addInputPath(j1, new Path(input));
        }

        j1.setMapOutputKeyClass(Text.class);
        j1.setMapOutputValueClass(MapWritable.class);
        j1.setOutputKeyClass(Text.class);
        j1.setOutputValueClass(Text.class);
        j1.setNumReduceTasks(1);
//        j1.setOutputFormatClass(SequenceFileOutputFormat.class);
        Date date = Calendar.getInstance().getTime();
        DateFormat dateFormat = new SimpleDateFormat("yyyymmdd_hhmmss");
        String strDate = dateFormat.format(date);
        FileOutputFormat.setOutputPath(j1, new Path(args[1] + strDate + "inter"));


        boolean err = j1.waitForCompletion(true);
        if (!err) {
            System.exit(-1);
        }

        conf = new Configuration();
        conf.set("mapreduce.input.fileinputformat.split.maxsize", "8388608");
        conf.set("mapreduce.map.memory.mb", "2048");
        conf.set("mapreduce.map.java.opts", "-Xmx1638m");
        conf.set("mapreduce.reduce.memory.mb", "4096");
        conf.set("mapreduce.reduce.java.opts", "-Xmx3286m");
        conf.set("mapred.max.split.size", "8388608");
        Job j2 = Job.getInstance(conf, "DocSim - Indexing");

        j2.setJarByClass(DsDriver.class);
        j2.setMapperClass(IndexingMapper.class);
        j2.setReducerClass(IndexingReducer.class);

        j2.setMapOutputKeyClass(Text.class);
        j2.setMapOutputValueClass(StringWritable.class);
        j2.setOutputKeyClass(Text.class);
        j2.setOutputValueClass(MapWritable.class);
//        j2.setInputFormatClass(SequenceFileInputFormat.class);
        j2.setOutputFormatClass(SequenceFileOutputFormat.class);
        FileInputFormat.addInputPath(j2, new Path(args[1] + strDate + "inter"));
        FileOutputFormat.setOutputPath(j2, new Path(args[1] + strDate + "indexer"));

        err = j2.waitForCompletion(true);
        if (!err) {
            System.exit(-1);
        }

        conf = new Configuration();
        conf.set("mapreduce.input.fileinputformat.split.maxsize", "8388608");
        conf.set("mapreduce.map.memory.mb", "2048");
        conf.set("mapreduce.map.java.opts", "-Xmx1638m");
        conf.set("mapreduce.reduce.memory.mb", "4096");
        conf.set("mapreduce.reduce.java.opts", "-Xmx3286m");
        conf.set("mapred.max.split.size", "8388608");
//        conf.set("mapred.tasktracker.map.tasks.maximum", "6");
        Job j3 = Job.getInstance(conf, "DocSim - Similarity");

        j3.setJarByClass(DsDriver.class);
        j3.setMapperClass(SimilarityMapper.class);
        j3.setReducerClass(SimilarityReducer.class);
        j3.setMapOutputKeyClass(StringWritable.class);
        j3.setMapOutputValueClass(DoubleWritable.class);
        j3.setOutputKeyClass(StringWritable.class);
        j3.setOutputValueClass(DoubleWritable.class);
        j3.setInputFormatClass(SequenceFileInputFormat.class);
        j3.setNumReduceTasks(1);

        FileInputFormat.addInputPath(j3, new Path(args[1] + strDate + "indexer"));
        FileOutputFormat.setOutputPath(j3, new Path(args[1] + strDate + "final"));

        err = j3.waitForCompletion(true);
        if (!err) {
            System.exit(-1);
        }
    }

}
