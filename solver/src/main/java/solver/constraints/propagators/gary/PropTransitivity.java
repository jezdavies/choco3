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

package solver.constraints.propagators.gary;

import choco.kernel.ESat;
import choco.kernel.common.util.procedure.IntProcedure;
import solver.Solver;
import solver.constraints.gary.GraphConstraint;
import solver.constraints.propagators.GraphPropagator;
import solver.constraints.propagators.PropagatorPriority;
import solver.exception.ContradictionException;
import solver.requests.GraphRequest;
import solver.requests.IRequest;
import solver.variables.EventType;
import solver.variables.delta.IntDelta;
import solver.variables.graph.GraphVar;
import solver.variables.graph.INeighbors;
import solver.variables.graph.directedGraph.DirectedGraphVar;

/**Propagator that ensures that the relation of the graph is transitive
 * 
 * TODO Incrementalite ? Pas sur que ce soit utile
 * 
 * @author Jean-Guillaume Fages
 */
public class PropTransitivity<V extends GraphVar> extends GraphPropagator<V>{

	//***********************************************************************************
	// VARIABLES
	//***********************************************************************************

	private V g;
	private int n;
	public static long duration;
	private IntProcedure arcEnforced;
	private IntProcedure arcRemoved;

	//***********************************************************************************
	// CONSTRUCTORS
	//***********************************************************************************

	public PropTransitivity(V graph, Solver solver, GraphConstraint constraint) {
		super((V[]) new GraphVar[]{graph}, solver, constraint, PropagatorPriority.LINEAR, false);
		g = graph;
		n = graph.getEnvelopGraph().getNbNodes();
		if( graph instanceof DirectedGraphVar){
			arcEnforced = new EnfArcDig(this,g);
			arcRemoved  = new RemArcDig(this,g);
		}else{
			arcEnforced = new EnfArcUndig(this);
			arcRemoved  = new RemArcUndig(this);
		}
	}

	//***********************************************************************************
	// PROPAGATIONS
	//***********************************************************************************

	@Override
	public void propagate() throws ContradictionException {duration=0;}

	
	@Override
	public void propagateOnRequest(IRequest<V> request, int idxVarInProp, int mask) throws ContradictionException {
		long time = System.currentTimeMillis();
		if( request instanceof GraphRequest){
			GraphRequest gr = (GraphRequest) request;
			if((mask & EventType.ENFORCEARC.mask) !=0){
				IntDelta d = (IntDelta) g.getDelta().getArcEnforcingDelta();
				d.forEach(arcEnforced, gr.fromArcEnforcing(), gr.toArcEnforcing());
			}
			if((mask & EventType.REMOVEARC.mask)!=0){
				IntDelta d = (IntDelta) g.getDelta().getArcRemovalDelta();
				d.forEach(arcRemoved, gr.fromArcRemoval(), gr.toArcRemoval());
			}
		}else{
			throw new UnsupportedOperationException("error ");
		}
		duration+=System.currentTimeMillis()-time;
	}

	//***********************************************************************************
	// INFO
	//***********************************************************************************

	@Override
	public int getPropagationConditions(int vIdx) {
		return EventType.REMOVEARC.mask +  EventType.ENFORCEARC.mask;
	}

	@Override
	public ESat isEntailed() {
		return ESat.UNDEFINED;
	}
	
	//***********************************************************************************
	// PROCEDURE
	//***********************************************************************************
	// --- Arc enforcings
	// undirected case
	private class EnfArcUndig implements IntProcedure{
		private GraphPropagator p;
		
		private EnfArcUndig(GraphPropagator p){
			this.p = p;
		}
		@Override
		public void execute(int i) throws ContradictionException {
			int from = i/n-1;
			int to   = i%n;
			if(from != to){
				apply(from,to);
				apply(to,from);
			}
		}
		private void apply(int node, int mate) throws ContradictionException {
			INeighbors ker = g.getKernelGraph().getNeighborsOf(node);
			INeighbors env = g.getEnvelopGraph().getNeighborsOf(node);
			INeighbors envMate = g.getEnvelopGraph().getNeighborsOf(mate);
			for(int i=env.getFirstElement(); i>=0; i = env.getNextElement()){
				if(ker.contain(i)){
					g.enforceArc(i, mate, p, false);
				}else if (!envMate.contain(i)){
					g.removeArc(i, node, p, false);
				}
			}
		}
	}
	// directed case
	private class EnfArcDig implements IntProcedure{
		private GraphPropagator p;
		private DirectedGraphVar g;
		
		private EnfArcDig(GraphPropagator p, V g){
			this.p = p;
			this.g = (DirectedGraphVar) g;
		}
		@Override
		public void execute(int i) throws ContradictionException {
			int from = i/n-1;
			int to   = i%n;
			if(from != to){
				apply(from,to);
			}
		}

		private void apply(int node, int mate) throws ContradictionException {
			INeighbors ker = g.getKernelGraph().getPredecessorsOf(node);
			for(int i=ker.getFirstElement(); i>=0; i = ker.getNextElement()){
				g.enforceArc(i, mate, p, false);
			}
			ker = g.getKernelGraph().getSuccessorsOf(mate);
			for(int i=ker.getFirstElement(); i>=0; i = ker.getNextElement()){
				g.enforceArc(node,i, p, false);
			}
		}
	}
	
	// --- Arc removals
	// undirected case
	private class RemArcUndig implements IntProcedure{
		private GraphPropagator p;
		
		private RemArcUndig(GraphPropagator p){
			this.p = p;
		}
		@Override
		public void execute(int i) throws ContradictionException {
			int from = i/n-1;
			int to   = i%n;
			if(from != to){
				apply(from,to);
				apply(to,from);
			}
		}

		private void apply(int node, int mate) throws ContradictionException {
			INeighbors nei = g.getEnvelopGraph().getNeighborsOf(node);
			for(int i=nei.getFirstElement(); i>=0; i = nei.getNextElement()){
				if(g.getKernelGraph().edgeExists(i, mate)){
					g.removeArc(node, i, p, false);
				}
			}
		}
	}
	// directed case
	private class RemArcDig implements IntProcedure{
		private GraphPropagator p;
		private DirectedGraphVar g;
		
		private RemArcDig(GraphPropagator p, V g){
			this.p = p;
			this.g = (DirectedGraphVar) g;
		}
		@Override
		public void execute(int i) throws ContradictionException {
			int from = i/n-1;
			int to   = i%n;
			if(from != to){
				INeighbors nei = g.getEnvelopGraph().getSuccessorsOf(from);
				for(i=nei.getFirstElement(); i>=0; i = nei.getNextElement()){
					if(g.getKernelGraph().arcExists(from,i)){
						g.removeArc(i, to, p, false);
					}else if(g.getKernelGraph().arcExists(i, to)){
						g.removeArc(from, i, p, false);
					}
				}
			}
		}
	}
}
