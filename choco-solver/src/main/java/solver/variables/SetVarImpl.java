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

package solver.variables;

import memory.IEnvironment;
import solver.ICause;
import solver.Solver;
import solver.exception.ContradictionException;
import solver.explanations.Explanation;
import solver.explanations.VariableState;
import solver.variables.delta.SetDelta;
import solver.variables.delta.monitor.SetDeltaMonitor;
import util.objects.setDataStructures.ISet;
import util.objects.setDataStructures.SetFactory;
import util.objects.setDataStructures.SetType;

/**
 * Set variable to represent a set of integers in the range [0,n-1]
 *
 * @author Jean-Guillaume Fages
 * @since Oct 2012
 */
public class SetVarImpl extends AbstractVariable<SetDelta, SetVar> implements SetVar {

    //////////////////////////////// GRAPH PART /////////////////////////////////////////
    //***********************************************************************************
    // VARIABLES
    //***********************************************************************************

    protected ISet envelope, kernel;
    protected IEnvironment environment;
    protected SetDelta delta;
    ///////////// Attributes related to Variable ////////////
    protected boolean reactOnModification;

    //***********************************************************************************
    // CONSTRUCTORS
    //***********************************************************************************

    /**
     * Set variable based on a linked list representation
     *
     * @param name
     * @param solver
     */
    public SetVarImpl(String name, Solver solver) {
        this(name, 0, SetType.LINKED_LIST, SetType.LINKED_LIST, solver);
    }

    /**
     * Set variable
     *
     * @param name
     * @param maximalSize values will be in range [0, maximalSize-1]
     * @param envType     data structure of the envelope
     * @param kerType     data structure of the kernel
     * @param solver
     */
    public SetVarImpl(String name, int maximalSize, SetType envType, SetType kerType, Solver solver) {
        super(name, solver);
        solver.associates(this);
        this.environment = solver.getEnvironment();
        envelope = SetFactory.makeStoredSet(envType, maximalSize, environment);
        kernel = SetFactory.makeStoredSet(kerType, maximalSize, environment);
    }

    //***********************************************************************************
    // METHODS
    //***********************************************************************************

    @Override
    public boolean instantiated() {
        return envelope.getSize() == kernel.getSize();
    }

    @Override
    public boolean addToKernel(int element, ICause cause) throws ContradictionException {
        assert cause != null;
        if (!envelope.contain(element)) {
            contradiction(cause, null, "");
            return true;
        }
        if (kernel.contain(element)) {
            return false;
        }
        kernel.add(element);
        if (reactOnModification) {
            delta.add(element, SetDelta.KERNEL, cause);
        }
        EventType e = EventType.ADD_TO_KER;
        notifyPropagators(e, cause);
        return true;
    }

    @Override
    public boolean removeFromEnvelope(int element, ICause cause) throws ContradictionException {
        assert cause != null;
        if (kernel.contain(element)) {
            contradiction(cause, EventType.REMOVE_FROM_ENVELOPE, "");
            return true;
        }
        if (!envelope.remove(element)) {
            return false;
        }
        if (reactOnModification) {
            delta.add(element, SetDelta.ENVELOP, cause);
        }
        EventType e = EventType.REMOVE_FROM_ENVELOPE;
        notifyPropagators(e, cause);
        return true;
    }

    @Override
    public boolean instantiateTo(int[] value, ICause cause) throws ContradictionException {
        boolean changed = !instantiated();
        for (int i : value) {
            addToKernel(i, cause);
        }
        if (kernel.getSize() != value.length) {
            contradiction(cause, null, "");
        }
        if (envelope.getSize() != value.length) {
            for (int i = envelope.getFirstElement(); i >= 0; i = envelope.getNextElement()) {
                if (!kernel.contain(i)) {
                    removeFromEnvelope(i, cause);
                }
            }
        }
        return changed;
    }

    @Override
    public boolean contains(int v) {
        return envelope.contain(v);
    }

    @Override
    public int[] getValue() {
        int[] lb = new int[kernel.getSize()];
        int k = 0;
        for (int i = kernel.getFirstElement(); i >= 0; i = kernel.getNextElement()) {
            lb[k++] = i;
        }
        return lb;
    }

    //***********************************************************************************
    // ACCESSORS
    //***********************************************************************************

    @Override
    public ISet getKernel() {
        return kernel;
    }

    @Override
    public ISet getEnvelope() {
        return envelope;
    }

    //***********************************************************************************
    // VARIABLE STUFF
    //***********************************************************************************

    @Override
    public void explain(VariableState what, Explanation to) {
        throw new UnsupportedOperationException("SetVar does not (yet) implement method explain(...)");
    }

    @Override
    public void explain(VariableState what, int val, Explanation to) {
        throw new UnsupportedOperationException("SetVar does not (yet) implement method explain(...)");
    }

    @Override
    public SetDelta getDelta() {
        return delta;
    }

    @Override
    public int getTypeAndKind() {
        return VAR | SET;
    }

    @Override
    public SetVar duplicate() {
        throw new UnsupportedOperationException("Cannot duplicate SetVar");
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public void createDelta() {
        if (!reactOnModification) {
            reactOnModification = true;
            delta = new SetDelta(solver.getSearchLoop());
        }
    }

    @Override
    public SetDeltaMonitor monitorDelta(ICause propagator) {
        createDelta();
        return new SetDeltaMonitor(delta, propagator);
    }

    public void notifyPropagators(EventType event, ICause cause) throws ContradictionException {
        assert cause != null;
        notifyMonitors(event);
        if ((modificationEvents & event.mask) != 0) {
            //records.forEach(afterModification.set(this, event, cause));
            solver.getEngine().onVariableUpdate(this, event, cause);
        }
        notifyViews(event, cause);
    }

    public void notifyMonitors(EventType event) throws ContradictionException {
        for (int i = mIdx - 1; i >= 0; i--) {
            monitors[i].onUpdate(this, event);
        }
    }

    @Override
    public void contradiction(ICause cause, EventType event, String message) throws ContradictionException {
        assert cause != null;
        solver.getEngine().fails(cause, this, message);
    }
}
