/*
 * Copyright (c) 1999-2012, Ecole des Mines de Nantes
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Ecole des Mines de Nantes nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package solver.constraints.propagators.gary.tsp.directed;

import choco.annotations.PropAnn;
import choco.kernel.ESat;
import choco.kernel.common.util.procedure.PairProcedure;
import choco.kernel.memory.IStateInt;
import gnu.trove.list.array.TIntArrayList;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.variables.EventType;
import solver.variables.IntVar;
import solver.variables.Variable;
import solver.variables.delta.monitor.GraphDeltaMonitor;
import solver.variables.graph.DirectedGraphVar;
import solver.variables.setDataStructures.ISet;

/**
 * Compute the cost of the graph by summing arcs costs
 * - For minimization problem
 */
@PropAnn(tested = PropAnn.Status.BENCHMARK)
public class PropPathOrCircuitEvalObj extends Propagator {

    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    DirectedGraphVar g;
    GraphDeltaMonitor gdm;
    int n;
    IntVar sum;
    int[][] distMatrix;
    IStateInt[] minCostSucc, maxCostSucc;
    PairProcedure arcEnforced, arcRemoved;
    IStateInt minSum;
    IStateInt maxSum;
    TIntArrayList toCompute;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Ensures that obj=SUM{costMatrix[i][j], (i,j) in arcs of graph}
     * - For minimization problem
     *
     * @param graph
     * @param obj
     * @param costMatrix
     * @param constraint
     * @param solver
     */
    public PropPathOrCircuitEvalObj(DirectedGraphVar graph, IntVar obj, int[][] costMatrix, Constraint constraint, Solver solver) {
        super(new Variable[]{graph, obj}, solver, constraint, PropagatorPriority.LINEAR);
        g = graph;
        gdm = (GraphDeltaMonitor) g.monitorDelta(this);
        sum = obj;
        n = g.getEnvelopGraph().getNbNodes();
        distMatrix = costMatrix;
        arcEnforced = new EnfArc();
        arcRemoved = new RemArc();
        minSum = environment.makeInt(0);
        maxSum = environment.makeInt(0);
        toCompute = new TIntArrayList();
        minCostSucc = new IStateInt[n];
        maxCostSucc = new IStateInt[n];
        for (int i = 0; i < n; i++) {
            minCostSucc[i] = environment.makeInt(-1);
            maxCostSucc[i] = environment.makeInt(-1);
        }
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        ISet succ;
        minSum.set(0);
        maxSum.set(0);
        for (int i = 0; i < n; i++) {
            succ = g.getEnvelopGraph().getSuccessorsOf(i);
            if (succ.getSize() > 0) {
                int min = succ.getFirstElement();
                int max = min;
                if (min == -1) {
                    contradiction(g, "");
                }
                int minC = distMatrix[i][min];
                int maxC = distMatrix[i][min];
                for (int s = min; s >= 0; s = succ.getNextElement()) {
                    if (distMatrix[i][s] < minC) {
                        minC = distMatrix[i][s];
                        min = s;
                    } else if (distMatrix[i][s] > maxC) {
                        maxC = distMatrix[i][s];
                        max = s;
                    }

                }
                minSum.add(minC);
                minCostSucc[i].set(min);
                maxSum.add(maxC);
                maxCostSucc[i].set(max);
            }
        }
        sum.updateLowerBound(minSum.get(), aCause);
        sum.updateUpperBound(maxSum.get(), aCause);
        // filter the graph
        ISet succs;
        int delta = minSum.get() - sum.getUB();
        int curMin;
        for (int i = 0; i < n; i++) {
            succs = g.getEnvelopGraph().getSuccessorsOf(i);
            if (succs.getSize() > 0) {
                curMin = distMatrix[i][minCostSucc[i].get()];
                for (int j = succs.getFirstElement(); j >= 0; j = succs.getNextElement()) {
                    if (delta > curMin - distMatrix[i][j]) {
                        g.removeArc(i, j, aCause);
                    }
                }
            }
        }
    }

    @Override
    public void propagate(int idxVarInProp, int mask) throws ContradictionException {
        toCompute.clear();
        int oldMin = minSum.get();
        Variable variable = vars[idxVarInProp];
        if ((variable.getTypeAndKind() & Variable.GRAPH) != 0) {
            gdm.freeze();
            if ((mask & EventType.ENFORCEARC.mask) != 0) {
                gdm.forEachArc(arcEnforced, EventType.ENFORCEARC);
            }
            if ((mask & EventType.REMOVEARC.mask) != 0) {
                gdm.forEachArc(arcRemoved, EventType.REMOVEARC);
            }
            gdm.unfreeze();
            for (int i = toCompute.size() - 1; i >= 0; i--) {
                findMin(toCompute.get(i));
            }
            sum.updateLowerBound(minSum.get(), aCause);
        }
        if ((minSum.get() > oldMin) || ((mask & EventType.DECUPP.mask) != 0)) {
            // filter the graph
            ISet succs;
            int delta = minSum.get() - sum.getUB();
            int curMin;
            for (int i = 0; i < n; i++) {
                succs = g.getEnvelopGraph().getSuccessorsOf(i);
                if (succs.getSize() > 0) {
                    curMin = distMatrix[i][minCostSucc[i].get()];
                    for (int j = succs.getFirstElement(); j >= 0; j = succs.getNextElement()) {
                        if (delta > curMin - distMatrix[i][j]) {
                            g.removeArc(i, j, aCause);
                        }
                    }
                }
            }
        }
    }

    private void findMin(int i) throws ContradictionException {
        ISet succ = g.getEnvelopGraph().getSuccessorsOf(i);
        int min = succ.getFirstElement();
        if (min == -1) {
            contradiction(g, "");
        }
        int minC = distMatrix[i][min];
        for (int s = min; s >= 0; s = succ.getNextElement()) {
            if (distMatrix[i][s] < minC) {
                minC = distMatrix[i][s];
                min = s;
            }
        }
        minSum.add(minC - distMatrix[i][minCostSucc[i].get()]);
        minCostSucc[i].set(min);
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.REMOVEARC.mask + EventType.ENFORCEARC.mask + EventType.DECUPP.mask + EventType.INSTANTIATE.mask;
    }

    @Override
    public ESat isEntailed() {
        return ESat.UNDEFINED;
    }

    //***********************************************************************************
    // PROCEDURES
    //***********************************************************************************

    private class EnfArc implements PairProcedure {
        @Override
        public void execute(int from, int to) throws ContradictionException {
            if (to != minCostSucc[from].get()) {
                minSum.add(distMatrix[from][to] - distMatrix[from][minCostSucc[from].get()]);
                minCostSucc[from].set(to);
            }
        }
    }

    private class RemArc implements PairProcedure {
        @Override
        public void execute(int from, int to) throws ContradictionException {
            if (to == minCostSucc[from].get()) {
                toCompute.add(from);
            }
        }
    }
}
