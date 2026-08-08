package cn.maian.device.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Arrays;
import java.util.List;

@Embeddable
public class DeviceServiceWindow {
    @Enumerated(EnumType.STRING)
    @Column(length = 12, nullable = false)
    private DayOfWeek dayOfWeek;
    @Column(nullable = false)
    private LocalTime opensAt;
    @Column(nullable = false)
    private LocalTime closesAt;

    protected DeviceServiceWindow() {
    }

    public DeviceServiceWindow(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
        this.dayOfWeek = Objects.requireNonNull(dayOfWeek);
        this.opensAt = Objects.requireNonNull(opensAt);
        this.closesAt = Objects.requireNonNull(closesAt);
    }

    public static List<DeviceServiceWindow> alwaysOpen() {
        return Arrays.stream(DayOfWeek.values())
            .map(day -> new DeviceServiceWindow(day, LocalTime.MIDNIGHT, LocalTime.MIDNIGHT))
            .toList();
    }

    public boolean contains(DayOfWeek day, LocalTime time) {
        if (opensAt.equals(closesAt)) return dayOfWeek == day;
        if (opensAt.isBefore(closesAt)) {
            return dayOfWeek == day && !time.isBefore(opensAt) && !time.isAfter(closesAt);
        }
        return (dayOfWeek == day && !time.isBefore(opensAt))
            || (dayOfWeek.plus(1) == day && !time.isAfter(closesAt));
    }

    public DayOfWeek getDayOfWeek() { return dayOfWeek; }
    public LocalTime getOpensAt() { return opensAt; }
    public LocalTime getClosesAt() { return closesAt; }

    @Override
    public boolean equals(Object other) {
        if (this == other) return true;
        if (!(other instanceof DeviceServiceWindow value)) return false;
        return dayOfWeek == value.dayOfWeek && opensAt.equals(value.opensAt) && closesAt.equals(value.closesAt);
    }

    @Override
    public int hashCode() { return Objects.hash(dayOfWeek, opensAt, closesAt); }
}
