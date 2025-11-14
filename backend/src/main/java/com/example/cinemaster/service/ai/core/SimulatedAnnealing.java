package com.example.cinemaster.service.ai.core;

import com.example.cinemaster.service.ai.dto.SchedulingContext;

import java.util.Random;

public class SimulatedAnnealing {

    private static final Random rand = new Random();

    private double initialTemp = 1000.0;
    private double coolingRate = 0.003;

    public Chromosome optimize(Chromosome chromosome, SchedulingContext ctx) {
        Chromosome current = chromosome.deepCopy();
        Chromosome best = current.deepCopy();

        double temp = initialTemp;

        while (temp > 1) {

            Chromosome neighbor = current.deepCopy();
            mutate(neighbor);

            double currentFitness = current.getFitness(ctx);
            double neighborFitness = neighbor.getFitness(ctx);

            if (acceptanceProbability(currentFitness, neighborFitness, temp) > Math.random()) {
                current = neighbor;
            }

            if (current.getFitness(ctx) > best.getFitness(ctx)) {
                best = current.deepCopy();
            }

            temp *= (1 - coolingRate);
        }

        return best;
    }

    private void mutate(Chromosome c) {
        int i = rand.nextInt(c.getGenes().size());
        int j = rand.nextInt(c.getGenes().size());

        var g1 = c.getGenes().get(i);
        var g2 = c.getGenes().get(j);

        int tmp = g1.getStaffId();
        g1.setStaffId(g2.getStaffId());
        g2.setStaffId(tmp);
    }

    private double acceptanceProbability(double currentFit, double newFit, double temp) {
        if (newFit > currentFit) return 1.0;
        return Math.exp((newFit - currentFit) / temp);
    }
}

