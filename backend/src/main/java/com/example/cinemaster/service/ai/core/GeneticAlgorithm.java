package com.example.cinemaster.service.ai.core;

import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.*;

        import java.util.*;
        import java.util.stream.Collectors;

/**
 * Genetic Algorithm tạo ra lịch làm tối ưu dựa vào:
 * - Ca yêu thích
 * - Đủ nhân sự
 * - Công bằng số ca
 * - Ngày nghỉ
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneticAlgorithm {

    private int populationSize = 80;   // số lịch trong một thế hệ
    private int generations = 150;     // số vòng lặp
    private double mutationRate = 0.12; // xác suất đột biến
    private double crossoverRate = 0.85; // xác suất lai ghép
    private Random random = new Random();

    /**
     * Chạy GA và trả về lịch tốt nhất
     */
    public Chromosome run(SchedulingContext ctx) {

        // 1️⃣ Tạo quần thể ban đầu
        List<Chromosome> population = generateInitialPopulation(ctx);

        // 2️⃣ Cho từng con tính fitness
        population.forEach(c -> c.evaluateFitness(ctx));

        Chromosome best = population.stream()
                .max(Comparator.comparingDouble(Chromosome::getFitnessScore))
                .orElseThrow();

        // 3️⃣ Lặp lại GA
        for (int gen = 0; gen < generations; gen++) {

            List<Chromosome> newPopulation = new ArrayList<>();

            while (newPopulation.size() < populationSize) {

                // 🔍 Chọn bố mẹ
                Chromosome parent1 = selectParent(population);
                Chromosome parent2 = selectParent(population);

                Chromosome child;

                // 🧬 Lai ghép
                if (random.nextDouble() < crossoverRate) {
                    child = parent1.crossover(parent2);
                } else {
                    child = parent1.deepCopy();
                }

                // 🔀 Đột biến
                if (random.nextDouble() < mutationRate) {
                    List<Integer> staffIds = ctx.getStaff()
                            .stream().map(s -> s.getAccountID())
                            .collect(Collectors.toList());

                    child.mutate(staffIds);
                }

                // 🎯 Tính fitness
                child.evaluateFitness(ctx);
                newPopulation.add(child);
            }

            // Thay thế quần thể
            population = newPopulation;

            // Cập nhật best
            Chromosome genBest = population.stream()
                    .max(Comparator.comparingDouble(Chromosome::getFitnessScore))
                    .orElseThrow();

            if (genBest.getFitnessScore() > best.getFitnessScore()) {
                best = genBest.deepCopy();
            }

            // Debug:
            System.out.println("GEN " + gen + " best = " + best.getFitnessScore());
        }

        return best;
    }

    // ============================
    // 1️⃣ Tạo quần thể ban đầu
    // ============================
    private List<Chromosome> generateInitialPopulation(SchedulingContext ctx) {

        List<Integer> staffIds = ctx.getStaff()
                .stream().map(s -> s.getAccountID()).toList();

        List<Chromosome> population = new ArrayList<>();

        List<String> shiftTypes = ctx.getShiftTypes();      // MORNING / AFTERNOON / NIGHT
        List<String> dates = ctx.getWeekDates()
                .stream().map(d -> d.toString())
                .toList();

        for (int i = 0; i < populationSize; i++) {

            List<Gene> genes = new ArrayList<>();

            for (String date : dates) {
                for (String shift : shiftTypes) {

                    Gene g = Gene.builder()
                            .date(date)
                            .shiftType(shift)
                            .staffId(staffIds.get(random.nextInt(staffIds.size())))
                            .fitnessBonus(0)
                            .build();

                    genes.add(g);
                }
            }

            Chromosome c = Chromosome.builder()
                    .genes(genes)
                    .fitnessScore(0)
                    .build();

            population.add(c);
        }

        return population;
    }


    // ============================
    // 2️⃣ Chọn lựa bố mẹ (Tournament selection)
    // ============================
    private Chromosome selectParent(List<Chromosome> population) {
        int tournamentSize = 6;

        Chromosome best = null;

        for (int i = 0; i < tournamentSize; i++) {
            Chromosome c = population.get(random.nextInt(population.size()));
            if (best == null || c.getFitnessScore() > best.getFitnessScore()) {
                best = c;
            }
        }

        return best;
    }
}

