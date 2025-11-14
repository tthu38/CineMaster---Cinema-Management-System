
package com.example.cinemaster.service.ai.core;

import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.*;

        import java.util.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Chromosome {

    private List<Gene> genes;      // danh sách gene (mỗi gene = 1 ca làm)
    private double fitnessScore;   // điểm fitness cuối cùng

    /**
     * Deep copy chromosome
     */
    public Chromosome deepCopy() {
        List<Gene> copiedGenes = new ArrayList<>();
        for (Gene g : genes) {
            copiedGenes.add(
                    Gene.builder()
                            .staffId(g.getStaffId())
                            .shiftType(g.getShiftType())
                            .date(g.getDate())
                            .fitnessBonus(g.getFitnessBonus())
                            .build()
            );
        }
        return Chromosome.builder()
                .genes(copiedGenes)
                .fitnessScore(this.fitnessScore)
                .build();
    }

    /**
     * Đánh giá fitness
     */
    public double evaluateFitness(SchedulingContext ctx) {

        double score = 0;

        // 1️⃣ Điểm thưởng từ Reinforcement Learning
        score += genes.stream().mapToDouble(Gene::getFitnessBonus).sum();

        // 2️⃣ Ưu tiên theo yêu cầu ca của staff
        var reqMap = new HashMap<String, Set<String>>();
        ctx.getShiftRequests().forEach(r -> {
            String key = r.getAccount().getAccountID() + "_" + r.getShiftDate();
            reqMap.computeIfAbsent(key, k -> new HashSet<>()).add(r.getShiftType());
        });

        for (Gene g : genes) {
            String key = g.getStaffId() + "_" + g.getDate();
            if (reqMap.containsKey(key) && reqMap.get(key).contains(g.getShiftType())) {
                score += 5; // ưu tiên ca nhân viên mong muốn
            }
        }

        // 3️⃣ Tránh xếp 1 người làm quá nhiều ca
        var countByStaff = new HashMap<Integer, Integer>();
        for (Gene g : genes) {
            countByStaff.merge(g.getStaffId(), 1, Integer::sum);
        }

        int avg = genes.size() / ctx.getStaff().size();
        for (var e : countByStaff.entrySet()) {
            int diff = Math.abs(e.getValue() - avg);
            score -= diff * 0.5;
        }

        // 4️⃣ Đủ nhân sự theo requiredPerShift
        for (String date : ctx.getWeekDates().stream().map(Object::toString).toList()) {
            for (String shift : ctx.getShiftTypes()) {
                int req = ctx.getRequiredPerShift()
                        .getOrDefault(date + "_" + shift, 1);

                long cnt = genes.stream()
                        .filter(g -> g.getDate().equals(date) && g.getShiftType().equals(shift))
                        .count();

                if (cnt < req) score -= (req - cnt) * 2;
            }
        }

        this.fitnessScore = score;
        return score;
    }

    public double getFitnessScore() {
        return fitnessScore;
    }

    public double getFitness(SchedulingContext ctx) {
        return evaluateFitness(ctx);
    }

    /**
     * Crossover: chọn ngẫu nhiên một điểm
     */
    public Chromosome crossover(Chromosome other) {
        int point = new Random().nextInt(genes.size());

        List<Gene> newGenes = new ArrayList<>();

        for (int i = 0; i < genes.size(); i++) {
            newGenes.add(i < point ? genes.get(i) : other.genes.get(i));
        }

        return Chromosome.builder().genes(newGenes).fitnessScore(0).build();
    }

    /**
     * Mutation
     */
    public void mutate(List<Integer> staffIds) {
        Random r = new Random();
        int index = r.nextInt(genes.size());
        genes.get(index).setStaffId(staffIds.get(r.nextInt(staffIds.size())));
    }
}

