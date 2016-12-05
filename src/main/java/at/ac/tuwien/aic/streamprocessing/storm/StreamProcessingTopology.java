package at.ac.tuwien.aic.streamprocessing.storm;

import at.ac.tuwien.aic.streamprocessing.storm.bolt.CalculateAverageSpeedBolt;
import at.ac.tuwien.aic.streamprocessing.storm.bolt.CalculateDistanceBolt;
import at.ac.tuwien.aic.streamprocessing.storm.bolt.WindowedCalculateSpeedBolt;
import at.ac.tuwien.aic.streamprocessing.storm.spout.KafkaDataSpout;
import org.apache.storm.Config;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.kafka.ZkHosts;

import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.tuple.Fields;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class StreamProcessingTopology {

    public static StormTopology buildTopology() {

        TopologyBuilder builder = new TopologyBuilder();

        ZkHosts hosts = new ZkHosts("localhost:2181", "");

        // attach the kafka data spout to the topology - no parallelism
        // NOTE: this is just to make sure we keep the data ordered. The kafka spout should have parallelism.
        //       we will probably need the trident framework inorder to achieve this.
        KafkaDataSpout spout = KafkaDataSpout.create(hosts, "test");
        builder.setSpout("kafka-spout", spout, 1);

        // attach the calculate speed bolt using fields grouping - parallelism of 15
        //builder.setBolt("calculate-speed-bolt", new CalculateSpeedBolt(), 15).fieldsGrouping("test-taxi-data-spout", new Fields("id"));
        builder.setBolt("w-calculate-speed-bolt", new WindowedCalculateSpeedBolt().withWindow(new BaseWindowedBolt.Count(2)), 15)
                .fieldsGrouping("kafka-spout", new Fields("id"));
        // attach the calculate average speed bolt using fields grouping - parallelism of 15
        builder.setBolt("calculate-average-speed-bolt", new CalculateAverageSpeedBolt(), 2).fieldsGrouping("w-calculate-speed-bolt", new Fields("id"));

        // attach the calculate distance bolt using fields grouping - parallelism of 15
        builder.setBolt("calculate-distance", new CalculateDistanceBolt(), 15).fieldsGrouping("kafka-spout", new Fields("id"));

        return builder.createTopology();
    }

    public static void main(String[] args) throws Exception {
        final Logger logger = LoggerFactory.getLogger(StreamProcessingTopology.class);

        // create the default config object and set the number of threads to run (similar to setting number of workers in live cluster)
        Config conf = new Config();
        conf.put("topology.eventlogger.executors",2); // TODO check if this has any effect
        conf.setDebug(true); // TODO check if this has any effect
        conf.setMaxTaskParallelism(2);

        StormSubmitter.submitTopology("taxicab-0_0_1",conf,buildTopology());
        logger.debug("submitted Topology");

//            LocalCluster cluster = new LocalCluster();
//
//            // submit the topology to the local cluster
//            cluster.submitTopology("stream-processing", conf, builder.createTopology());
//
//            // run the topology for 20 seconds
//            Thread.sleep(20000);
//
//            // shutdown the local cluster
//            cluster.shutdown();
    }
}
