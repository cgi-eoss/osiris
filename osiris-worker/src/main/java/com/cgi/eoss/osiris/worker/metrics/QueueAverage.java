package com.cgi.eoss.osiris.worker.metrics;

import lombok.Data;

@Data
public class QueueAverage {

    private final long count;
    
    private final double averageLength;
    
}
