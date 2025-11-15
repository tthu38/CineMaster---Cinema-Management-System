package com.example.cinemaster.service.ai.core;


import com.example.cinemaster.service.ai.dto.SchedulingContext;
import java.util.List;
import java.util.Random;


public class SimulatedAnnealing {


    private static final Random rand = new Random();


    private double initialTemp = 800;     // giảm nhẹ để ít phá GA
    private double coolingRate = 0.004;   // giảm chậm hơn → kết quả ổn hơn


    public Chromosome optimize(Chromosome c, SchedulingContext ctx) {


        Chromosome current = c.deepCopy();
        current.evaluateFitness(ctx);


        Chromosome best = current.deepCopy();


        double temp = initialTemp;


        while (temp > 1) {


            Chromosome neighbor = current.deepCopy();
            mutateNeighbor(neighbor, ctx);
            neighbor.evaluateFitness(ctx);


            double curFit = current.getFitnessScore();
            double neiFit = neighbor.getFitnessScore();


            if (accept(curFit, neiFit, temp)) {
                current = neighbor;
            }


            if (current.getFitnessScore() > best.getFitnessScore()) {
                best = current.deepCopy();
            }


            temp *= (1 - coolingRate);
        }


        return best;
    }


    /** ============================
     * Mutation KHÔNG BAO GIỜ tạo ca không ai request
     * ============================ */
    private void mutateNeighbor(Chromosome c, SchedulingContext ctx) {
        List<Integer> ids = ctx.getStaff()
                .stream().map(s -> s.getAccountID()).toList();


        // mutate 1 gene
        c.mutate(ids, ctx);
    }


    /** ============================
     * SA acceptance
     * ============================ */
    private boolean accept(double oldFit, double newFit, double temp) {
        if (newFit > oldFit) return true; // tốt hơn → nhận ngay
        return Math.exp((newFit - oldFit) / temp) > Math.random();
    }
}

