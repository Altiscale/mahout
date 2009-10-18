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

package org.apache.mahout.fpm.pfpgrowth.convertors;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TransactionIterator<AttributePrimitive> implements Iterator<int[]> {
  private Map<AttributePrimitive, Integer> attributeIdMapping = null;

  private Iterator<List<AttributePrimitive>> iterator = null;

  int[] transactionBuffer = null;

  public TransactionIterator(Iterator<List<AttributePrimitive>> iterator,
      Map<AttributePrimitive, Integer> attributeIdMapping) {
    this.attributeIdMapping = attributeIdMapping;
    this.iterator = iterator;
    transactionBuffer = new int[attributeIdMapping.size()];
  }

  @Override
  public boolean hasNext() {
    return iterator.hasNext();
  }

  @Override
  public int[] next() {
    List<AttributePrimitive> transaction = iterator.next();
    int index = 0;
    for (AttributePrimitive Attribute : transaction) {
      if (attributeIdMapping.containsKey(Attribute)) {
        transactionBuffer[index++] = attributeIdMapping.get(Attribute);
      }
    }

    int[] transactionList = new int[index];
    System.arraycopy(transactionBuffer, 0, transactionList, 0, index);
    return transactionList;

  }

  @Override
  public void remove() {
    iterator.remove();
  }

}
