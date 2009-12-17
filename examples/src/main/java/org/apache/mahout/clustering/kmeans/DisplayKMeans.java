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

package org.apache.mahout.clustering.kmeans;

import java.awt.BasicStroke;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.mahout.clustering.canopy.Canopy;
import org.apache.mahout.clustering.dirichlet.DisplayDirichlet;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.common.distance.DistanceMeasure;
import org.apache.mahout.common.distance.ManhattanDistanceMeasure;
import org.apache.mahout.common.RandomUtils;

class DisplayKMeans extends DisplayDirichlet {
  DisplayKMeans() {
    initialize();
    this.setTitle("K-Means Clusters (> 5% of population)");
  }

  private static List<List<Cluster>> clusters;

  private static final double t1 = 3.0;

  private static final double t2 = 1.5;

  @Override
  public void paint(Graphics g) {
    super.plotSampleData(g);
    Graphics2D g2 = (Graphics2D) g;
    Vector dv = new DenseVector(2);
    int i = clusters.size() - 1;
    for (List<Cluster> cls : clusters) {
      g2.setStroke(new BasicStroke(i == 0 ? 3 : 1));
      g2.setColor(colors[Math.min(colors.length - 1, i--)]);
      for (Cluster cluster : cls) {
        //if (true || cluster.getNumPoints() > sampleData.size() * 0.05) {
          dv.assign(cluster.getStd() * 3);
          plotEllipse(g2, cluster.getCenter(), dv);
        //}
      }
    }
  }

  /**
   * This is the reference k-means implementation. Given its inputs it iterates
   * over the points and clusters until their centers converge or until the
   * maximum number of iterations is exceeded.
   * 
   * @param points the input List<Vector> of points
   * @param clusters the initial List<Cluster> of clusters
   * @param measure the DistanceMeasure to use
   * @param maxIter the maximum number of iterations
   */
  private static void referenceKmeans(List<Vector> points,
      List<List<Cluster>> clusters, DistanceMeasure measure, int maxIter) {
    boolean converged = false;
    int iteration = 0;
    while (!converged && iteration < maxIter) {
      List<Cluster> next = new ArrayList<Cluster>();
      List<Cluster> cs = clusters.get(iteration++);
      for (Cluster c : cs)
        next.add(new Cluster(c.getCenter()));
      clusters.add(next);
      converged = iterateReference(points, clusters.get(iteration), measure);
    }
  }

  /**
   * Perform a single iteration over the points and clusters, assigning points
   * to clusters and returning if the iterations are completed.
   * 
   * @param points the List<Vector> having the input points
   * @param clusters the List<Cluster> clusters
   * @param measure a DistanceMeasure to use
   * @return
   */
  private static boolean iterateReference(List<Vector> points,
      List<Cluster> clusters, DistanceMeasure measure) {
    // iterate through all points, assigning each to the nearest cluster
    for (Vector point : points) {
      Cluster closestCluster = null;
      double closestDistance = Double.MAX_VALUE;
      for (Cluster cluster : clusters) {
        double distance = measure.distance(cluster.getCenter(), point);
        if (closestCluster == null || closestDistance > distance) {
          closestCluster = cluster;
          closestDistance = distance;
        }
      }
      closestCluster.addPoint(point);
    }
    // test for convergence
    boolean converged = true;
    for (Cluster cluster : clusters) {
      if (!cluster.computeConvergence(measure, 0.001))
        converged = false;
    }
    // update the cluster centers
    if (!converged)
      for (Cluster cluster : clusters)
        cluster.recomputeCenter();
    return converged;
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
      List<Vector> points, double t1, double t2) {
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
      Iterator<Vector> ptIter = points.iterator();
      Vector p1 = ptIter.next();
      ptIter.remove();
      Canopy canopy = new Canopy(p1, nextCanopyId++);
      canopies.add(canopy);
      while (ptIter.hasNext()) {
        Vector p2 = ptIter.next();
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
    List<Vector> points = new ArrayList<Vector>();
    points.addAll(sampleData);
    List<Canopy> canopies = populateCanopies(new ManhattanDistanceMeasure(), points, t1, t2);
    DistanceMeasure measure = new ManhattanDistanceMeasure();
    clusters = new ArrayList<List<Cluster>>();
    clusters.add(new ArrayList<Cluster>());
    for (Canopy canopy : canopies)
      if (canopy.getNumPoints() > 0.05 * sampleData.size())
        clusters.get(0).add(new Cluster(canopy.getCenter()));
    referenceKmeans(sampleData, clusters, measure, 10);
    new DisplayKMeans();
  }
}
