package it.unisa.di.soa2019;

import java.io.IOException;
import java.util.Map;

public interface PartitioningInterface {
    void callback(Map<Long, Map<String, Long>> mapPartitioning);
    void callback() throws IOException;
}
