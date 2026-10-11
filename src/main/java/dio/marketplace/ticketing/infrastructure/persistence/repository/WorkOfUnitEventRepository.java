package dio.marketplace.ticketing.infrastructure.persistence.repository;

import dio.marketplace.ticketing.domain.*;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class WorkOfUnitEventRepository implements EventRepository {
    private final EventCrudRepository eventCrudRepository;

    public WorkOfUnitEventRepository(EventCrudRepository eventCrudRepository) {
        this.eventCrudRepository = eventCrudRepository;
    }

    @Override
    public void save(Event event) {
        var sectors = event.getSeats().entrySet().stream()
                .map(entry -> {
                    Sector domainSector = entry.getKey();
                    List<Seat> domainSeats = entry.getValue();

                    var seats = domainSeats.stream()
                            .map(s -> new dio.marketplace.ticketing.infrastructure.persistence.entity.Seat(
                                    s.getId(),
                                    s.getCorrelationId().id()
                            ))
                            .toList();

                    return new dio.marketplace.ticketing.infrastructure.persistence.entity.Sector(
                            domainSector.getId(),
                            domainSector.getCorrelationId().id(),
                            domainSector.getPrice(),
                            seats
                    );
                })
                .toList();

        var entity = new dio.marketplace.ticketing.infrastructure.persistence.entity.Event(
                event.getId(),
                event.getCorrelationId().id(),
                sectors);

        eventCrudRepository.save(entity);
    }
}
