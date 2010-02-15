package kieker.tpan.plugins.dependencyGraph;

import java.util.Collection;
import java.util.TreeMap;

/*
 * ==================LICENCE=========================
 * Copyright 2006-2010 Kieker Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==================================================
 */

/**
 *
 * @author Andre van Hoorn
 */
public class DependencyNode<T> {
    private final T entity;
    private final int id;

    private final TreeMap<Integer, DependencyEdge> incomingDependencies
            = new TreeMap<Integer, DependencyEdge>();

     private final TreeMap<Integer, DependencyEdge> outgoingDependencies
            = new TreeMap<Integer, DependencyEdge>();

    public DependencyNode (final int id, final T entity){
        this.id = id;
        this.entity = entity;
    }

   public final T getAllocationComponent() {
        return this.entity;
    }

    public final Collection<DependencyEdge> getIncomingDependencies() {
        return this.incomingDependencies.values();
    }

    public final Collection<DependencyEdge> getOutgoingDependencies() {
        return this.outgoingDependencies.values();
    }

    public void addOutgoingDependency(DependencyNode<T> destination){
        DependencyEdge e = this.outgoingDependencies.get(destination.getId());
        if (e == null){
            e = new DependencyEdge();
            e.setSource(this);
            e.setDestination(destination);
            this.outgoingDependencies.put(destination.getId(), e);
        }
        //e.incOutgoingWeight();
    }

    public void addIncomingDependency(DependencyNode source){
       DependencyEdge e = this.incomingDependencies.get(source.getId());
        if (e == null){
            e = new DependencyEdge();
            e.setSource(this);
            e.setDestination(source);
            this.incomingDependencies.put(source.getId(), e);
        }
        //e.incIncomingWeight();
    }

    public final int getId() {
        return this.id;
    }
}
