/**
 *  Copyright (c) 1999-2011, Ecole des Mines de Nantes
 *  All rights reserved.
 *  Redistribution and use in source and binary forms, with or without
 *  modification, are permitted provided that the following conditions are met:
 *
 *      * Redistributions of source code must retain the above copyright
 *        notice, this list of conditions and the following disclaimer.
 *      * Redistributions in binary form must reproduce the above copyright
 *        notice, this list of conditions and the following disclaimer in the
 *        documentation and/or other materials provided with the distribution.
 *      * Neither the name of the Ecole des Mines de Nantes nor the
 *        names of its contributors may be used to endorse or promote products
 *        derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE REGENTS AND CONTRIBUTORS ``AS IS'' AND ANY
 *  EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE REGENTS AND CONTRIBUTORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package samples.integer;

import org.kohsuke.args4j.Option;
import org.slf4j.LoggerFactory;
import samples.AbstractProblem;
import solver.Solver;
import solver.constraints.IntConstraintFactory;
import solver.search.strategy.IntStrategyFactory;
import solver.variables.BoolVar;
import solver.variables.VariableFactory;
import util.ESat;
import util.tools.ArrayUtils;

/**
 * CSPLib: prob015:<br/>
 * "The problem is to put n balls labelled {1,...n} into 3 boxes so that
 * for any triple of balls (x,y,z) with x+y=z, not all are in the same box.
 * This has a solution iff n < 14.
 * <br/>
 * One natural generalization is to consider partitioning into k boxes (for k>3)."
 * <p/>
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 19/08/11
 */
public class SchurLemma extends AbstractProblem {

    @Option(name = "-n", usage = "Number of balls.", required = false)
    int n = 43;

    @Option(name = "-k", usage = "Number of boxes.", required = false)
    int k = 4;


    BoolVar[][] M;

    @Override
    public void createSolver() {
        solver = new Solver();
    }

    @Override
    public void buildModel() {

        M = VariableFactory.boolMatrix("b", n, k, solver); // M_ij is true iff ball i is in box j

        for (int i = 0; i < n; i++) {
            solver.post(IntConstraintFactory.sum(M[i], VariableFactory.fixed(1, solver)));
        }

        for (int i = 0; i < k; i++) {
            for (int x = 1; x <= n; x++) {
                for (int y = 1; y <= n; y++) {
                    for (int z = 1; z <= n; z++) {
                        if (x + y == z)
                            solver.post(IntConstraintFactory.sum(new BoolVar[]{M[x - 1][i], M[y - 1][i], M[z - 1][i]}, VariableFactory.bounded("sum", 0, 2, solver)));
                    }
                }
            }
        }
    }

    @Override
    public void configureSearch() {
        solver.set(IntStrategyFactory.inputOrder_InDomainMin(ArrayUtils.flatten(M)));
    }

    @Override
    public void solve() {
        solver.findSolution();
    }

    @Override
    public void prettyOut() {
        LoggerFactory.getLogger("bench").info("Schur's lemma ({},{})", new Object[]{n, k});
        StringBuilder st = new StringBuilder();
        if (solver.isFeasible() == ESat.TRUE) {
            for (int i = 0; i < k; i++) {
                st.append("\tBox #").append(i + 1).append(": ");
                for (int j = 0; j < n; j++) {
                    if (M[j][i].getValue() > 0) {
                        st.append(j + 1).append(" ");
                    }
                }
                st.append("\n");
            }
        } else {
            st.append("\tINFEASIBLE");
        }
        LoggerFactory.getLogger("bench").info(st.toString());
    }

    public static void main(String[] args) {
        new SchurLemma().execute(args);
    }
}
