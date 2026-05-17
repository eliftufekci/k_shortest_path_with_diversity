package org.example.core;

/**
 * Five path similarity metrics from the paper (Table 3).
 * All return a value in [0,1]; higher means more similar.
 * S_Pi ∩ S_Pj denotes the set of shared edges; L(·) is their total weight.
 */
public enum SimilarityMetric {

    /**
     * Sim1 – Weighted Jaccard:
     *   L(inter) / ( L(Pi) + L(Pj) - L(inter) )
     */
    SIM1,

    /**
     * Sim4 – Overlap ratio to the longer path:
     *   L(inter) / max{ L(Pi), L(Pj) }
     */
    SIM4,

    /**
     * Sim3 – Geometric mean of per-path overlap ratios:
     *   sqrt( L(inter)^2 / ( L(Pi)·L(Pj) ) )
     */
    SIM3,

    /**
     * Sim2 – Arithmetic mean of per-path overlap ratios:
     *   L(inter)/(2·L(Pi))  +  L(inter)/(2·L(Pj))
     */
    SIM2,

    /**
     * Sim5 – Overlap ratio to the shorter path:
     *   L(inter) / min{ L(Pi), L(Pj) }
     */
    SIM5
}
