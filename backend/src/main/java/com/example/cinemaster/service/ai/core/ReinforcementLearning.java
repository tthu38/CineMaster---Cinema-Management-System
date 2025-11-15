package com.example.cinemaster.service.ai.core;


import com.example.cinemaster.entity.WorkSchedule;
import com.example.cinemaster.repository.WorkScheduleRepository;
import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;


import java.util.*;


@Service
@RequiredArgsConstructor
public class ReinforcementLearning {


    private final WorkScheduleRepository scheduleRepo;


    public Chromosome adjust(Chromosome c, SchedulingContext ctx) {


        Map<Integer, Integer> exp = new HashMap<>();


        List<WorkSchedule> past = scheduleRepo.findByBranch_IdAndShiftDateBetween(
                ctx.getBranchId(),
                ctx.getWeekStart().minusDays(30),
                ctx.getWeekStart().minusDays(1)
        );


        for (WorkSchedule ws : past) {
            exp.merge(ws.getAccount().getAccountID(), 1, Integer::sum);
        }


        c.getGenes().forEach(g -> {
            int count = exp.getOrDefault(g.getStaffId(), 0);
            double bonus = Math.min(count * 0.1, 3.0);
            g.setFitnessBonus(g.getFitnessBonus() + bonus);
        });


        return c;
    }
}

