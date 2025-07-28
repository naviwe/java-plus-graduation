package ewm.request;

import ewm.request.dto.ParticipationRequestDto;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.event.Event;
import ewm.event.EventRepository;
import ewm.event.State;
import ewm.exception.ConflictException;
import ewm.utils.CheckRequestService;
import ewm.utils.CheckUserService;
import ewm.utils.CheckEventService;

import java.time.LocalDateTime;
import java.util.List;

import static ewm.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {
    RequestRepository requestRepository;
    EventRepository eventRepository;
    RequestMapper requestMapper;
    CheckUserService checkUserService;
    CheckEventService checkEventService;
    CheckRequestService checkRequestService;

    @Override
    public List<ParticipationRequestDto> findRequestsByUserId(Long userId) {
        checkUserService.checkUser(userId);
        return logAndReturn(
                requestMapper.toDtoList(requestRepository.findByRequesterId(userId)),
                requests -> log.info("Found {} requests for user with id={}",
                        requests.size(), userId)
        );
    }

    @Override
    @Transactional
    public ParticipationRequestDto saveRequest(Long userId, Long eventId) {
        checkUserService.checkUser(userId);
        Event event = checkEventService.checkEvent(eventId);
        checksBeforeSave(userId, event);

        RequestStatus status = determineRequestStatus(event);

        Request request = Request.builder()
                .requesterId(userId)
                .event(event)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequestDto dto = logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                savedRequest -> log.info("{} request created successfully: {}",
                        savedRequest.getStatus(), savedRequest)
        );

        if (status.equals(RequestStatus.CONFIRMED)) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventRepository.save(event);
        }

        return dto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = checkRequestService.checkRequest(requestId);
        checksBeforeCancel(userId, request);
        request.setStatus(RequestStatus.CANCELED);
        return logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                canceledRequest -> log.info("Request canceled successfully: {}", canceledRequest)
        );
    }

    private RequestStatus determineRequestStatus(Event event) {
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }
        if (event.getParticipantLimit() - event.getConfirmedRequests() == 0) {
            throw new ConflictException("Достигнут лимит участников события");
        }
        return RequestStatus.PENDING;
    }

    private void checksBeforeSave(Long requesterId, Event event) {
        if (requestRepository.existsByRequesterIdAndEventId(requesterId, event.getId())) {
            throw new ConflictException(String.format("Request from user with id=%d for event with id=%d already exists",
                    requesterId, event.getId()));
        }
        if (requesterId.equals(event.getInitiatorId())) {
            throw new ConflictException(String.format("User with id=%d cannot request their own event with id=%d",
                    requesterId, event.getId()));
        }
        if (!event.getState().equals(State.PUBLISHED)) {
            throw new ConflictException(String.format("Cannot participate in unpublished event with id=%d",
                    event.getId()));
        }
        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
    }

    private void checksBeforeCancel(Long userId, Request request) {
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d cannot cancel request with id=%d that does not belong to them",
                    userId, request.getId()));
        }
        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new ConflictException(String.format("Request with id=%d is already canceled", request.getId()));
        }
    }
}