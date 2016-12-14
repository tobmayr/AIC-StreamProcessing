package at.ac.tuwien.aic.streamprocessing.storm.trident.speed;

import at.ac.tuwien.aic.streamprocessing.storm.trident.state.RedisState;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.StateQuery;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.SpeedState;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.SpeedStateMapper;
import at.ac.tuwien.aic.streamprocessing.storm.trident.state.objects.StateObjectMapper;

public class SpeedQuery extends StateQuery<RedisState<SpeedState>, SpeedState> {

    @Override
    protected StateObjectMapper<SpeedState> createMapper() {
        return new SpeedStateMapper();
    }
}
