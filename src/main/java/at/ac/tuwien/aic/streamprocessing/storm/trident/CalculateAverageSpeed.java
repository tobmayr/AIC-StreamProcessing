package at.ac.tuwien.aic.streamprocessing.storm.trident;

import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.AverageSpeedState;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.AverageSpeedStateMapper;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.StateObjectMapper;
import org.apache.storm.trident.operation.TridentOperationContext;
import org.apache.storm.trident.tuple.TridentTuple;
import org.apache.storm.tuple.Values;

import java.util.Map;

public class CalculateAverageSpeed extends Aggregator<AverageSpeedState> {

    public CalculateAverageSpeed() {
        super(false);
    }

    private StateObjectMapper<AverageSpeedState> mapper;

    @Override
    public void prepare(Map conf, TridentOperationContext context) {
        super.prepare(conf, context);

        this.mapper = new AverageSpeedStateMapper();
    }

    @Override
    protected AverageSpeedState compute(AverageSpeedState previous, TridentTuple tuple) {
        Double speedSum = previous.getSpeedSum();
        Integer observations = previous.getObservations();
        Double speed = tuple.getDoubleByField("speed");

        Double newSpeedSum = speedSum + speed;
        Integer newObservations = observations + 1;

        return new AverageSpeedState(newObservations, newSpeedSum);
    }

    @Override
    protected StateObjectMapper<AverageSpeedState> getMapper() {
        return mapper;
    }
}
