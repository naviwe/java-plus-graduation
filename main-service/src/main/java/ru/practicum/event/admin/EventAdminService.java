package ru.practicum.event.admin;


import ru.practicum.event.dto.EventFullDto;
import ru.practicum.event.dto.UpdateEventRequest;

import java.util.List;

public interface EventAdminService {

    List<EventFullDto> getEvents(List<Long> users, List<String> states,
                                 List<Long> categories, String rangeStart,
                                 String rangeEnd, Integer from,
                                 Integer size);


    EventFullDto updateEvent(UpdateEventRequest updateEventRequest, Long eventId);
}
