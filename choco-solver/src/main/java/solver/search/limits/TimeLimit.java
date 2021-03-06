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

package solver.search.limits;

import solver.Solver;
import solver.search.loop.monitors.IMonitorOpenNode;

/**
 * A limit over run time.
 * It acts as a monitor, to be up-to-date when the search loop asks for limit reaching.
 * <br/>
 *
 * @author Charles Prud'homme
 * @since 19/04/11
 */
public class TimeLimit extends ALimit implements IMonitorOpenNode {

    private long timeLimit;

    public TimeLimit(Solver solver, long timeLimit) {
        super(solver.getSearchLoop().getMeasures());
        this.timeLimit = timeLimit;
        solver.getSearchLoop().plugSearchMonitor(this);
    }


    @Override
    public boolean isReached() {
        final float diff = timeLimit - measures.getTimeCount();
        return diff <= 0.0;
    }

    @Override
    public String toString() {
        return String.format("Time: %.3f >= %d", measures.getTimeCount(), timeLimit);
    }

    @Override
    public long getLimitValue() {
        return timeLimit;
    }

    @Override
    public void overrideLimit(long newLimit) {
        timeLimit = newLimit;
    }

    @Override
    public void beforeOpenNode() {
        this.measures.updateTimeCount();
    }

    @Override
    public void afterOpenNode() {
    }

}
