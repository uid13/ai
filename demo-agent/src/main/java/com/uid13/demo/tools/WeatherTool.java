package com.uid13.demo.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * 天气查询工具 - 供 WeatherAgent 使用
 */
@Slf4j
@Component
public class WeatherTool {

    private static final Map<String, String> WEATHER_DATA = Map.of(
            "beijing", "晴，25°C，空气质量优",
            "shanghai", "多云，28°C，湿度 65%",
            "hangzhou", "小雨，22°C，记得带伞",
            "shenzhen", "晴转多云，30°C，适宜出行",
            "guangzhou", "雷阵雨，29°C，注意防雨"
    );

    @Tool(description = "根据城市名称查询当前天气状况，返回温度、天气情况和出行建议")
    public String getWeather(
            @ToolParam(description = "城市名称，支持中文或拼音，例如：北京、beijing、杭州") String city) {

        log.debug("[WeatherTool] 查询城市天气：{}", city);

        String weather = WEATHER_DATA.getOrDefault(city.toLowerCase(), "未知城市，请检查城市名称");

        if (weather.equals("未知城市，请检查城市名称")) {
            weather = switch (city) {
                case "北京" -> WEATHER_DATA.get("beijing");
                case "上海" -> WEATHER_DATA.get("shanghai");
                case "杭州" -> WEATHER_DATA.get("hangzhou");
                case "深圳" -> WEATHER_DATA.get("shenzhen");
                case "广州" -> WEATHER_DATA.get("guangzhou");
                default -> "未知城市：" + city + "，暂不支持查询";
            };
        }

        return city + "的天气：" + weather;
    }

    @Tool(description = "获取当前的日期和时间，用于回答用户关于时间的问题")
    public String getCurrentDateTime() {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy 年 MM 月 dd 日 HH:mm:ss");
        return "当前时间：" + now.format(formatter);
    }
}
