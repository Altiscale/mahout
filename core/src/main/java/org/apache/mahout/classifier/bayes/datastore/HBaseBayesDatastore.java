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

package org.apache.mahout.classifier.bayes.datastore;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.HTable;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.mahout.common.Parameters;
import org.apache.mahout.classifier.bayes.exceptions.InvalidDatastoreException;
import org.apache.mahout.classifier.bayes.interfaces.Datastore;
import org.apache.mahout.common.cache.Cache;
import org.apache.mahout.common.cache.HybridCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HBaseBayesDatastore implements Datastore {

  private static final Logger log = LoggerFactory.getLogger(HBaseBayesDatastore.class);
  
  protected HBaseConfiguration config = null;

  protected HTable table = null;

  protected Cache<String, Result> tableCache = null;

  protected final String hbaseTable;

  protected Parameters parameters = null;

  public HBaseBayesDatastore(String hbaseTable, Parameters params) {
    this.hbaseTable = hbaseTable;
    this.parameters = params;
    this.tableCache = new HybridCache<String, Result>(50000, 100000);
  }

  protected double thetaNormalizer = 1.0d;

  @Override
  public void initialize() throws InvalidDatastoreException {
    config = new HBaseConfiguration(new Configuration());
    try {
      table = new HTable(config, hbaseTable);
    } catch (IOException e) {
      throw new InvalidDatastoreException(e.getMessage());
    }
    Collection<String> labels = getKeys("thetaNormalizer");
    for (String label : labels) {
      thetaNormalizer = Math.max(thetaNormalizer, Math.abs(getWeightFromHbase(
          "*thetaNormalizer", label)));
    }
    for (String label : labels) {
      System.out.println( label + " " +getWeightFromHbase(
          "*thetaNormalizer", label) +" " +thetaNormalizer + " " + getWeightFromHbase(
          "*thetaNormalizer", label)/thetaNormalizer);
    }
  }

  final Map<String, Set<String>> keys = new HashMap<String, Set<String>>();

  @Override
  public Collection<String> getKeys(String name)
      throws InvalidDatastoreException {
    if (keys.containsKey(name))
      return keys.get(name);
    Result r = null;
    if (name.equals("labelWeight")) {
      r = getRowFromHbase("*labelWeight");
    } else if (name.equals("thetaNormalizer")) {
      r = getRowFromHbase("*thetaNormalizer");
    } else
      r = getRowFromHbase(name);
    
    if (r == null){
      log.error("Encountered NULL");
      throw new InvalidDatastoreException("Encountered NULL");
    }

    Set<byte[]> labelBytes = r.getNoVersionMap().get(Bytes.toBytes("label"))
        .keySet();
    Set<String> keySet = new HashSet<String>();
    for (byte[] key : labelBytes) {
      keySet.add(Bytes.toString(key));
    }
    keys.put(name, keySet);
    return keySet;
  }

  @Override
  public double getWeight(String matrixName, String row, String column)
      throws InvalidDatastoreException {
    if ("weight".equals(matrixName)) {
      if (column.equals("sigma_j"))
        return getSigma_jFromHbase(row);
      else
        return getWeightFromHbase(row, column);
    } else
      throw new InvalidDatastoreException();
  }

  @Override
  public double getWeight(String vectorName, String index)
      throws InvalidDatastoreException {
    if (vectorName.equals("sumWeight")) {
      if (index.equals("vocabCount"))
        return getVocabCountFromHbase();
      else if (index.equals("sigma_jSigma_k"))
        return getSigma_jSigma_kFromHbase();
      else
        throw new InvalidDatastoreException();

    } else if (vectorName.equals("labelWeight")) {
      return getWeightFromHbase("*labelWeight", index);
    } else if (vectorName.equals("thetaNormalizer")) {
      return getWeightFromHbase("*thetaNormalizer", index) / thetaNormalizer;
    } else {

      throw new InvalidDatastoreException();
    }
  }

  protected double getCachedCell(String row, String family, String column) {
    Result r = null;

    if ((r = tableCache.get(row)) == null) {
      Get g = new Get(Bytes.toBytes(row));
      g.addFamily(Bytes.toBytes(family));
      try {
        r = table.get(g);
      } catch (IOException e) {
        return 0.0d;
      }
      tableCache.set(row, r);
    }
    byte[] value = r.getValue(Bytes.toBytes("label"), Bytes.toBytes(column));
    if (value == null)
      return 0.0d;
    return Bytes.toDouble(value);

  }

  protected double getWeightFromHbase(String feature, String label) {
    return getCachedCell(feature, "label", label);
  }

  protected Result getRowFromHbase(String feature) {
    Result r = null;
    try {
      if ((r = tableCache.get(feature)) == null) {
        Get g = new Get(Bytes.toBytes(feature));
        g.addFamily(Bytes.toBytes("label"));
        r = table.get(g);
        tableCache.set(feature, r);
        return r;
      } else
        return r;

    } catch (IOException e) {
      return r;
    }
  }

  protected double getSigma_jFromHbase(String feature) {
    return getCachedCell(feature, "label", "Sigma_j");
  }

  protected double vocabCount = -1.0;

  protected double getVocabCountFromHbase() {
    if (vocabCount == -1.0) {
      vocabCount = getCachedCell("*totalCounts", "label", "vocabCount");
      return vocabCount;
    } else {
      return vocabCount;
    }
  }

  protected double sigma_jSigma_k = -1.0;

  protected double getSigma_jSigma_kFromHbase() {
    if (sigma_jSigma_k == -1.0) {
      sigma_jSigma_k = getCachedCell("*totalCounts", "label", "sigma_jSigma_k");
      return sigma_jSigma_k;
    } else {
      return sigma_jSigma_k;
    }
  }

}
