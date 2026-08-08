package cn.maian.device.dto;

import cn.maian.device.domain.DeviceServiceWindow;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record DeviceServiceWindowResponse(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
    public static DeviceServiceWindowResponse from(DeviceServiceWindow window) {
        return new DeviceServiceWindowResponse(window.getDayOfWeek(), window.getOpensAt(), window.getClosesAt());
    }
}
