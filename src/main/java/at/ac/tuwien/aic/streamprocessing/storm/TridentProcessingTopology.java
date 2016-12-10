package at.ac.tuwien.aic.streamprocessing.storm;

import at.ac.tuwien.aic.streamprocessing.kafka.utils.LocalKafkaInstance;
import at.ac.tuwien.aic.streamprocessing.storm.spout.TaxiEntryKeyValueScheme;
import at.ac.tuwien.aic.streamprocessing.storm.trident.CalculateAverageSpeed;
import at.ac.tuwien.aic.streamprocessing.storm.trident.CalculateDistance;
import at.ac.tuwien.aic.streamprocessing.storm.trident.CalculateSpeed;
import at.ac.tuwien.aic.streamprocessing.storm.tuple.TaxiFields;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.generated.StormTopology;
import org.apache.storm.kafka.KeyValueSchemeAsMultiScheme;
import org.apache.storm.kafka.ZkHosts;
import org.apache.storm.kafka.trident.OpaqueTridentKafkaSpout;
import org.apache.storm.kafka.trident.TridentKafkaConfig;
import org.apache.storm.trident.Stream;
import org.apache.storm.trident.TridentTopology;
import org.apache.storm.trident.operation.BaseFilter;
import org.apache.storm.trident.operation.builtin.Debug;

public class TridentProcessingTopology {
    private static final String SPOUT_ID = "kafka-spout";

    private String topic;

    private final String redisHost;
    private final int redisPort;

    private final BaseFilter speedHook;
    private final BaseFilter avgSpeedHook;
    private final BaseFilter distanceHook;

    private LocalKafkaInstance localKafkaInstance;
    private TridentTopology topology;
    private LocalCluster cluster;

    private boolean stopped = false;

    public TridentProcessingTopology(String topic, String redisHost, int redisPort) {
        this.topic = topic;
        this.redisHost = redisHost;
        this.redisPort = redisPort;

        this.speedHook = null;
        this.avgSpeedHook = null;
        this.distanceHook = null;
    }

    public TridentProcessingTopology(String topic, String redisHost, int redisPort, BaseFilter speedHook, BaseFilter avgSpeedHook, BaseFilter distanceHook) {
        this.topic = topic;
        this.redisHost = redisHost;
        this.redisPort = redisPort;
        this.speedHook = speedHook;
        this.avgSpeedHook = avgSpeedHook;
        this.distanceHook = distanceHook;
    }

    public void stop() {
        if (stopped) {
            return;
        }

        stopped = true;

        try {
            cluster.shutdown();
            stopKafka();
        } catch (Exception e) {
            System.out.println("Failed to stop cluster.");
            e.printStackTrace();

            System.exit(1);
        }
    }

    private void startKafka() {
        localKafkaInstance = new LocalKafkaInstance(9092, 2000);

        try {
            localKafkaInstance.start();
        } catch (Exception e) {
            System.out.println("Caught exception while starting kafka. Aborting");
            e.printStackTrace();

            System.exit(1);
        }

        localKafkaInstance.createTopic(topic);
    }

    private void stopKafka() {
        try {
            localKafkaInstance.stop();
        } catch (Exception e) {
            System.out.println("Caught exception while stopping kafka. Aborting");
            e.printStackTrace();

            System.exit(1);
        }
    }

    public StormTopology build() {
        if (localKafkaInstance == null) {
            throw new IllegalStateException("Must start kafka before building topology");
        }

        topology = new TridentTopology();

        OpaqueTridentKafkaSpout spout = buildKafkaSpout();


        // setup topology
        Stream inputStream = topology.newStream(SPOUT_ID, spout);

        // setup speed aggregator
        Stream speedStream = inputStream
                .partitionAggregate(TaxiFields.BASE_FIELDS, new CalculateSpeed(), TaxiFields.BASE_SPEED_FIELDS)
                .toStream();

        if (speedHook != null) {
            speedStream = speedStream.each(TaxiFields.BASE_SPEED_FIELDS, speedHook);
        }

        // setup average speed aggregator
        speedStream = speedStream
                .partitionAggregate(TaxiFields.BASE_SPEED_FIELDS, new CalculateAverageSpeed(), TaxiFields.BASE_SPEED_AVG_FIELDS)
                .toStream();

        if (avgSpeedHook != null) {
            speedStream = speedStream.each(TaxiFields.BASE_SPEED_AVG_FIELDS, avgSpeedHook);
        }

        // TODO: enable
        // speedStream.each(taxiFieldsWithAvgSpeed, new StoreInformation(OperatorType.AVERGAGE_SPEED));

        // setup distance aggregator
        Stream distanceStream = inputStream
                .partitionAggregate(TaxiFields.BASE_FIELDS, new CalculateDistance(), TaxiFields.BASE_DISTANCE_FIELDS);

        if (distanceHook != null) {
            distanceStream = distanceStream.toStream()
                    .each(TaxiFields.BASE_DISTANCE_FIELDS, distanceHook);
        }

        // TODO: enable
        // distanceStream.each(taxiFieldsWithDistance, new StoreInformation(OperatorType.DISTANCE));

        return topology.build();
    }

    public String getTopic() {
        return topic;
    }

    public LocalKafkaInstance getKafkaInstance() {
        return localKafkaInstance;
    }

    private OpaqueTridentKafkaSpout buildKafkaSpout() {
        ZkHosts zkHosts = new ZkHosts(localKafkaInstance.getConnectString());
        TridentKafkaConfig spoutConfig = new TridentKafkaConfig(zkHosts, topic);
        spoutConfig.scheme = new KeyValueSchemeAsMultiScheme(new TaxiEntryKeyValueScheme());

        return new OpaqueTridentKafkaSpout(spoutConfig);
    }

    public void submitLocalCluster() {
        Config conf = new Config();
        conf.setDebug(false);
        conf.setMaxTaskParallelism(1);

        startKafka();

        cluster = new LocalCluster();
        cluster.submitTopology("stream-processing", conf, build());
    }

    public static TridentProcessingTopology createWithHooks(BaseFilter speedHook, BaseFilter avgSpeedHook, BaseFilter distanceHook) throws Exception {
        return new TridentProcessingTopology(
                "taxi", "localhost", 6379,
                speedHook, avgSpeedHook, distanceHook);
    }

    public static TridentProcessingTopology createWithTopicAndHooks(String topic, BaseFilter speedHook, BaseFilter avgSpeedHook, BaseFilter distanceHook) throws Exception {
        return new TridentProcessingTopology(
                topic, "localhost", 6379,
                speedHook, avgSpeedHook, distanceHook);
    }

    public static void main(String[] args) throws Exception {
        BaseFilter speedHook = new Debug("speed");
        BaseFilter avgSpeedHook = new Debug("avgSpeed");
        BaseFilter distanceHook = new Debug("distance");

        TridentProcessingTopology topology = createWithHooks(speedHook, avgSpeedHook, distanceHook);
        topology.submitLocalCluster();

        try {
            Thread.sleep(60* 1000);
        } catch (InterruptedException e) {

        }

        topology.stop();
    }
}
