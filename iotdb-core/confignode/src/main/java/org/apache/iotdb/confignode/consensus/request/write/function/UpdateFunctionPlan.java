/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.iotdb.confignode.consensus.request.write.function;

import org.apache.iotdb.commons.udf.UDFInformation;
import org.apache.iotdb.confignode.consensus.request.ConfigPhysicalPlan;
import org.apache.iotdb.confignode.consensus.request.ConfigPhysicalPlanType;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Objects;

public class UpdateFunctionPlan extends ConfigPhysicalPlan {
  private UDFInformation udfInformation;

  public UpdateFunctionPlan() {
    super(ConfigPhysicalPlanType.UpdateFunction);
  }

  public UpdateFunctionPlan(UDFInformation udfInformation) {
    super(ConfigPhysicalPlanType.UpdateFunction);
    this.udfInformation = udfInformation;
  }

  public UDFInformation getUdfInformation() {
    return udfInformation;
  }

  @Override
  protected void serializeImpl(DataOutputStream stream) throws IOException {
    stream.writeShort(getType().getPlanType());
    udfInformation.serialize(stream);
  }

  @Override
  protected void deserializeImpl(ByteBuffer buffer) throws IOException {
    udfInformation = UDFInformation.deserialize(buffer);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    UpdateFunctionPlan that = (UpdateFunctionPlan) o;
    return Objects.equals(udfInformation, that.udfInformation);
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), udfInformation);
  }
}
