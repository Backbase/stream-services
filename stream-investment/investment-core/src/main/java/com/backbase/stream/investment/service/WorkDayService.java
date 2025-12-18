package com.backbase.stream.investment.service;

import static java.time.DayOfWeek.SATURDAY;
import static java.time.DayOfWeek.SUNDAY;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class WorkDayService {

    static List<LocalDate> workDays(LocalDate startDay, LocalDate endDay) {
        return Stream.iterate(startDay,
                offsetDate -> offsetDate.isBefore(endDay),
                offsetDateTime -> offsetDateTime.plusDays(1))
            .filter(Predicate.not(isWeekend()))
//            .sorted(Comparator.reverseOrder())
            .toList();
    }

    static LocalDate nextWorkDay(OffsetDateTime date, LocalDate defaultDay, int skipDays) {
        return nextWorkDay(Optional.ofNullable(date)
            .map(OffsetDateTime::toLocalDate), defaultDay, skipDays);
    }

    static LocalDate nextWorkDay(Optional<LocalDate> date, LocalDate defaultDay, int skipDays) {
        return date.map(d -> findNextWorkday(d, skipDays))
            .filter(d -> d.isBefore(defaultDay))
            .orElse(defaultDay);
    }

    private static LocalDate findNextWorkday(LocalDate date, int skipDays) {
        LocalDate nextDay = date.plusDays(skipDays);
        while (isWeekend().test(nextDay)) {
            nextDay = nextDay.plusDays(1);
        }
        return nextDay;
    }

    private static Predicate<LocalDate> isWeekend() {
        return date -> date.getDayOfWeek() == SATURDAY || date.getDayOfWeek() == SUNDAY;
    }

}
