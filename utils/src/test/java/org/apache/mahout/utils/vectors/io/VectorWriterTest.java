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

package org.apache.mahout.utils.vectors.io;

import junit.framework.TestCase;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.LongWritable;
import org.apache.mahout.math.DenseVector;
import org.apache.mahout.math.SparseVector;
import org.apache.mahout.math.Vector;
import org.apache.mahout.utils.vectors.RandomVectorIterable;

import java.io.File;
import java.io.StringWriter;
import java.util.List;
import java.util.ArrayList;

public class VectorWriterTest extends TestCase {

  public void testSFVW() throws Exception {
    File tmpDir = new File(System.getProperty("java.io.tmpdir"));
    File tmpLoc = new File(tmpDir, "sfvwt");
    tmpLoc.mkdirs();
    File tmpFile = File.createTempFile("sfvwt", ".dat", tmpLoc);

    Path path = new Path(tmpFile.getAbsolutePath());
    Configuration conf = new Configuration();
    FileSystem fs = FileSystem.get(conf);
    SequenceFile.Writer seqWriter = new SequenceFile.Writer(fs, conf, path, LongWritable.class, SparseVector.class);
    SequenceFileVectorWriter writer = new SequenceFileVectorWriter(seqWriter);
    RandomVectorIterable iter = new RandomVectorIterable(50);
    writer.write(iter);
    writer.close();

    SequenceFile.Reader seqReader = new SequenceFile.Reader(fs, path, conf);
    LongWritable key = new LongWritable();
    SparseVector value = new SparseVector();
    int count = 0;
    while (seqReader.next(key, value)){
      count++;
    }
    assertEquals(count + " does not equal: " + 50, 50, count);
  }

  public void test() throws Exception {
    StringWriter strWriter = new StringWriter();
    VectorWriter writer = new JWriterVectorWriter(strWriter);
    List<Vector> vectors = new ArrayList<Vector>();
    vectors.add(new DenseVector(new double[]{0.3, 1.5, 4.5}));
    vectors.add(new DenseVector(new double[]{1.3, 1.5, 3.5}));
    writer.write(vectors);
    writer.close();
    StringBuffer buffer = strWriter.getBuffer();
    assertNotNull(buffer);
    assertTrue(buffer.length() > 0);
    System.out.println("Buffer: " + buffer);

  }
}
