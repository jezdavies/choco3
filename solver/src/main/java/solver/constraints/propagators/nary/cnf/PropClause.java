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

package solver.constraints.propagators.nary.cnf;

import choco.kernel.ESat;
import solver.Solver;
import solver.constraints.Constraint;
import solver.constraints.nary.cnf.ALogicTree;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.requests.IRequest;
import solver.variables.BoolVar;
import solver.variables.EventType;

/**
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 22 nov. 2010
 */
public class PropClause extends Propagator<BoolVar> {

    // index of the first not positive (resp. negative) literal.
    final int firstNotPosLit;

    int watchLit1, watchLit2;

    @SuppressWarnings({"unchecked"})
    public PropClause(ALogicTree t, Solver solver,
                      Constraint constraint) {
        super(t.flattenBoolVar(), solver, constraint, PropagatorPriority.LINEAR, false);
        this.firstNotPosLit = t.getNbPositiveLiterals();
    }

    @SuppressWarnings({"unchecked"})
    protected PropClause(Solver solver,
                         Constraint constraint) {
        super(new BoolVar[0], solver, constraint, PropagatorPriority.UNARY, false);
        this.firstNotPosLit = 0;
    }

    void awakeOnInst(int index) throws ContradictionException {
        int val = vars[index].getValue();
        if ((index < firstNotPosLit && val == 1)
                || (index >= firstNotPosLit && val == 0)) {
            setPassive();
            return;
        }
        if (watchLit1 == index) {
            setWatchLiteral(watchLit2);
        } else if (watchLit2 == index) {
            setWatchLiteral(watchLit1);
        }
        //HACK
        //propagate();
    }

    /**
     * Search a watchLiteral. A watchLiteral (or wL) is pointing out one variable not yet instantiated.
     * If every variables are instantiated, get out.
     * Otherwise, set the new not yet instantiated wL.
     *
     * @param otherWL previous known wL
     * @throws ContradictionException if a contradiction occurs
     */
    private void setWatchLiteral(int otherWL) throws ContradictionException {
        int i = 0;
        int cnt = 0;

        BoolVar bv;
        for (; i < firstNotPosLit; i++) {
            bv = vars[i];
            if (bv.instantiated()) {
                if (bv.getValue() == 1) {
                    setPassive();
                    return;
                } else {
                    cnt++;
                }
            } else if (i != otherWL) {
                watchLit1 = i;
                watchLit2 = otherWL;
                return;
            }
        }
        for (; i < vars.length; i++) {
            bv = vars[i];
            if (bv.instantiated()) {
                if (bv.getValue() == 0) {
                    setPassive();
                    return;
                } else {
                    cnt++;
                }
            } else if (i != otherWL) {
                watchLit1 = i;
                watchLit2 = otherWL;
                return;
            }
        }
        if (cnt == vars.length) {
            this.contradiction(null, "Inconsistent");
        }
        if (i == vars.length) {
            if (otherWL < firstNotPosLit) {
                vars[otherWL].instantiateTo(1, this, false);
            } else {
                vars[otherWL].instantiateTo(0, this, false);
            }
            setPassive();
        }
    }

    @Override
    public void propagate() throws ContradictionException {
        if (vars.length == 1) {
            if (firstNotPosLit == 1) {
                vars[0].instantiateTo(1, this, false);
            } else {
                vars[0].instantiateTo(0, this, false);
            }
            setPassive();
        } else {
            // search for watch literals and check the clause
            int n = vars.length;
            int i = 0, wl = 0, cnt = 0;
            while (i < n && wl < 2) {
                BoolVar bv = vars[i];
                if (bv.instantiated()) {
                    if (i < firstNotPosLit) {
                        if (bv.getValue() == 1) {
                            setPassive();
                            return;
                        } else {
                            cnt++;
                        }
                    } else {
                        if (bv.getValue() == 0) {
                            setPassive();
                            return;
                        } else {
                            cnt++;
                        }
                    }
                } else {
                    watchLit2 = watchLit1;
                    watchLit1 = i;
                    wl++;
                }
                i++;
            }
            if (cnt == n) {
                this.contradiction(null, "Inconsistent");
            } else if (cnt == n - 1) {
                setWatchLiteral(watchLit1);
            }
        }
    }


    @Override
    public void propagateOnRequest(IRequest<BoolVar> boolVarIFineRequest, int varIdx, int mask) throws ContradictionException {
        if (EventType.isInstantiate(mask)) {
            this.awakeOnInst(varIdx);
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        return EventType.INSTANTIATE.mask;
    }

    @Override
    public String toString() {
        StringBuilder st = new StringBuilder();
        int i = 0;
        for (; i < firstNotPosLit; i++) {
            st.append(vars[i].getName()).append(" or ");
        }
        for (; i < vars.length; i++) {
            st.append("not(").append(vars[i].getName()).append(") or ");
        }
        st.replace(st.length() - 4, st.length(), "");
        return st.toString();
    }

    @Override
    public ESat isEntailed() {
        int i = 0;
        int cnt = vars.length;
        for (; i < firstNotPosLit; i++) {
            if (vars[i].instantiated()) {
                if (vars[i].getValue() == 1) {
                    return ESat.TRUE;
                } else {
                    cnt--;
                }
            }
        }
        for (; i < vars.length; i++) {
            if (vars[i].instantiated()) {
                if (vars[i].getValue() == 0) {
                    return ESat.TRUE;
                } else {
                    cnt--;
                }
            }
        }
        if (cnt == 0) {
            return ESat.FALSE;
        }
        return ESat.UNDEFINED;
    }
}
