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

package solver.constraints.propagators.reified;

import solver.constraints.Constraint;
import solver.constraints.propagators.Propagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.constraints.reified.ImplicationConstraint;
import solver.exception.ContradictionException;
import solver.variables.BoolVar;
import solver.variables.EventType;
import solver.variables.Variable;
import util.ESat;
import util.tools.ArrayUtils;

/**
 * Implication propagator
 * <p/>
 * <br/>
 *
 * @author Jean-Guillaume Fages
 * @since 02/2013
 */
public class PropImplied extends Propagator<Variable> {

    // boolean variable of the reification
    private final BoolVar bVar;
    // constraint to apply if bVar = true
    private final Constraint impliedCons;
    // constraint of this propagator
    private final ImplicationConstraint reifCons;

    public PropImplied(BoolVar bool, ImplicationConstraint reifCons, Constraint consIfBoolTrue) {
        super(ArrayUtils.append(new BoolVar[]{bool}, reifCons.getVariables()), PropagatorPriority.LINEAR, false);
        this.bVar = (BoolVar) vars[0];
        this.impliedCons = consIfBoolTrue;
        this.reifCons = reifCons;
    }

    @Override
    public void propagate(int evtmask) throws ContradictionException {
        if (bVar.instantiated()) {
            if (bVar.getBooleanValue() == ESat.TRUE) {
                reifCons.activate();
            }
            setPassive();
        } else {
            ESat sat = impliedCons.isEntailed();
            if (sat == ESat.FALSE) {
                bVar.setToFalse(aCause);
                setPassive();
            }
        }
    }

    @Override
    public void propagate(int varIdx, int mask) throws ContradictionException {
        if (varIdx == 0) {
            if (bVar.getBooleanValue() == ESat.TRUE) {
                reifCons.activate();
            }
            setPassive();
        } else {
            forcePropagate(EventType.FULL_PROPAGATION);
        }
    }

    @Override
    public int getPropagationConditions(int vIdx) {
        // we do not known which kind of variables are involved in the target constraint
        return EventType.ALL_FINE_EVENTS.mask;
    }

    @Override
    public ESat isEntailed() {
        if (bVar.instantiated()) {
            if (bVar.getValue() == 1) {
                return impliedCons.isEntailed();
            } else {
                return ESat.TRUE;
            }
        }
        return ESat.UNDEFINED;
    }

    @Override
    public String toString() {
        return bVar.toString() + "=>" + impliedCons.toString();
    }
}
