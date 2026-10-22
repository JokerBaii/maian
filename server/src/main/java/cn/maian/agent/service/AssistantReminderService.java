package cn.maian.agent.service;

import cn.maian.agent.domain.AssistantReminder;
import cn.maian.agent.repository.AssistantReminderRepository;
import cn.maian.common.exception.ResourceNotFoundException;
import cn.maian.device.repository.EmergencyDeviceRepository;
import cn.maian.science.repository.ScienceSubmissionRepository;
import cn.maian.user.service.CurrentUserService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service
public class AssistantReminderService {
    private final AssistantReminderRepository reminders;
    private final CurrentUserService user;
    private final EmergencyDeviceRepository devices;
    private final ScienceSubmissionRepository submissions;
    private final Clock clock;
    public AssistantReminderService(AssistantReminderRepository reminders,CurrentUserService user,EmergencyDeviceRepository devices,ScienceSubmissionRepository submissions,Clock clock){this.reminders=reminders;this.user=user;this.devices=devices;this.submissions=submissions;this.clock=clock;}
    public record View(UUID id,String title,String kind,UUID targetId,String status,String result,Instant dueAt,Instant createdAt,Instant triggeredAt){}
    private View view(AssistantReminder r){return new View(r.getId(),r.getTitle(),r.getKind(),r.getTargetId(),r.getStatus(),r.getResult(),r.getDueAt(),r.getCreatedAt(),r.getTriggeredAt());}
    @Transactional
    public View create(String title,String kind,UUID target,String due) {
        UUID owner=user.currentUserId(); Instant now=clock.instant();
        if(reminders.countByOwnerIdAndStatusNot(owner,"DISMISSED")>=50)throw new IllegalArgumentException("最多保留50条未结束提醒，请先处理已有提醒");
        Instant dueAt=null;
        if("TIME".equals(kind)){
            try{dueAt=Instant.parse(due);}catch(Exception e){dueAt=due==null?null:AssistantReminderLanguage.time(due,now);}
            if(dueAt==null)throw new IllegalArgumentException("请提供明确提醒时间，例如明天上午8点");
            if(!dueAt.isAfter(now)||dueAt.isAfter(now.plus(Duration.ofDays(366))))throw new IllegalArgumentException("提醒时间须在未来一年内");
        }else if("DEVICE_REVIEW".equals(kind)){
            if(target==null||devices.findOwnedById(target,owner).isEmpty())throw new ResourceNotFoundException("设备不存在");
        }else if("SCIENCE_REVIEW".equals(kind)){
            if(target==null||submissions.findById(target).filter(s->owner.equals(s.getUserId())).isEmpty())throw new ResourceNotFoundException("稿件不存在");
        }else throw new IllegalArgumentException("不支持的提醒类型");
        return view(reminders.saveAndFlush(AssistantReminder.create(owner,title,kind,target,dueAt,now)));
    }
    @Transactional(readOnly=true)
    public List<View> list(){return reminders.findByOwnerIdAndStatusNotOrderByCreatedAtDesc(user.currentUserId(),"DISMISSED",PageRequest.of(0,50)).stream().map(this::view).toList();}
    @Transactional
    public void dismiss(UUID id){var r=reminders.lock(id).filter(v->user.currentUserId().equals(v.getOwnerId())).orElseThrow(()->new ResourceNotFoundException("提醒不存在"));r.dismiss();}
    @Transactional(readOnly=true)
    public List<UUID> pending(){return reminders.pending(clock.instant(),PageRequest.of(0,100));}
    @Transactional
    public void check(UUID id){
        var r=reminders.lock(id).orElse(null);Instant now=clock.instant();
        if(r==null||!"WAITING".equals(r.getStatus())||r.getNextCheckAt().isAfter(now))return;
        String result=null;
        if("TIME".equals(r.getKind()))result="到提醒时间了："+r.getTitle();
        else if("DEVICE_REVIEW".equals(r.getKind())){
            var device=devices.findOwnedById(r.getTargetId(),r.getOwnerId()).orElse(null);
            if(device==null)result="设备记录已删除，跟进已结束。";
            else if(!"PENDING_REVIEW".equals(device.getStatus().name()))result="设备审核状态已更新："+switch(device.getStatus().name()){case "AVAILABLE","RESERVED"->"已通过";case "REJECTED"->"未通过";default->"已结束待审核状态";};
        }else if("SCIENCE_REVIEW".equals(r.getKind())){
            var submission=submissions.findById(r.getTargetId()).filter(s->r.getOwnerId().equals(s.getUserId())).orElse(null);
            if(submission==null)result="稿件记录已删除，跟进已结束。";
            else if(!"PENDING".equals(submission.getStatus().name()))result="稿件审核状态已更新："+("APPROVED".equals(submission.getStatus().name())?"已通过":"未通过");
        }
        r.checked(now,result);
    }
}
