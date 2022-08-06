package de.flapdoodle.embed.mongo.spring.autoconfigure;

import de.flapdoodle.checks.Preconditions;
import de.flapdoodle.embed.mongo.transitions.RunningMongodProcess;
import de.flapdoodle.embed.process.io.progress.ProgressListener;
import de.flapdoodle.embed.process.io.progress.ProgressListeners;
import de.flapdoodle.reverse.StateID;
import de.flapdoodle.reverse.TransitionWalker;
import de.flapdoodle.reverse.Transitions;

import java.util.Optional;

public class MongodWrapper {

	private final Transitions transitions;
	private final Optional<ProgressListener> progressListener;
	private TransitionWalker.ReachedState<RunningMongodProcess> runningMongo = null;

	public MongodWrapper(Transitions transitions, ProgressListener progressListener) {
		this.transitions = transitions;
		this.progressListener = Optional.of(progressListener);
	}

	private void start() {
		if (progressListener.isPresent()) {
			try (ProgressListeners.RemoveProgressListener ignored = ProgressListeners.setProgressListener(progressListener.get())) {
				runningMongo = transitions.walker().initState(StateID.of(RunningMongodProcess.class));
			}
		} else {
			runningMongo = transitions.walker().initState(StateID.of(RunningMongodProcess.class));
		}
	}

	private void stop() {
		Preconditions.checkNotNull(runningMongo, "stop called, but runningMongo is null");
		runningMongo.close();
	}
}
