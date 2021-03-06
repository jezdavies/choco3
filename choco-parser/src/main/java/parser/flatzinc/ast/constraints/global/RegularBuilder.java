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

package parser.flatzinc.ast.constraints.global;

import gnu.trove.map.hash.THashMap;
import parser.flatzinc.ast.constraints.IBuilder;
import parser.flatzinc.ast.expression.EAnnotation;
import parser.flatzinc.ast.expression.Expression;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.IntConstraintFactory;
import solver.constraints.nary.automata.FA.FiniteAutomaton;
import solver.variables.IntVar;

import java.util.List;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 26/01/11
 */
public class RegularBuilder implements IBuilder {

    @Override
    public Constraint[] build(Solver solver, String name, List<Expression> exps, List<EAnnotation> annotations, THashMap<String, Object> map) {
//        array[int] of var int: x, int: Q, int: S,
//        array[int,int] of int: d, int: q0, set of int: F
        IntVar[] vars = exps.get(0).toIntVarArray(solver);
        int Q = exps.get(1).intValue();
        int S = exps.get(2).intValue();
        int[] d = exps.get(3).toIntArray();
        int q0 = exps.get(4).intValue();
        int[] F = exps.get(5).toIntArray();
        FiniteAutomaton auto = new FiniteAutomaton();
        for (int q = 0; q <= Q; q++) auto.addState();
        auto.setInitialState(q0);
        auto.setFinal(F);

        for (int i = 0, k = 0; i < Q; i++) {
            for (int j = 0; j < S; j++, k++) {
                // 0 is the fail state;
                if (d[k] > 0) {
                    auto.addTransition(i + 1, d[k], j + 1);
                }
            }
        }
//        auto.removeDeadTransitions();
//        auto.minimize();

        return new Constraint[]{IntConstraintFactory.regular(vars, auto)};
    }
}
