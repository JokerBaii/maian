package cn.maian.agent.service;

import cn.maian.agent.dto.AssistantContext;
import cn.maian.agent.dto.AssistantResponse.Task;
import java.time.*;
import java.util.*;
import java.util.regex.Pattern;

final class AssistantReminderLanguage {
    private AssistantReminderLanguage(){}
    static Task parse(String text,AssistantContext context){
        if(!text.contains("提醒"))return null;
        if(text.matches(".*(取消|删除|结束|处理掉).*提醒.*"))return AssistantOperations.task("REMINDER_DISMISS",Map.of());
        if(text.matches(".*(查看|看看|查询|我的).*提醒.*")&&!text.contains("提醒我"))return AssistantOperations.task("REMINDER_LIST",Map.of());
        if(!text.contains("提醒我")&&!text.contains("给我提醒"))return null;
        String title=text.replace("提醒我","").replace("给我提醒","").trim();
        var p=new LinkedHashMap<String,String>();p.put("title",title.substring(0,Math.min(120,title.length())));
        if(text.contains("审核")&&(text.contains("设备")||context!=null&&context.deviceId()!=null))p.put("kind","DEVICE_REVIEW");
        else if(text.contains("审核")&&(text.contains("稿")||context!=null&&context.submissionId()!=null))p.put("kind","SCIENCE_REVIEW");
        else{
            p.put("kind","TIME");Instant time=time(text,Instant.now());if(time!=null)p.put("dueAt",time.toString());
        }
        return AssistantOperations.task("REMINDER_CREATE",p);
    }
    static Instant time(String text,Instant now){
        text=text.replace("明早","明天上午").replace("明晚","明天晚上").replace("今晚","今天晚上");
        var relative=Pattern.compile("([一二两三四五六七八九十\\d]+)\\s*(分钟|小时|天)后").matcher(text);
        if(relative.find()){
            int n=number(relative.group(1));if(n<=0)return null;
            long seconds=n*switch(relative.group(2)){case "小时"->3600L;case "天"->86400L;default->60L;};
            return seconds<=366L*86400?now.plusSeconds(seconds):null;
        }
        var zone=ZoneId.of("Asia/Shanghai");var local=now.atZone(zone);
        var clock=Pattern.compile("(今天|明天|后天)?\\s*(上午|早上|清晨|下午|晚上|傍晚|中午)?([一二两三四五六七八九十\\d]{1,2})(?:点|:|：)(半|一刻|三刻|[一二两三四五六七八九十\\d]{1,3})?").matcher(text);
        if(!clock.find())return null;
        int hour=number(clock.group(3));
        String minuteText=clock.group(4);
        int minute=minuteText==null?0:switch(minuteText){case "半"->30;case "一刻"->15;case "三刻"->45;default->number(minuteText);};
        if(clock.group(2)!=null&&Set.of("下午","晚上","傍晚","中午").contains(clock.group(2))&&hour<12)hour+=12;
        if(hour<0||hour>23||minute<0||minute>59)return null;
        int days=text.contains("后天")?2:text.contains("明天")?1:0;
        var date=local.toLocalDate().plusDays(days);
        var absolute=Pattern.compile("(20\\d{2})[-/年](\\d{1,2})[-/月](\\d{1,2})日?").matcher(text);
        if(absolute.find()){
            try{date=LocalDate.of(Integer.parseInt(absolute.group(1)),Integer.parseInt(absolute.group(2)),Integer.parseInt(absolute.group(3)));}
            catch(DateTimeException invalid){return null;}
        }
        var result=date.atTime(hour,minute).atZone(zone).toInstant();
        return result.isAfter(now)?result:null;
    }
    private static int number(String value){
        if(value.matches("\\d+"))try{return Integer.parseInt(value);}catch(NumberFormatException e){return -1;}
        var digits=Map.of("一",1,"二",2,"两",2,"三",3,"四",4,"五",5,"六",6,"七",7,"八",8,"九",9);
        if(value.contains("十")){var parts=value.split("十",-1);return (parts[0].isEmpty()?1:digits.getOrDefault(parts[0],0))*10+(parts.length>1?digits.getOrDefault(parts[1],0):0);}
        return digits.getOrDefault(value,0);
    }
}
