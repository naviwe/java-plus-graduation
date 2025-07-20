package ewm.server;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ewm.EndpointHitDto;
import ewm.StatsDto;
import ewm.exception.ValidationException;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static ewm.utils.LoggingUtils.logAndReturn;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatsServiceImpl implements StatsService {
    private final StatsRepository statsRepository;

    @Override
    @Transactional
    public EndpointHitDto saveHit(EndpointHitDto endpointHitDto) {
        return logAndReturn(StatsMapper.mapToEndpointHitDto(
                        statsRepository.save(StatsMapper.mapToEndpointHit(endpointHitDto))),
                savedEndpoint -> log.info("запрос c id = {} - cохранен", savedEndpoint.getId()));
    }

    @Override
    public List<StatsDto> getStats(String start, String end, List<String> uris, boolean unique) {
        LocalDateTime startTime = LocalDateTime.parse(start, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        LocalDateTime endTime = LocalDateTime.parse(end, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        if (startTime.isAfter(endTime)) {
            throw new ValidationException("Событие не может заканчиваться позже, чем начинается");
        }
        List<StatsDto> stats;
        if (unique) {
            stats = statsRepository.findStatsWithUniqueIp(startTime, endTime, uris);
            stats.forEach(stat -> log.info("Уникальные IP для app={}, uri={}: {}",
                    stat.getApp(), stat.getUri(), stat.getHits()));
        } else {
            stats = statsRepository.findStats(startTime, endTime, uris);
            stats.forEach(stat -> log.info("Все просмотры для app={}, uri={}: {}",
                    stat.getApp(), stat.getUri(), stat.getHits()));
        }
        return logAndReturn(stats, result -> log.info("Статистика собрана в количестве - {}",
                result.size()));
    }
}
