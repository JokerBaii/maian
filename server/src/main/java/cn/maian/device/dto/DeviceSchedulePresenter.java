package cn.maian.device.dto;

import cn.maian.device.domain.DeviceServiceWindow;

import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.util.List;

final class DeviceSchedulePresenter {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    private DeviceSchedulePresenter() {
    }

    static List<DeviceServiceWindowResponse> windows(List<DeviceServiceWindow> windows) {
        return windows.stream().map(DeviceServiceWindowResponse::from).toList();
    }

    static String display(List<DeviceServiceWindow> windows) {
        if (windows.size() == 7
            && windows.stream().allMatch(window -> window.getOpensAt().equals(window.getClosesAt()))) {
            return "全天";
        }
        if (windows.isEmpty()) {
            return "未设置";
        }
        boolean weekdays = windows.size() == 5
            && windows.stream().map(DeviceServiceWindow::getDayOfWeek).distinct().count() == 5
            && windows.stream().noneMatch(window -> window.getDayOfWeek() == DayOfWeek.SATURDAY
                || window.getDayOfWeek() == DayOfWeek.SUNDAY);
        var first = windows.getFirst();
        boolean sameTime = windows.stream().allMatch(window ->
            window.getOpensAt().equals(first.getOpensAt())
                && window.getClosesAt().equals(first.getClosesAt()));
        if (sameTime) {
            String range = first.getOpensAt().format(TIME) + "-" + first.getClosesAt().format(TIME);
            return weekdays ? "工作日 " + range : range;
        }
        return "分时段服务";
    }
}
