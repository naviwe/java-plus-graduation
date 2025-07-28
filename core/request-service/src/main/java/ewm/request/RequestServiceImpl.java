package ewm.request;

import ewm.interaction.client.EventFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.event.UpdateEventRequest;
import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.interaction.dto.request.RequestStatus;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.utils.CheckEventService;
import ewm.interaction.utils.CheckUserService;
import ewm.interaction.utils.LoggingUtils;
import ewm.utils.CheckRequestService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {
    RequestRepository requestRepository;
    RequestMapper requestMapper;
    CheckUserService userService;
    CheckEventService eventService;
    EventFeignClient eventFeignClient;
    CheckRequestService checkRequestService;

    @Override
    public List<ParticipationRequestDto> findRequestsByUserId(Long userId) {
        userService.checkUser(userId);
        return LoggingUtils.logAndReturn(
                requestMapper.toDtoList(requestRepository.findByRequesterId(userId)),
                requests -> log.info("Found {} requests for user with id={}", requests.size(), userId)
        );
    }

    @Override
    @Transactional
    public ParticipationRequestDto saveRequest(Long userId, Long eventId) {
        userService.checkUser(userId);
        EventFullDto event = eventService.checkEvent(eventId);
        checksBeforeSave(userId, event);

        RequestStatus status = determineRequestStatus(event);

        Request request = Request.builder()
                .requesterId(userId)
                .eventId(eventId)
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequestDto dto = LoggingUtils.logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                savedRequest -> log.info("{} request created successfully: {}",
                        savedRequest.getStatus(), savedRequest)
        );

        if (status.equals(RequestStatus.CONFIRMED)) {
            UpdateEventRequest updateRequest = UpdateEventRequest.builder()
                    .participantLimit(event.getParticipantLimit())
                    .build();
            eventFeignClient.updateEvent(eventId, updateRequest);
        }

        return dto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = checkRequestService.checkRequest(requestId);
        checksBeforeCancel(userId, request);
        request.setStatus(RequestStatus.CANCELED);
        return LoggingUtils.logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                canceledRequest -> log.info("Request canceled successfully: {}", canceledRequest)
        );
    }

    private RequestStatus determineRequestStatus(EventFullDto event) {
        if (!event.getRequestModeration() || event.getParticipantLimit() == 0) {
            return RequestStatus.CONFIRMED;
        }
        if (event.getParticipantLimit() - event.getConfirmedRequests() == 0) {
            throw new ConflictException("Достигнут лимит участников события");
        }
        return RequestStatus.PENDING;
    }

    private void checksBeforeSave(Long requesterId, EventFullDto event) {
        if (requestRepository.existsByRequesterIdAndEventId(requesterId, event.getId())) {
            throw new ConflictException(String.format("Request from user with id=%d for event with id=%d already exists",
                    requesterId, event.getId()));
        }
        if (requesterId.equals(event.getInitiator().getId())) {
            throw new ConflictException(String.format("User with id=%d cannot request their own event with id=%d",
                    requesterId, event.getId()));
        }
        if (!event.getState().equals(State.PUBLISHED.toString())) {
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