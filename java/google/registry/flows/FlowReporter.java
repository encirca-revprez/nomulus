// Copyright 2017 The Nomulus Authors. All Rights Reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package google.registry.flows;

import static com.google.common.io.BaseEncoding.base64;
import static google.registry.xml.XmlTransformer.prettyPrint;

import com.google.common.collect.ImmutableMap;
import google.registry.flows.FlowModule.ClientId;
import google.registry.flows.FlowModule.InputXml;
import google.registry.flows.annotations.ReportingSpec;
import google.registry.model.eppcommon.Trid;
import google.registry.util.FormattingLogger;
import javax.inject.Inject;
import org.json.simple.JSONValue;

/** Reporter used by {@link FlowRunner} to record flow execution data for reporting. */
public class FlowReporter {
  /**
   * Log signature used by reporting pipelines to extract matching log lines.
   *
   * <p><b>WARNING:<b/> DO NOT CHANGE this value unless you want to break reporting.
   */
  private static final String REPORTING_LOG_SIGNATURE = "EPP-REPORTING-LOG-SIGNATURE";

  private static final FormattingLogger logger = FormattingLogger.getLoggerForCallerClass();

  @Inject Trid trid;
  @Inject @ClientId String clientId;
  @Inject @InputXml byte[] inputXmlBytes;
  @Inject Class<? extends Flow> flowClass;
  @Inject FlowReporter() {}

  /** Records information about the current flow execution in the GAE request logs. */
  public void recordToLogs() {
    // WARNING: This JSON log statement is parsed by reporting pipelines - be careful when changing.
    // It should be safe to add new keys, but be very cautious in changing existing keys.
    logger.infofmt(
        "%s: %s",
        REPORTING_LOG_SIGNATURE,
        JSONValue.toJSONString(ImmutableMap.<String, Object>of(
            "trid", trid.getServerTransactionId(),
            "clientId", clientId,
            "xml", prettyPrint(inputXmlBytes),
            "xmlBytes", base64().encode(inputXmlBytes),
            "icannActivityReportField", extractActivityReportField(flowClass))));
  }

  /**
   * Returns the ICANN activity report field for the given flow class, or the empty string if no
   * activity report field specification is found.
   */
  private static final String extractActivityReportField(Class<? extends Flow> flowClass) {
    ReportingSpec reportingSpec = flowClass.getAnnotation(ReportingSpec.class);
    if (reportingSpec != null) {
      return reportingSpec.value().getFieldName();
    }
    return "";
  }
}