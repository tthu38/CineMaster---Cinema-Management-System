package com.example.cinemaster.service.ai.core;


import com.example.cinemaster.service.ai.dto.SchedulingContext;
import lombok.*;


import java.time.LocalDate;
import java.util.*;


@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class GeneticAlgorithm {


    private int populationSize = 80;
    private int generations = 150;
    private double mutationRate = 0.15;
    private double crossoverRate = 0.85;


    private Random random = new Random();


    /** ============================
     *  MAIN RUN
     * ============================ */
    public Chromosome run(SchedulingContext ctx) {


        List<Chromosome> population = generateInitialPopulation(ctx);


        // tính fitness lần đầu
        population.forEach(c -> c.evaluateFitness(ctx));


        Chromosome best = getBest(population);


        for (int gen = 0; gen < generations; gen++) {


            List<Chromosome> newPop = new ArrayList<>();


            while (newPop.size() < populationSize) {


                Chromosome p1 = select(population);
                Chromosome p2 = select(population);


                Chromosome child;


                if (random.nextDouble() < crossoverRate) {
                    child = p1.crossover(p2);
                } else {
                    child = p1.deepCopy();
                }


                if (random.nextDouble() < mutationRate) {
                    List<Integer> ids = ctx.getStaff().stream()
                            .map(s -> s.getAccountID()).toList();
                    child.mutate(ids, ctx);
                }


                child.evaluateFitness(ctx);
                newPop.add(child);
            }


            population = newPop;


            Chromosome genBest = getBest(population);
            if (genBest.getFitnessScore() > best.getFitnessScore()) {
                best = genBest.deepCopy();
            }
        }


        return best;
    }




    /** ============================
     *  1) Generate Initial Population (CHUẨN)
     * ============================ */
    private List<Chromosome> generateInitialPopulation(SchedulingContext ctx) {


        List<Integer> staffIds = ctx.getStaff().stream()
                .map(s -> s.getAccountID())
                .toList();


        List<Chromosome> pop = new ArrayList<>();


        List<String> shifts = ctx.getShiftTypes();
        List<String> dates = ctx.getWeekDates()
                .stream().map(LocalDate::toString).toList();


        for (int i = 0; i < populationSize; i++) {


            List<Gene> genes = new ArrayList<>();


            for (String date : dates) {
                for (String shift : shifts) {


                    // 🔍 Lấy danh sách nhân viên request đúng ca / ngày
                    List<Integer> valid = staffIds.stream()
                            .filter(id -> ctx.hasRequestedShift(id, date, shift))
                            .toList();


                    // ❗ Không ai request -> không xếp ai
                    if (valid.isEmpty()) continue;


                    // ❗ Nhiều người request -> xếp đúng bằng đó người (không lặp)
                    for (Integer staffId : valid) {
                        genes.add(Gene.builder()
                                .date(date)
                                .shiftType(shift)
                                .staffId(staffId)
                                .fitnessBonus(0)
                                .build());
                    }
                }
            }


            pop.add(Chromosome.builder()
                    .genes(genes)
                    .fitnessScore(0)
                    .build());
        }


        return pop;
    }






    /** ============================
     *  2) Tournament Selection
     * ============================ */
    private Chromosome select(List<Chromosome> pop) {
        int k = 5;
        Chromosome best = null;


        for (int i = 0; i < k; i++) {
            Chromosome c = pop.get(random.nextInt(pop.size()));
            if (best == null || c.getFitnessScore() > best.getFitnessScore()) {
                best = c;
            }
        }
        return best;
    }


    /** ============================
     *  Helper
     * ============================ */
    private Chromosome getBest(List<Chromosome> list) {
        return list.stream()
                .max(Comparator.comparingDouble(Chromosome::getFitnessScore))
                .orElseThrow();
    }
}

