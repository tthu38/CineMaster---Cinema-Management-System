package com.example.cinemaster.service.ai.core;


import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.*;


import java.time.LocalDate;
import java.util.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chromosome {


    private List<Gene> genes;
    private double fitnessScore;


    /** Deep Copy */
    public Chromosome deepCopy() {
        List<Gene> list = new ArrayList<>();
        for (Gene g : genes) {
            list.add(Gene.builder()
                    .staffId(g.getStaffId())
                    .shiftType(g.getShiftType())
                    .date(g.getDate())
                    .fitnessBonus(g.getFitnessBonus())
                    .build());
        }
        return Chromosome.builder()
                .genes(list)
                .fitnessScore(fitnessScore)
                .build();
    }


    /** ⭐ FITNESS CHUẨN — Staff chỉ được xếp đúng ca đã REQUEST */
    public double evaluateFitness(SchedulingContext ctx) {


        double score = 0;


        // 1. Đúng request thưởng / sai request phạt mạnh
        for (Gene g : genes) {
            if (ctx.hasRequestedShift(g.getStaffId(), g.getDate(), g.getShiftType()))
                score += 10;
            else
                score -= 20;
        }


        // 2. Điểm reinforcement learning
        score += genes.stream().mapToDouble(Gene::getFitnessBonus).sum();


        // 3. Công bằng số ca
        Map<Integer, Integer> count = new HashMap<>();
        for (Gene g : genes) {
            count.merge(g.getStaffId(), 1, Integer::sum);
        }
        int avg = genes.size() / Math.max(ctx.getStaff().size(), 1);
        for (var e : count.entrySet()) {
            score -= Math.abs(e.getValue() - avg) * 0.8;
        }


        // 4. Đủ nhân sự theo từng ca
        // 4. Đủ nhân sự theo từng ca
        for (LocalDate d : ctx.getWeekDates()) {


            String date = d.toString();
            String dow = d.getDayOfWeek().toString(); // MONDAY, TUESDAY...


            for (String s : ctx.getShiftTypes()) {


                // ⭐ LẤY REQUIRED THEO DAY-OF-WEEK, KHÔNG DÙNG NGÀY THỰC
                int required = ctx.getRequiredPerShift()
                        .getOrDefault(dow + "_" + s, 1);


                long cnt = genes.stream()
                        .filter(g -> g.getDate().equals(date) && g.getShiftType().equals(s))
                        .count();


                if (cnt < required) score -= (required - cnt) * 6;
                if (cnt > required) score -= (cnt - required) * 3;
            }
        }




        this.fitnessScore = score;
        return score;
    }


    public double getFitness(SchedulingContext ctx) {
        return evaluateFitness(ctx);
    }


    /** Crossover CHUẨN */
    public Chromosome crossover(Chromosome other) {


        Random r = new Random();
        int point = r.nextInt(genes.size());


        List<Gene> newGenes = new ArrayList<>();


        for (int i = 0; i < genes.size(); i++) {
            Gene src = (i < point) ? genes.get(i) : other.getGenes().get(i);


            newGenes.add(Gene.builder()
                    .date(src.getDate())
                    .shiftType(src.getShiftType())
                    .staffId(src.getStaffId())
                    .fitnessBonus(src.getFitnessBonus())
                    .build());
        }


        return Chromosome.builder()
                .genes(newGenes)
                .fitnessScore(0)
                .build();
    }


    /** Mutation — chỉ đổi sang staff có request hợp lệ */
    public void mutate(List<Integer> staffIds, SchedulingContext ctx) {


        Random r = new Random();


        if (genes == null || genes.isEmpty())
            return;


        int idx = r.nextInt(genes.size());
        Gene g = genes.get(idx);


        List<Integer> valid = staffIds.stream()
                .filter(id -> ctx.hasRequestedShift(id, g.getDate(), g.getShiftType()))
                .toList();


        if (!valid.isEmpty()) {
            g.setStaffId(valid.get(r.nextInt(valid.size())));
        }
    }
}

