package ewm.request;

import ewm.interaction.feign.EventFeignClient;
import ewm.interaction.feign.UserFeignClient;
import ewm.interaction.dto.event.EventFullDto;
import ewm.interaction.dto.event.State;
import ewm.interaction.dto.request.EventRequestStatusUpdateRequest;
import ewm.interaction.dto.request.EventRequestStatusUpdateResult;
import ewm.interaction.dto.request.ParticipationRequestDto;
import ewm.interaction.dto.request.RequestStatus;
import ewm.interaction.exception.ConflictException;
import ewm.interaction.exception.NotFoundException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.client.CollectorClient;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static ewm.interaction.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class RequestServiceImpl implements RequestService {

    RequestRepository requestRepository;
    RequestMapper requestMapper;
    UserFeignClient userClient;
    EventFeignClient eventClient;
    CollectorClient collectorClient;

    @Override
    public List<ParticipationRequestDto> findRequestsByUserId(Long userId) {
        userClient.getUser(userId);
        return logAndReturn(
                requestMapper.toDtoList(requestRepository.findByRequesterId(userId)),
                requests -> log.info("Found {} requests for user with id={}", requests.size(), userId)
        );
    }

    @Override
    @Transactional
    public ParticipationRequestDto saveRequest(Long userId, Long eventId) {
        Long requesterId = userClient.getUser(userId).getId();
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        checksBeforeSave(requesterId, event);

        RequestStatus status = event.getRequestModeration() ? RequestStatus.PENDING : RequestStatus.CONFIRMED;
        if (event.getParticipantLimit() == 0) {
            status = RequestStatus.CONFIRMED;
        } else if (event.getParticipantLimit() - event.getConfirmedRequests() == 0) {
            throw new ConflictException("Достигнут лимит участников события");
        }

        Request request = Request.builder()
                .requesterId(requesterId)
                .eventId(event.getId())
                .created(LocalDateTime.now())
                .status(status)
                .build();

        ParticipationRequestDto dto = logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                savedRequest -> log.info("{} request created successfully: {}", savedRequest.getStatus(), savedRequest)
        );

        if (status == RequestStatus.CONFIRMED || status == RequestStatus.PENDING) {
            try {
                collectorClient.registrationInEvent(requesterId, eventId);
                log.info("User action REGISTER sent to Collector for userId={}, eventId={}", requesterId, eventId);
            } catch (Exception e) {
                log.error("Ошибка при отправке действия REGISTER в Collector: {}", e.getMessage(), e);
            }
        }

        if (status.equals(RequestStatus.CONFIRMED)) {
            event.setConfirmedRequests(event.getConfirmedRequests() + 1);
            eventClient.changeEventFields(event);
        }

        return dto;
    }

    @Override
    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        Request request = requestRepository.findById(requestId)
                .orElseThrow(() -> new NotFoundException(String.format("Request with id=%d was not found", requestId)));
        checksBeforeCancel(userClient.getUser(userId).getId(), request);

        request.setStatus(RequestStatus.CANCELED);
        return logAndReturn(
                requestMapper.toDto(requestRepository.save(request)),
                canceledRequest -> log.info("Request canceled successfully: {}", canceledRequest)
        );
    }

    @Override
    public List<ParticipationRequestDto> findRequestsByEventId(Long userId, Long eventId) {
        userClient.getUser(userId);
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d isn't an initiator for event with id=%d", userId, eventId));
        }

        List<Request> requests = requestRepository.findByEventId(eventId);
        return requests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public EventRequestStatusUpdateResult updateRequestStatus(EventRequestStatusUpdateRequest requestDto, Long userId, Long eventId) {
        userClient.getUser(userId);
        EventFullDto event = eventClient.getEventByIdInternal(eventId);
        if (!event.getInitiator().getId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d isn't an initiator for event with id=%d", userId, eventId));
        }
        if (event.getParticipantLimit() != 0 && event.getParticipantLimit().equals(event.getConfirmedRequests().intValue())) {
            throw new ConflictException("There is no more space");
        }

        List<Request> requests = requestRepository.findAllByIdInAndStatus(requestDto.getRequestIds(), RequestStatus.PENDING);
        if (requests.size() != requestDto.getRequestIds().size()) {
            throw new ConflictException("Some requests are not in PENDING status or do not exist");
        }

        EventRequestStatusUpdateResult result = new EventRequestStatusUpdateResult();
        List<ParticipationRequestDto> confirmedRequests = new ArrayList<>();
        List<ParticipationRequestDto> rejectedRequests = new ArrayList<>();

        if (requestDto.getStatus().equals(RequestStatus.CONFIRMED)) {
            confirmedRequests = processRequests(requests, requestDto.getStatus());
            result.setConfirmedRequests(confirmedRequests);
            event.setConfirmedRequests(event.getConfirmedRequests() + confirmedRequests.size());

            try {
                for (Request request : requests) {
                    collectorClient.registrationInEvent(request.getRequesterId(), eventId);
                    log.info("User action REGISTER sent to Collector for userId={}, eventId={}", request.getRequesterId(), eventId);
                }
            } catch (Exception e) {
                log.error("Ошибка при отправке действия REGISTER в Collector: {}", e.getMessage(), e);
            }
        } else if (requestDto.getStatus().equals(RequestStatus.REJECTED)) {
            rejectedRequests = processRequests(requests, requestDto.getStatus());
            result.setRejectedRequests(rejectedRequests);
        }

        eventClient.changeEventFields(event);
        return result;
    }

    private List<ParticipationRequestDto> processRequests(List<Request> requests, RequestStatus status) {
        requests.forEach(request -> request.setStatus(status));
        List<Request> updatedRequests = requestRepository.saveAll(requests);
        return updatedRequests.stream()
                .map(requestMapper::toDto)
                .collect(Collectors.toList());
    }

    private void checksBeforeSave(Long requesterId, EventFullDto event) {
        if (requestRepository.existsByRequesterIdAndEventId(requesterId, event.getId())) {
            throw new ConflictException(String.format("Request from user with id=%d for event with id=%d already exists", requesterId, event.getId()));
        }
        if (requesterId.equals(event.getInitiator().getId())) {
            throw new ConflictException(String.format("User with id=%d cannot request their own event with id=%d", requesterId, event.getId()));
        }
        if (!event.getState().equals(State.PUBLISHED.toString())) {
            throw new ConflictException(String.format("Cannot participate in unpublished event with id=%d", event.getId()));
        }
        if (event.getParticipantLimit() > 0 && event.getConfirmedRequests() >= event.getParticipantLimit()) {
            throw new ConflictException("The participant limit has been reached");
        }
    }

    private void checksBeforeCancel(Long userId, Request request) {
        if (!request.getRequesterId().equals(userId)) {
            throw new ConflictException(String.format("User with id=%d cannot cancel request with id=%d that does not belong to them", userId, request.getId()));
        }
        if (request.getStatus() == RequestStatus.CANCELED) {
            throw new ConflictException(String.format("Request with id=%d is already canceled", request.getId()));
        }
    }
}