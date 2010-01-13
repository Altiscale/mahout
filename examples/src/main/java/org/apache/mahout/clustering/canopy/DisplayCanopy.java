/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.mahout.clustering.canopy;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.mahout.clustering.dirichlet.DisplayDirichlet;
import org.apache.mahout.clustering.dirichlet.models.NormalModelDistribution;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.ManhattanDistanceMeasure;
import org.apache.mahout.common.RandomUtils;
import org.apache.mahout.math.VectorWritable;

class DisplayCanopy extends DisplayDirichlet {
  DisplayCanopy() {
    initialize();
    this.setTitle("Canopy Clusters (> 5% of population)");
  }

  private static List<Canopy> canopies;

  private static final double t1 = 3.0;

  private static final double t2 = 1.5;

  @Override
  public void paint(Graphics g) {
    super.plotSampleData(g);
    Graphics2D g2 = (Graphics2D) g;
    Vector dv = new DenseVector(2);
    for (Canopy canopy : canopies) {
      if (canopy.getNumPoints() > sampleData.size() * 0.05) {
        dv.assign(t1);
        g2.setColor(colors[0]);
        plotEllipse(g2, canopy.getCenter(), dv);
        dv.assign(t2);
        plotEllipse(g2, canopy.getCenter(), dv);
      }
    }
  }

  /**
   * Iterate through the points, adding new canopies. Return the canopies.
   * 
   * @param measure
   *            a DistanceMeasure to use
   * @param points
   *            a list<Vector> defining the points to be clustered
   * @param t1
   *            the T1 distance threshold
   * @param t2
   *            the T2 distance threshold
   * @return the List<Canopy> created
   */
  static List<Canopy> populateCanopies(DistanceMeasure measure,
      List<VectorWritable> points, double t1, double t2) {
    List<Canopy> canopies = new ArrayList<Canopy>();
    /**
     * Reference Implementation: Given a distance metric, one can create
     * canopies as follows: Start with a list of the data points in any order,
     * and with two distance thresholds, T1 and T2, where T1 > T2. (These
     * thresholds can be set by the user, or selected by cross-validation.) Pick
     * a point on the list and measure its distance to all other points. Put all
     * points that are within distance threshold T1 into a canopy. Remove from
     * the list all points that are within distance threshold T2. Repeat until
     * the list is empty.
     */
    int nextCanopyId = 0;
    while (!points.isEmpty()) {
      Iterator<VectorWritable> ptIter = points.iterator();
      Vector p1 = ptIter.next().get();
      ptIter.remove();
      Canopy canopy = new Canopy(p1, nextCanopyId++);
      canopies.add(canopy);
      while (ptIter.hasNext()) {
        Vector p2 = ptIter.next().get();
        double dist = measure.distance(p1, p2);
        // Put all points that are within distance threshold T1 into the canopy
        if (dist < t1)
          canopy.addPoint(p2);
        // Remove from the list all points that are within distance threshold T2
        if (dist < t2)
          ptIter.remove();
      }
    }
    return canopies;
  }

  public static void main(String[] args) {
    RandomUtils.useTestSeed();
    generateSamples();
    List<VectorWritable> points = new ArrayList<VectorWritable>();
    points.addAll(sampleData);
    canopies = populateCanopies(new ManhattanDistanceMeasure(), points, t1, t2);
    new DisplayCanopy();
  }

  static void generateResults() {
    DisplayDirichlet.generateResults(new NormalModelDistribution());
  }
}
