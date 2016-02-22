package de.zalando.aruha.nakadi.validation;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.json.JSONObject;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import de.zalando.aruha.nakadi.domain.EventType;
import de.zalando.aruha.nakadi.domain.ValidationStrategyConfiguration;

public class EventValidation {

    private static final Map<String, EventTypeValidator> eventTypeValidators = Maps.newConcurrentMap();

    public static EventTypeValidator forType(final EventType eventType) {
        return forType(eventType, false);
    }

    public static EventTypeValidator forType(final EventType eventType, final boolean incrementalEdit) {

        final boolean contains = eventTypeValidators.containsKey(eventType.getName());
        Preconditions.checkState(incrementalEdit || !contains, "Validator for EventType {} already defined",
            eventType.getName());

        if (!contains) {
            final EventTypeValidator etsv = new EventTypeValidator(eventType);
            eventTypeValidators.put(eventType.getName(), etsv);
        }

        return eventTypeValidators.get(eventType.getName());
    }

    public static EventTypeValidator lookup(final EventType eventType) {
        return eventTypeValidators.get(eventType.getName());
    }

    public static void reset() {
        eventTypeValidators.clear();
    }
}

class EventTypeValidator {

    private final EventType eventType;
    private final List<EventValidator> validators = Lists.newArrayList();

    public EventTypeValidator(final EventType eventType) {
        this.eventType = eventType;
    }

    public Optional<ValidationError> validate(final JSONObject event) {
        return validators
                .stream()
                .map(validator -> validator.accepts(event))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }

    public EventTypeValidator withConfiguration(final ValidationStrategyConfiguration vsc) {
        final ValidationStrategy vs = ValidationStrategy.lookup(vsc.getStrategyName());
        validators.add(vs.materialize(eventType, vsc));

        return this;
    }

}
